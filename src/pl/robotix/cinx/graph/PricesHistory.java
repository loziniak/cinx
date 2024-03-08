package pl.robotix.cinx.graph;

import static java.lang.Math.min;
import static pl.robotix.cinx.App.fromEpochSeconds;
import static pl.robotix.cinx.Currency.WALLET;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import pl.robotix.cinx.api.AsyncApi;
import pl.robotix.cinx.api.TimeValues;

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
					var price = history.get(history.size() - 1).price;
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
							
							walletValue += histories[i].get(j).price * amounts[i];
						}
						walletHistory.add(new Point(histories[0].get(j).date, walletValue, 0.0));
					}
				}
				displayedCurrencies.put(WALLET, walletHistory);			
			}
		});
		
	}

	public void retrieveUSDPriceHistory(Currency currency, Consumer<List<Point>> callback) {
		
		Task<List<History>> compositeTask = new Task<List<History>>() {

			@Override
			protected List<History> call() throws Exception {
				List<Pair> pairs;
				if (currency.equals(Currency.BTC)) {
					pairs = App.prices.pairsToComputeUSDFor(currency);
				} else {
					pairs = App.prices.pairsToComputeBTCFor(currency);
				}
				List<History> histories = new CopyOnWriteArrayList<>();
				CountDownLatch latch = new CountDownLatch(pairs.size());
				pairs.forEach((intermediatePair) -> {
					api.retrievePriceHistory(intermediatePair, timeRange.get(), 
							(history) -> {
//						System.out.println(intermediatePair.toString()
//								+ " = " + history.points.size()
//								+ " " + history.timeValues);
						if (history.pair.base.equals(currency)) {
							history.isVolumeSignificant = true;
						}
						histories.add(history);
						latch.countDown();
					});
				});
				latch.await();
				return histories;
			}
			
			@Override
			protected void succeeded() {
				
				List<History> histories = null;
				try {
					histories = get();
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}
				
				ArrayList<Point> combined = combine(histories);
//				System.out.println("combined: "+combined.size());
				callback.accept(combined);
			}
		};
		
		new Thread(compositeTask).start();
	}
	
	public static ArrayList<Point> combine(List<History> histories) {
		History longest = histories.get(0);
		for (int i = 1; i < histories.size(); i++) {
			History current = histories.get(i);
			if (longest.timeValues.getPointsCount() < current.timeValues.getPointsCount()) {
				longest = current;
			}
		}
		final History theLongest = longest;
		ArrayList<Point> accumPriceHistory = initWithOnes(longest.timeValues);
		histories.forEach((intermediateHistory) -> {
//			System.out.println("intermediate: "+intermediateHistory.pair+" "+intermediateHistory.isVolumeSignificant);
			double rate = ((double) theLongest.timeValues.getPointsCount()) / intermediateHistory.timeValues.getPointsCount();

			var ihCount = min((int) (intermediateHistory.points.size() * rate), accumPriceHistory.size());
			var ihSmallerBy = accumPriceHistory.size() - ihCount;

			Point accum, interm;
			for (int i = accumPriceHistory.size() - 1; i >= ihSmallerBy; i--) {
				accum = accumPriceHistory.get(i);
				interm = intermediateHistory.points.get((int) ((i - ihSmallerBy) / rate));

				accum.price *= interm.price;
				if (intermediateHistory.isVolumeSignificant) {
					accum.volume = interm.volume;
				}
			}

			double firstKnownPrice = intermediateHistory.points.get(0).price;
			for (int i = ihSmallerBy - 1; i >= 0; i--) {
				accumPriceHistory.get(i).price *= firstKnownPrice;
			}
			
		});
		
		return accumPriceHistory;
	}
	
	private static ArrayList<Point> initWithOnes(TimeValues range) {
		ArrayList<Point> usdPriceHistory = new ArrayList<>(100);
		long start = range.getStart();
		for (int i=0; i < range.getPointsCount(); i++) {
			usdPriceHistory.add(new Point(
				fromEpochSeconds(start + i * range.densitySeconds),
				1.0, 0.0)
			);
		}
		return usdPriceHistory;
	}
	
	public static class History {
		Pair pair;
		List<Point> points;
		public boolean isVolumeSignificant;
		public TimeValues timeValues;

		public History(Pair pair, List<Point> points, boolean isVolumeSignificant, TimeValues timeValues) {
			super();
			this.pair = pair;
			this.points = points;
			this.isVolumeSignificant = isVolumeSignificant;
			this.timeValues = timeValues;
		}
		
		@Override
		public String toString() {
			return pair.toString()+" "+points.size()+" "+isVolumeSignificant+" "+timeValues;
		}

	}
	
}
