package pl.robotix.cinx.api;

import static pl.robotix.cinx.Currency.BTC;
import static pl.robotix.cinx.Currency.USDT;
import static pl.robotix.cinx.Pair.USDT_BTC;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Point;
import pl.robotix.cinx.Prices;
import pl.robotix.cinx.TimeRange;
import pl.robotix.cinx.graph.PricesHistory;
import pl.robotix.cinx.graph.PricesHistory.History;

public class MultiApi implements AsyncApi {
	
	AsyncApi baseApi;
	
	List<AsyncApi> allApis = new ArrayList<>();
	
	Map<Pair, AsyncApi> specificApis = new HashMap<>();

	
	public MultiApi(AsyncApi baseApi) {
		this.baseApi = baseApi;
		addApi(baseApi);
	}

	
	public void addApi(AsyncApi api) {
		allApis.add(api);
		var pairs = new HashSet<>(api.pairsForMarket(BTC)); // TODO: trade with USDT, not BTC. bigger volumes.
		pairs.addAll(api.pairsForMarket(USDT));
		for (Pair pair: pairs) {
			specificApis.putIfAbsent(pair, api);
		}
	}

	
	@Override
	public Map<Currency, BigDecimal> retrieveBalance() {
		return baseApi.retrieveBalance();
	}

	@Override
	public void buy(Pair pair, BigDecimal rate, BigDecimal amount, Consumer<OperationException> callback) {
		baseApi.buy(pair, rate, amount, callback);
	}

	@Override
	public void sell(Pair pair, BigDecimal rate, BigDecimal amount, Consumer<OperationException> callback) {
		baseApi.sell(pair, rate, amount, callback);
	}

	@Override
	public double takerFee() {
		return baseApi.takerFee();
	}
	
	@Override
	public boolean isBusy() {
		return baseApi.isBusy();
	}
	
	@Override
	public TimeValues timeValues(TimeRange range, Currency currency) {
		return specificApis.get(new Pair(BTC, currency)).timeValues(range, currency);
	}

	
	@Override
	public void retrievePriceHistory(Pair pair, TimeRange range, Consumer<History> callback) {
		AsyncApi api = specificApis.get(pair);
		if (api != null) {
			api.retrievePriceHistory(pair, range, callback);
		} else {
			Pair switched = switchQuote(pair);
			api = specificApis.get(switched);
			Pair quotes = new Pair(pair.quote, switched.quote);
			var quotesApi = specificApis.get(quotes);
			if (api != null && quotesApi != null) {
				
				api.retrievePriceHistory(switched, range, points -> {
					points.isVolumeSignificant = true;	
					quotesApi.retrievePriceHistory(quotes, range, quotesPoints -> {
						
						List<Point> combinedPoints = PricesHistory.combine(Arrays.asList(
								points, quotesPoints));
						
						TimeValues t = quotesPoints.timeValues.getPointsCount() > points.timeValues.getPointsCount() ? quotesPoints.timeValues : points.timeValues;
						
						callback.accept(new History(pair, combinedPoints, false, t));
					});
				});			
			}
		}
	}

	public Prices retrievePrices() {
		Prices ret = new Prices();
		ret.setApi(this);
		for (AsyncApi api : allApis) {
			ret.join(api.retrievePrices());
		}
		return ret;
	}

	@Override
	public Prices retrievePrices(Collection<Pair> pairs) {
		Prices ret = new Prices();
		ret.setApi(this);

		Set<Pair> pairsLeft = new HashSet<>(pairs);

		for (AsyncApi api : allApis) {
			var apiPairs = new ArrayList<Pair>(api.pairsForMarket(BTC));
			var usdtPairs = new ArrayList<Pair>(api.pairsForMarket(USDT));
			apiPairs.addAll(usdtPairs);
			apiPairs.retainAll(pairs);

			var newPrices = api.retrievePrices(apiPairs);
			ret.join(newPrices);
			pairsLeft.removeAll(apiPairs);
		}
		
		if (!pairsLeft.isEmpty()) {
			for (AsyncApi api : allApis) {
				var apiPairs = new ArrayList<Pair>(api.pairsForMarket(BTC));
				var usdtPairs = new ArrayList<Pair>(api.pairsForMarket(USDT));
				apiPairs.addAll(usdtPairs);
				
				var switchedPairs = new ArrayList<Pair>();
				var straightPairs = new ArrayList<Pair>();
				for (Pair pair : pairsLeft) {
					var switched = switchQuote(pair);
					if (apiPairs.contains(switched)) {
						switchedPairs.add(switched);
						straightPairs.add(pair);
					}
				}
				var newPrices = api.retrievePrices(switchedPairs);
				newPrices.switchQuotes(ret.getRate(USDT_BTC));
				ret.join(newPrices);
				pairsLeft.removeAll(straightPairs);
			}
			
		}

		if (!pairsLeft.isEmpty()) {
			System.err.println("(MultiApi) retrievePrices left: " + pairsLeft.toString());
		}
		return ret;
	}
	
	private Pair switchQuote(Pair p) {
		return new Pair(
				p.quote.equals(BTC) ? USDT : BTC,
				p.base
			);
	}

	@Override
	public Collection<Pair> pairsForMarket(Currency c) {
		Set<Pair> ret = new HashSet<Pair>();
		for (AsyncApi api : allApis) {
			var newPairs = api.pairsForMarket(c);
			ret.addAll(newPairs);
		}
		return ret;
	}

	@Override
	public boolean isExchangeable(Currency c) {
		for (AsyncApi api : allApis) {
			if (api.isExchangeable(c)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void initTimeRanges() {
		for (AsyncApi api : allApis) {
			api.initTimeRanges();
		}
	}

	@Override
	public void close() {
		for (AsyncApi api : allApis) {
			api.close();
		}
	}
	
}
