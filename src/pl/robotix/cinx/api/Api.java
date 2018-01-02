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
import com.cf.data.model.poloniex.PoloniexTicker;

import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Point;
import pl.robotix.cinx.PriceRange;
import pl.robotix.cinx.Prices;

public class Api {
	
	private static final long THROTTLE_MS = 200; // 5 calls per second
	
	private PoloniexExchangeService service;
	
	private Prices prices;
	
	private long lastOpMillis;
	
	public Api() {
		service = new PoloniexExchangeService(null, null);
		lastOpMillis = System.currentTimeMillis();
	}
	
	
	public Map<Pair, BigDecimal> getPrices() {
		throttleControl();
		
		Map<Pair, BigDecimal> prices = new HashMap<>();
		Map<String, PoloniexTicker> tickerAll = service.returnTicker();
		tickerAll.forEach((pairSymbol, tickerOne) -> {
			prices.put(new Pair(pairSymbol), tickerOne.last);
		});
		
		return prices;
	}
	
	public List<Point> getUSDPriceHistory(Currency currency, PriceRange range) {
		List<Point> usdPriceHistory = initWithOnes(range);
		
		List<Pair> pairs = prices.pairsToComputeUSDFor(currency);
		pairs.forEach((intermediatePair) -> {
			List<Point> intermediateHistory = getPriceHistory(intermediatePair, range);

			if (Math.abs(intermediateHistory.size() / usdPriceHistory.size() - 1) > 0.01) {
				throw new IllegalStateException("Price history sizes differ too much. "
						+ intermediatePair + ":" + intermediateHistory.size() + ", "
						+ "USDT_" + currency + ": " + usdPriceHistory.size());
			}

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


	private List<Point> initWithOnes(PriceRange range) {
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
	
    private List<Point> getPriceHistory(Pair pair, PriceRange range) {
        throttleControl();
        
        return service.returnChartData(pair.toString(), range.densitySeconds, range.getStart())
        .stream()
        .map((point) -> {
            return new Point(point.date.toLocalDateTime() , point.weightedAverage.doubleValue());
        }).collect(toList());
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
	
	
	public void setPrices(Prices prices) {
		this.prices = prices;
	}

}
