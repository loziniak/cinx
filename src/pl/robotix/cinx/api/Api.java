package pl.robotix.cinx.api;

import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.cf.client.poloniex.PoloniexExchangeService;
import com.cf.data.model.poloniex.PoloniexCompleteBalance;
import com.cf.data.model.poloniex.PoloniexOrderResult;
import com.cf.data.model.poloniex.PoloniexTicker;

import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Point;
import pl.robotix.cinx.Prices;
import pl.robotix.cinx.TimeRange;

public class Api {
	
	private static final long THROTTLE_MS = 200; // 5 calls per second
	
	private PoloniexExchangeService service;
	
	private Prices prices;
	
	private long lastOpMillis;
	
	public Api(String poloniexApiKey, String poloniexSecret) {
		service = new PoloniexExchangeService(poloniexApiKey, poloniexSecret);
		lastOpMillis = System.currentTimeMillis();
		prices = retrievePrices();
	}
	
	
	public List<Point> retrieveUSDPriceHistory(Currency currency, TimeRange range) {
		List<Point> usdPriceHistory = initWithOnes(range);
		
		List<Pair> pairs = prices.pairsToComputeUSDFor(currency);
		pairs.forEach((intermediatePair) -> {
			List<Point> intermediateHistory = retrievePriceHistory(intermediatePair, range);

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
	
//	public Map<Currency, BigDecimal> retrieveUSDBalanceMock() {
//		Map<Currency, BigDecimal> usdBalance = new HashMap<>();
//		usdBalance.put(USDT, valueOf(1000.0));
//		usdBalance.put(BTC, valueOf(2500.0));
//		usdBalance.put(new Currency("MAID"), valueOf(700.0));
//		
//		return usdBalance;
//	}
	
	public void refreshPrices() {
		prices = retrievePrices();
	}
	
	public boolean buy(Pair pair, BigDecimal rate, BigDecimal amount) {
		PoloniexOrderResult res = service.buy(pair.toString(), rate, amount, true, false, false);
		return res.error == null || res.error.isEmpty();
	}

	public boolean sell(Pair pair, BigDecimal rate, BigDecimal amount) {
		PoloniexOrderResult res = service.sell(pair.toString(), rate, amount, true, false, false);
		return res.error == null || res.error.isEmpty();
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
	
    private List<Point> retrievePriceHistory(Pair pair, TimeRange range) {
        throttleControl();
        
        return service.returnChartData(pair.toString(), range.densitySeconds, range.getStart())
        .stream()
        .map((point) -> {
            return new Point(point.date.toLocalDateTime() , point.weightedAverage.doubleValue());
        }).collect(toList());
    }
    
	private Prices retrievePrices() {
		throttleControl();
		
		Map<Pair, BigDecimal> prices = new HashMap<>();
		Map<Pair, BigDecimal> volumes = new HashMap<>();
		Map<String, PoloniexTicker> tickerAll = service.returnTicker();
		tickerAll.forEach((pairSymbol, tickerOne) -> {
			prices.put(new Pair(pairSymbol), tickerOne.last);
			volumes.put(new Pair(pairSymbol), tickerOne.baseVolume);
		});
		
		return new Prices(prices, volumes);
	}
	
	
	private void throttleControl() {
		try {
			long waitMs = THROTTLE_MS - (System.currentTimeMillis() - lastOpMillis);
			if (waitMs > 0) { 
				Thread.sleep(waitMs);
			}
		} catch (InterruptedException e) {
		}
		lastOpMillis = System.currentTimeMillis();
	}
	
	
	public Prices getPrices() {
		return prices;
	}

}
