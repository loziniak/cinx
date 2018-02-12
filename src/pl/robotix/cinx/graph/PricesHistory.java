package pl.robotix.cinx.graph;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener.Change;
import javafx.concurrent.Task;
import pl.robotix.cinx.App;
import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Point;
import pl.robotix.cinx.TimeRange;
import pl.robotix.cinx.api.AsyncThrottledCachedApi;

public class PricesHistory {
	
	final ObjectProperty<TimeRange> timeRange = new SimpleObjectProperty<>(TimeRange.WEEK);
	
	final ObservableMap<Currency, List<Point>> displayedCurrencies = FXCollections.observableHashMap();
	
	private AsyncThrottledCachedApi api;
	

	public PricesHistory(AsyncThrottledCachedApi api, ObservableSet<Currency> chartCurrencies) {
		this.api = api;

		chartCurrencies.addListener((Change<? extends Currency> change) -> {
			if (change.wasAdded()) {
				Currency added = change.getElementAdded();
				retrieveUSDPriceHistory(added, (priceHistory) -> {
					displayedCurrencies.put(added, priceHistory);
				});
			} else if (change.wasRemoved()) {
				displayedCurrencies.remove(change.getElementRemoved());
			}
		});
		
		timeRange.addListener((observable, oldValue, newValue) -> {
			if (oldValue != newValue) {
				Set<Currency> currencies = new HashSet<>(displayedCurrencies.keySet());
				currencies.forEach((currency) -> {
					displayedCurrencies.remove(currency);
					retrieveUSDPriceHistory(currency, (priceHistory) -> {
						displayedCurrencies.put(currency, priceHistory);
					});
				});
			}
		});
	}

	public void retrieveUSDPriceHistory(Currency currency, Consumer<List<Point>> callback) {
		
		Task<List<List<Point>>> compositeTask = new Task<List<List<Point>>>() {

			@Override
			protected List<List<Point>> call() throws Exception {
				List<Pair> pairs = App.prices.pairsToComputeBTCFor(currency);
				List<List<Point>> histories = new CopyOnWriteArrayList<>();
				CountDownLatch latch = new CountDownLatch(pairs.size());
				pairs.forEach((intermediatePair) -> {
					api.retrievePriceHistory(intermediatePair, timeRange.get(), 
							(history) -> {
						histories.add(history);
						latch.countDown();
					});
				});
				latch.await();
				return histories;
			}
			
			@Override
			protected void succeeded() {
				
				List<List<Point>> histories = null;
				try {
					histories = get();
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}
				
				List<Point> usdPriceHistory = initWithOnes(timeRange.getValue());
				histories.forEach((intermediateHistory) -> {
					Iterator<Point> intermediateIterator = intermediateHistory.iterator();
					Point intermediatePoint = usdPriceHistory.get(0);
					int filled = 0;
					try {
						for (Point usdPoint: usdPriceHistory) {
							intermediatePoint = intermediateIterator.next();
							usdPoint.value *= intermediatePoint.value;
							filled++;
						}
						
					} catch (NoSuchElementException e) { // intermediateHistory had less data than necessary
						Iterator<Point> i = usdPriceHistory.listIterator(filled);
						while (i.hasNext()) {
							i.next().value *= intermediatePoint.value; // fill using last known price
						}
					}
				});
				
				callback.accept(usdPriceHistory);
			}
		};
		
		new Thread(compositeTask).start();
	}
	
	private static List<Point> initWithOnes(TimeRange range) {
		List<Point> usdPriceHistory = new LinkedList<>();
		long start = range.getStart();
		for (int i=0; i < range.getPointsCount(); i++) {
			usdPriceHistory.add(new Point(
				fromEpochSeconds(start + i * range.densitySeconds),
				1.0)
			);
		}
		return usdPriceHistory;
	}
	
	public static LocalDateTime fromEpochSeconds(long epochSeconds) {
		return LocalDateTime.ofEpochSecond(epochSeconds, 0, ZoneOffset.UTC);
	}
	
}
