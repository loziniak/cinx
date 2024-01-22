package pl.robotix.cinx.graph;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static pl.robotix.cinx.Currency.WALLET;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener.Change;
import javafx.concurrent.Task;
import pl.robotix.cinx.App;
import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Point;
import pl.robotix.cinx.TimeRange;
import pl.robotix.cinx.api.AsyncApi;

public class PricesHistory {
	
	final ObjectProperty<TimeRange> timeRange = new SimpleObjectProperty<>(TimeRange.WEEK);
	
	final ObservableMap<Currency, List<Point>> displayedCurrencies = FXCollections.observableHashMap();
	
	private AsyncApi api;
	

	public PricesHistory(AsyncApi api, ObservableSet<Currency> chartCurrencies, ObjectProperty<Map<Currency, Double>> walletCurrencies) {
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

		walletCurrencies.addListener((observable, oldValue, newValue) -> {
			displayedCurrencies.remove(WALLET);
			
			ArrayList<Point> walletHistory = new ArrayList<Point>();
			if (!displayedCurrencies.isEmpty()) {
				var wc = newValue;
//				double[] percents = new double[wc.size()];
				double[] amounts = new double[wc.size()];
				ArrayList<double[]> values = new ArrayList<double[]>();
				List<Point>[] histories = (List<Point>[]) new List[wc.size()];
				int minHistSize = Integer.MAX_VALUE;

				Object[] currencies = wc.keySet().toArray();
				for (int i=0; i<currencies.length; i++) {
					var percent = wc.get(currencies[i]).doubleValue();
					var history = displayedCurrencies.get(currencies[i]);
					var price = history.get(history.size() - 1).value;
					amounts[i] = percent / price / 100;
					histories[i] = history;
					if (history.size() < minHistSize) {
						minHistSize = history.size();
					}
				}
				
				if (currencies.length > 0) {
					for (int j=0; j<minHistSize; j++) {
						double walletValue = 0.0;
						for (int i=0; i<currencies.length; i++) {
							
							walletValue += histories[i].get(j).value * amounts[i];
						}
						walletHistory.add(new Point(histories[0].get(j).date, walletValue));
					}
				}
				displayedCurrencies.put(WALLET, walletHistory);			
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
				
				ArrayList<Point> usdPriceHistory = initWithOnes(timeRange.getValue());
				histories.forEach((intermediateHistory) -> {

					var ihCount = min(intermediateHistory.size(), usdPriceHistory.size());
					var ihSmallerBy = usdPriceHistory.size() - ihCount;

					for (int i = usdPriceHistory.size() - 1; i >= ihSmallerBy; i--) {
						usdPriceHistory.get(i).value *= intermediateHistory.get(i - ihSmallerBy).value;
					}
					double firstKnownPrice = intermediateHistory.get(0).value;
					for (int i = ihSmallerBy - 1; i >= 0; i--) {
						usdPriceHistory.get(i).value *= firstKnownPrice;
					}
				});
				
				callback.accept(usdPriceHistory);
			}
		};
		
		new Thread(compositeTask).start();
	}
	
	private static ArrayList<Point> initWithOnes(TimeRange range) {
		ArrayList<Point> usdPriceHistory = new ArrayList<>(100);
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
