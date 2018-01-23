package pl.robotix.cinx.graph;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener.Change;
import pl.robotix.cinx.App;
import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Point;
import pl.robotix.cinx.TimeRange;
import pl.robotix.cinx.api.Api;

public class PricesHistory {
	
	final ObjectProperty<TimeRange> timeRange = new SimpleObjectProperty<>(TimeRange.DAY);
	
	final ObservableMap<Currency, List<Point>> displayedCurrencies = FXCollections.observableHashMap();
	
	private Api api;
	

	public PricesHistory(Api api, ObservableSet<Currency> chartCurrencies) {
		this.api = api;

		chartCurrencies.addListener((Change<? extends Currency> change) -> {
			if (change.wasAdded()) {
				Currency added = change.getElementAdded();
				List<Point> priceHistory = retrieveUSDPriceHistory(added);
				displayedCurrencies.put(added, priceHistory);
			
			} else if (change.wasRemoved()) {
				displayedCurrencies.remove(change.getElementRemoved());
			}
		});
		
		timeRange.addListener((ObservableValue<? extends TimeRange> observable, TimeRange oldValue, TimeRange newValue) -> {
			if (oldValue != newValue) {
				Set<Currency> currencies = new HashSet<>(displayedCurrencies.keySet());
				currencies.forEach((currency) -> {
					displayedCurrencies.remove(currency);
					displayedCurrencies.put(currency, retrieveUSDPriceHistory(currency));
				});
			}
		});
	}

	public List<Point> retrieveUSDPriceHistory(Currency currency) {
		List<Point> usdPriceHistory = initWithOnes(timeRange.getValue());
		
//		List<Pair> pairs = prices.pairsToComputeUSDFor(currency);
		List<Pair> pairs = App.prices.pairsToComputeBTCFor(currency);
		pairs.forEach((intermediatePair) -> {
			List<Point> intermediateHistory = api.retrievePriceHistory(intermediatePair, timeRange.get());

			Iterator<Point> intermediateIterator = intermediateHistory.iterator();
			Point intermediatePoint = null;
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
		
		return usdPriceHistory;
	}
	
	private List<Point> initWithOnes(TimeRange range) {
		List<Point> usdPriceHistory = new LinkedList<>();
		long start = range.getStart();
		for (int i=0; i < range.getPointsCount(); i++) {
			usdPriceHistory.add(new Point(
				LocalDateTime.ofEpochSecond(start + i * range.densitySeconds, 0, ZoneOffset.UTC),
				1.0)
			);
		}
		return usdPriceHistory;
	}
	
	
}
