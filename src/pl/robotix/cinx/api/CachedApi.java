package pl.robotix.cinx.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Point;
import pl.robotix.cinx.TimeRange;

public class CachedApi extends Api {

	private Map<String, Object> cache = new HashMap<>();

	public CachedApi(String poloniexApiKey, String poloniexSecret) {
		super(poloniexApiKey, poloniexSecret);
	}
	
	@Override
	protected List<Point> retrievePriceHistory(Pair pair, TimeRange range) {
		String key = "retrievePriceHistory_" + pair + "_" + range;
		List<Point> value = (List<Point>) cache.get(key);
		if (value == null) {
			value = super.retrievePriceHistory(pair, range);
			cache.put(key, value);
		} else {
			System.out.println("Cache: "+key);
		}
		return value;
	}
	
}
