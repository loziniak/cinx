package pl.robotix.cinx.api;

import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
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

public class Api {
	
	private PoloniexExchangeService service;

	private Prices prices;
	
	public Api(String poloniexApiKey, String poloniexSecret) {
		service = new PoloniexExchangeService(poloniexApiKey, poloniexSecret);
		prices = retrievePrices();
	}
	
	
	public Map<Currency, BigDecimal> retrieveUSDBalance() {
		Map<Currency, BigDecimal> usdBalance = new HashMap<>();
		Map<String, PoloniexCompleteBalance> balanceData = service.returnBalance(false);
		balanceData.entrySet().forEach((entry) -> {
			Currency currency = new Currency(entry.getKey());
			usdBalance.put(currency,
				prices.getUSDFor(currency).multiply(entry.getValue().available.add(entry.getValue().onOrders))
				);
		});
		
		return usdBalance;
	}
	
	public boolean buy(Pair pair, BigDecimal rate, BigDecimal amount) {
		PoloniexOrderResult res = service.buy(pair.toString(), rate, amount, true, false, false);
		return res.error == null || res.error.isEmpty();
	}

	public boolean sell(Pair pair, BigDecimal rate, BigDecimal amount) {
		PoloniexOrderResult res = service.sell(pair.toString(), rate, amount, true, false, false);
		return res.error == null || res.error.isEmpty();
	}


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
    
	public Prices retrievePrices() {
		Map<Pair, BigDecimal> prices = new HashMap<>();
		Map<Pair, BigDecimal> volumes = new HashMap<>();
		Map<String, PoloniexTicker> tickerAll = service.returnTicker();
		tickerAll.forEach((pairSymbol, tickerOne) -> {
			prices.put(new Pair(pairSymbol), tickerOne.last);
			volumes.put(new Pair(pairSymbol), tickerOne.baseVolume);
		});
		
		return new Prices(prices, volumes);
	}
	
}
