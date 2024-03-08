package pl.robotix.cinx.api.poloniex;

import static java.util.stream.Collectors.toList;
import static pl.robotix.cinx.TimeRange.DAY;
import static pl.robotix.cinx.TimeRange.MONTH;
import static pl.robotix.cinx.TimeRange.WEEK;
import static pl.robotix.cinx.TimeRange.YEAR;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.cf.client.poloniex.PoloniexExchangeService;
import com.cf.data.model.poloniex.PoloniexChartData;
import com.cf.data.model.poloniex.PoloniexCompleteBalance;
import com.cf.data.model.poloniex.PoloniexOrderResult;
import com.cf.data.model.poloniex.PoloniexTicker;

import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Point;
import pl.robotix.cinx.Prices;
import pl.robotix.cinx.TimeRange;
import pl.robotix.cinx.api.OperationException;
import pl.robotix.cinx.api.SyncApi;
import pl.robotix.cinx.api.TimeValues;
import pl.robotix.cinx.graph.PricesHistory.History;

public class PoloniexApi implements SyncApi {
	
	private static final int NONCE_ERROR_RETRY_COUNT = 2; 

	private static final double TAKER_FEE = 0.0025;

	private static final HashMap<TimeRange, TimeValues> TIME_VALUES = new HashMap<>();

	private PoloniexExchangeService service;

	public PoloniexApi(String poloniexApiKey, String poloniexSecret) {
		service = new PoloniexExchangeService(poloniexApiKey, poloniexSecret);
	}
	
	
	@Override
	public void initTimeRanges() {
		TIME_VALUES.put(DAY, new TimeValues(DAY.seconds,          5 * 60)); // DAY -> 5 minutes
		TIME_VALUES.put(WEEK, new TimeValues(WEEK.seconds,       30 * 60)); // WEEK -> 30 min
		TIME_VALUES.put(MONTH, new TimeValues(MONTH.seconds, 4 * 60 * 60)); // MONTH -> 4h
		TIME_VALUES.put(YEAR, new TimeValues(YEAR.seconds,  24 * 60 * 60)); // YEAR -> 1d
	}

	@Override
	public Map<Currency, BigDecimal> retrieveBalance() {
		Map<Currency, BigDecimal> realBalance = new HashMap<>();
		Map<String, PoloniexCompleteBalance> balanceData = service.returnBalance(false);
		balanceData.forEach((currencyName, balance) -> {
			Currency currency = new Currency(currencyName);
			realBalance.put(currency,
					balance.available.add(balance.onOrders)
				);
		});
		
		return realBalance;
	}
	
	@Override
	public void buy(Pair pair, BigDecimal rate, BigDecimal amount) throws OperationException {
		PoloniexOrderResult res = service.buy(pairString(pair), rate, amount, true, false, false);

		for (int i = 0; i < NONCE_ERROR_RETRY_COUNT; i++) {
			if (res.error != null && res.error.startsWith("Nonce must be greater than ")) {
				System.out.println("Buy "+pair+" retrying.");
		                    res = service.buy(pairString(pair), rate, amount, true, false, false);
			}
		}
		if (res.error != null) {
			throw new OperationException(res.error);
		}
	}

	@Override
	public void sell(Pair pair, BigDecimal rate, BigDecimal amount) throws OperationException {
		PoloniexOrderResult res = service.sell(pairString(pair), rate, amount, true, false, false);

		for (int i = 0; i < NONCE_ERROR_RETRY_COUNT; i++) {
			if (res.error != null && res.error.startsWith("Nonce must be greater than ")) {
				System.out.println("Sell "+pair+" retrying.");
		                    res = service.sell(pairString(pair), rate, amount, true, false, false);
			}
		}
		if (res.error != null) {
			throw new OperationException(res.error);
		}
	}


    @Override
	public History retrievePriceHistory(Pair pair, TimeRange range) {
    	var time = timeValues(range, pair.base);
        Function<PoloniexChartData, Point> pointCreator;
        if (pair.isReverse()) {
        	pointCreator = (point) -> new Point(
        			point.date.toLocalDateTime(),
        			1.0 / point.weightedAverage.doubleValue(),
        			point.volume.doubleValue());
        	pair = pair.reverse();
        } else {
        	pointCreator = (point) -> new Point(
        			point.date.toLocalDateTime(),
        			point.weightedAverage.doubleValue(),
        			point.volume.doubleValue());
        }
        
        var ret = service.returnChartData(pairString(pair), time.densitySeconds, time.getStart())
	        .stream()
	        .map(pointCreator)
	        .collect(toList());
        return new History(pair, ret, false, timeValues(range, null));
    }
    
	@Override
	public Prices retrievePrices() {
		Map<Pair, BigDecimal> prices = new HashMap<>();
		Map<Pair, BigDecimal> volumes = new HashMap<>();
		Map<String, PoloniexTicker> tickerAll = service.returnTicker();
		tickerAll.forEach((pairSymbol, tickerOne) -> {
			prices.put(pair(pairSymbol), tickerOne.last);
			volumes.put(pair(pairSymbol), tickerOne.baseVolume);
		});
		
		return new Prices(prices, volumes);
	}
	
	@Override
	public Prices retrievePrices(Collection<Pair> pairs) {
		throw new UnsupportedOperationException("Not implemented");
	}
	
	@Override
	public Collection<Pair> pairsForMarket(Currency c) {
		throw new UnsupportedOperationException("Not implemented");
	}
	
	@Override
	public boolean isExchangeable(Currency c) {
		return true;
	}
	
	@Override
	public double takerFee() {
		return TAKER_FEE;
	}
	
	@Override
	public TimeValues timeValues(TimeRange range, Currency currency) {
		return TIME_VALUES.get(range);
	}


	private static String pairString(Pair pair) {
		return pair.quote.symbol + "_" + pair.base.symbol;
	}
	
	private static Pair pair(String pairString) {
		String[] currencySymbols = pairString.split("_");
		return new Pair(currencySymbols[0], currencySymbols[1]);
	}
	
}
