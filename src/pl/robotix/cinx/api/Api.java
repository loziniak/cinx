package pl.robotix.cinx.api;

import static java.util.stream.Collectors.toList;

import java.util.List;

import com.cf.client.poloniex.PoloniexExchangeService;

import pl.robotix.cinx.Point;
import pl.robotix.cinx.PriceRange;

public class Api {
	
	private static final long THROTTLE_MS = 200; // 5 calls per second
	
	private PoloniexExchangeService service;
	
	private long lastOpMillis;
	
	public Api() {
		service = new PoloniexExchangeService(null, null);
		lastOpMillis = System.currentTimeMillis();
	}
	
	
	public List<Point> getPriceHistory(String pair, PriceRange range) {
		throttleControl();
		
		return service.returnChartData(pair, range.densitySeconds, 
				System.currentTimeMillis() / 1000 - range.periodSeconds)
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

}
