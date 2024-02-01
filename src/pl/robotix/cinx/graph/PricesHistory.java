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
				List<Pair> pairs = App.prices.pairsToComputeBTCFor(currency);
				List<History> histories = new CopyOnWriteArrayList<>();
				CountDownLatch latch = new CountDownLatch(pairs.size());
				pairs.forEach((intermediatePair) -> {
					api.retrievePriceHistory(intermediatePair, timeRange.get(), 
							(history) -> {
						histories.add(new History(intermediatePair, history));
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
				
				ArrayList<Point> usdPriceHistory = initWithOnes(timeRange.getValue());
				histories.forEach((intermediateHistory) -> {

					var ihCount = min(intermediateHistory.points.size(), usdPriceHistory.size());
					var ihSmallerBy = usdPriceHistory.size() - ihCount;
					boolean isVolumeSignificant = intermediateHistory.pair.base.equals(currency);

					Point usd, interm;
					for (int i = usdPriceHistory.size() - 1; i >= ihSmallerBy; i--) {
						usd = usdPriceHistory.get(i);
						interm = intermediateHistory.points.get(i - ihSmallerBy);

						usd.price *= interm.price;
						if (isVolumeSignificant) {
							usd.volume = interm.volume;
						}
					}

					double firstKnownPrice = intermediateHistory.points.get(0).price;
					for (int i = ihSmallerBy - 1; i >= 0; i--) {
						usdPriceHistory.get(i).price *= firstKnownPrice;
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
				1.0, 0.0)
			);
		}
		return usdPriceHistory;
	}
	
	private static class History {
		Pair pair;
		List<Point> points;

		public History(Pair pair, List<Point> points) {
			super();
			this.pair = pair;
			this.points = points;
		}

	}
	
}
