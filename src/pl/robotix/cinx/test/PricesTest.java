package pl.robotix.cinx.test;

import static java.time.LocalDateTime.ofEpochSecond;
import static java.time.ZoneOffset.UTC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Point;
import pl.robotix.cinx.Prices;
import pl.robotix.cinx.api.TimeValues;
import pl.robotix.cinx.graph.PricesHistory;
import pl.robotix.cinx.graph.PricesHistory.History;

public class PricesTest {
	
	public static final Currency LTC = new Currency("LTC");
	public static final Currency USDT = new Currency("USDT");
	public static final Currency BTC = new Currency("BTC");
	public static final Currency ETH = new Currency("ETH");
	
	public static final Pair USDT_ETH = new Pair(USDT, ETH);
	public static final Pair ETH_LTC = new Pair(ETH, LTC);
	public static final Pair USDT_BTC = new Pair(USDT, BTC);
	public static final Pair BTC_LTC = new Pair(BTC, LTC);

	@Test
	public void findsPriceChain() {
		
		Map<Pair, BigDecimal> prices = new HashMap<>();
		prices.put(USDT_ETH, new BigDecimal("1.1"));
		prices.put(ETH_LTC, new BigDecimal("1.2"));
		prices.put(USDT_BTC, new BigDecimal("1.32"));
		prices.put(BTC_LTC, new BigDecimal("1.0"));
		
		Map<Pair, BigDecimal> volumes = new HashMap<>();
		volumes.put(USDT_ETH, new BigDecimal("100"));
		volumes.put(ETH_LTC, new BigDecimal("0.200"));
		volumes.put(USDT_BTC, new BigDecimal("1.0"));
		volumes.put(BTC_LTC, new BigDecimal("10.0"));
		
		assertEquals(null, new BigDecimal("1.32").doubleValue(),
				new Prices(prices, volumes).getUSDFor(LTC).doubleValue(), 0.0001);
		
//		assertEquals(Arrays.asList(ETH_LTC, USDT_ETH),
//				new Prices(prices, volumes).pairsToComputeUSDFor(LTC));
		assertEquals(Arrays.asList(USDT_BTC, BTC_LTC),
				new Prices(prices, volumes).pairsToComputeUSDFor(LTC));

		assertEquals(Arrays.asList(new Pair[] {}),
				new Prices(prices, volumes).pairsToComputeUSDFor(USDT));

		assertEquals(Arrays.asList(new Pair[] {}),
				new Prices(prices, volumes).pairsToComputeBTCFor(BTC));

		assertEquals(Arrays.asList(USDT_BTC.reverse()),
				new Prices(prices, volumes).pairsToComputeBTCFor(USDT));

		assertEquals(Arrays.asList(BTC_LTC),
				new Prices(prices, volumes).pairsToComputeBTCFor(LTC));

	}
	
	@Test
	public void combinesZero() {
		History h1 = new History(BTC_LTC, Arrays.asList(
				new Point(ofEpochSecond(0, 0, UTC), 0, 0)
			), true, new TimeValues(1, 1));
		
		History h2 = new History(USDT_BTC, Arrays.asList(
				new Point(ofEpochSecond(0, 0, UTC), 0, 0)
			), false, new TimeValues(1, 1));
		
		ArrayList<Point> combined = PricesHistory.combine(Arrays.asList(h1, h2));

		assertTrue("Zero histories", historyEquals(combined,
				new Point(ofEpochSecond(0, 0, UTC), 0, 0)
			));


	}
	
	@Test
	public void combinesSameLength() {
		History h1 = new History(BTC_LTC, Arrays.asList(
				new Point(ofEpochSecond(0, 0, UTC), 1, 5),
				new Point(ofEpochSecond(0, 0, UTC), 2, 4),
				new Point(ofEpochSecond(0, 0, UTC), 3, 3)
			), true, new TimeValues(3, 1));
		
		History h2 = new History(USDT_BTC, Arrays.asList(
				new Point(ofEpochSecond(0, 0, UTC), 10, 0),
				new Point(ofEpochSecond(0, 0, UTC), 20, 0),
				new Point(ofEpochSecond(0, 0, UTC), 30, 0)
			), false, new TimeValues(3, 1));
		
		ArrayList<Point> combined = PricesHistory.combine(Arrays.asList(h1, h2));

		assertTrue("Same length", historyEquals(combined,
				new Point(ofEpochSecond(0, 0, UTC), 10, 5),
				new Point(ofEpochSecond(0, 0, UTC), 40, 4),
				new Point(ofEpochSecond(0, 0, UTC), 90, 3)
			));
	}
	
	@Test
	public void combinesVariousLength() {
		History h1 = new History(BTC_LTC, Arrays.asList(
				new Point(ofEpochSecond(0, 0, UTC), 1, 5),
				new Point(ofEpochSecond(0, 0, UTC), 2, 4)
			), true, new TimeValues(3, 1));
		
		History h2 = new History(USDT_BTC, Arrays.asList(
				new Point(ofEpochSecond(0, 0, UTC), 10, 0),
				new Point(ofEpochSecond(0, 0, UTC), 20, 0),
				new Point(ofEpochSecond(0, 0, UTC), 30, 0)
			), false, new TimeValues(3, 1));
		
		ArrayList<Point> combined = PricesHistory.combine(Arrays.asList(h1, h2));

		assertTrue("Same length", historyEquals(combined,
				new Point(ofEpochSecond(0, 0, UTC), 10, 0),
				new Point(ofEpochSecond(0, 0, UTC), 20, 5),
				new Point(ofEpochSecond(0, 0, UTC), 60, 4)
			));
	}
	
	@Test
	public void combinesVariousDensity() {
		History h1 = new History(BTC_LTC, Arrays.asList(
				new Point(ofEpochSecond(0, 0, UTC), 1, 10),
				new Point(ofEpochSecond(0, 0, UTC), 2, 20),
				new Point(ofEpochSecond(0, 0, UTC), 3, 30),
				new Point(ofEpochSecond(0, 0, UTC), 4, 40)
			), false, new TimeValues(7200, 1800));
		
		History h2 = new History(USDT_BTC, Arrays.asList(
				new Point(ofEpochSecond(0, 0, UTC), 10, 1),
				new Point(ofEpochSecond(0, 0, UTC), 20, 2)
			), true, new TimeValues(7200, 3600));
		
		ArrayList<Point> combined = PricesHistory.combine(Arrays.asList(h1, h2));

		assertTrue("Same length", historyEquals(combined,
				new Point(ofEpochSecond(0, 0, UTC), 10, 1),
				new Point(ofEpochSecond(0, 0, UTC), 20, 1),
				new Point(ofEpochSecond(0, 0, UTC), 60, 2),
				new Point(ofEpochSecond(0, 0, UTC), 80, 2)
			));
	}
	
	@Test
	public void combinesVariousDensityAndLength() {
		History h1 = new History(BTC_LTC, Arrays.asList(
				new Point(ofEpochSecond(0, 0, UTC), 1, 10),
				new Point(ofEpochSecond(0, 0, UTC), 2, 20),
				new Point(ofEpochSecond(0, 0, UTC), 3, 30),
				new Point(ofEpochSecond(0, 0, UTC), 4, 40),
				new Point(ofEpochSecond(0, 0, UTC), 5, 50),
				new Point(ofEpochSecond(0, 0, UTC), 6, 60)
			), false, new TimeValues(10800, 1800));
		
		History h2 = new History(USDT_BTC, Arrays.asList(
				new Point(ofEpochSecond(0, 0, UTC), 10, 1),
				new Point(ofEpochSecond(0, 0, UTC), 20, 2)
			), true, new TimeValues(10800, 3600));
		
		ArrayList<Point> combined = PricesHistory.combine(Arrays.asList(h1, h2));

		assertTrue("Same length", historyEquals(combined,
				new Point(ofEpochSecond(0, 0, UTC), 10, 0),
				new Point(ofEpochSecond(0, 0, UTC), 20, 0),
				new Point(ofEpochSecond(0, 0, UTC), 30, 1),
				new Point(ofEpochSecond(0, 0, UTC), 40, 1),
				new Point(ofEpochSecond(0, 0, UTC), 100, 2),
				new Point(ofEpochSecond(0, 0, UTC), 120, 2)
			));
	}

	private boolean historyEquals(ArrayList<Point> points, Point ...points2) {
		if (points == null) { return points2 == null; }
		else if (points2 == null) { return false; }
		
		if (points.size() != points2.length) { return false; }
		
		for (int i = 0; i < points.size(); i++) {
			if (!equalValues(points.get(i), points2[i])) {
				return false;
			}
		}
		
		return true;
	}
	
	private boolean equalValues(Point p1, Point p2) {
		return p1.price == p2.price && p1.volume == p2.volume;
	}

}
