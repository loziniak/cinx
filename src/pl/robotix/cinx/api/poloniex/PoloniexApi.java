package pl.robotix.cinx.api.poloniex;

import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
import pl.robotix.cinx.api.SyncApi;

public class PoloniexApi implements SyncApi {
	
	private static final int NONCE_ERROR_RETRY_COUNT = 2; 
	
	private PoloniexExchangeService service;

	public PoloniexApi(String poloniexApiKey, String poloniexSecret) {
		service = new PoloniexExchangeService(poloniexApiKey, poloniexSecret);
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
	public boolean buy(Pair pair, BigDecimal rate, BigDecimal amount) {
		PoloniexOrderResult res = service.buy(pair.toString(), rate, amount, true, false, false);

		for (int i = 0; i < NONCE_ERROR_RETRY_COUNT; i++) {
			if (res.error != null && res.error.startsWith("Nonce must be greater than ")) {
				System.out.println("Buy "+pair+" retrying.");
		                    res = service.buy(pair.toString(), rate, amount, true, false, false);
			}
		}
		return res.error == null;
	}

	@Override
	public boolean sell(Pair pair, BigDecimal rate, BigDecimal amount) {
		PoloniexOrderResult res = service.sell(pair.toString(), rate, amount, true, false, false);

		for (int i = 0; i < NONCE_ERROR_RETRY_COUNT; i++) {
			if (res.error != null && res.error.startsWith("Nonce must be greater than ")) {
				System.out.println("Sell "+pair+" retrying.");
		                    res = service.sell(pair.toString(), rate, amount, true, false, false);
			}
		}
		return res.error == null;
	}


    @Override
	public List<Point> retrievePriceHistory(Pair pair, TimeRange range) {
        Function<PoloniexChartData, Point> pointCreator;
        if (pair.isReverse()) {
        	pointCreator = (point) -> new Point(point.date.toLocalDateTime() , 1.0 / point.weightedAverage.doubleValue());
        	pair = pair.reverse();
        } else {
        	pointCreator = (point) -> new Point(point.date.toLocalDateTime() , point.weightedAverage.doubleValue());
        }
        
        return service.returnChartData(pair.toString(), range.densitySeconds, range.getStart())
        .stream()
        .map(pointCreator)
        .collect(toList());
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
	public Collection<Currency> pairsForMarket(Currency c) {
		throw new UnsupportedOperationException("Not implemented");
	}
	
	@Override
	public String pairString(Pair pair) {
		return pair.quote.symbol + "_" + pair.base.symbol;
	}
	
	@Override
	public Pair pair(String pairString) {
		String[] currencySymbols = pairString.split("_");
		return new Pair(currencySymbols[0], currencySymbols[1]);
	}
	
	@Override
	public boolean isExchangeable(Currency c) {
		return true;
	}
}