package pl.robotix.cinx.api.bitmart;

import static java.math.MathContext.DECIMAL64;
import static java.util.stream.Collectors.toCollection;
import static pl.robotix.cinx.Currency.USDT;
import static pl.robotix.cinx.TimeRange.DAY;
import static pl.robotix.cinx.TimeRange.TWO_MONTHS;
import static pl.robotix.cinx.TimeRange.WEEK;
import static pl.robotix.cinx.TimeRange.YEAR;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bitmart.api.Call;
import com.bitmart.api.CloudContext;
import com.bitmart.api.common.CloudException;
import com.bitmart.api.common.CloudResponse;
import com.bitmart.api.request.spot.pub.market.V3LatestKlineRequest;
import com.bitmart.api.request.spot.pub.market.V3TickerRequest;

import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Point;
import pl.robotix.cinx.Prices;
import pl.robotix.cinx.TimeRange;
import pl.robotix.cinx.api.OperationException;
import pl.robotix.cinx.api.SyncApi;
import pl.robotix.cinx.api.TimeValues;
import pl.robotix.cinx.graph.PricesHistory;
import pl.robotix.cinx.graph.PricesHistory.History;

public class BitMartApi implements SyncApi {
	
	private static final BigDecimal TWO = BigDecimal.valueOf(2);

	private static final Currency EMAID = new Currency("EMAID");
	private static final Currency BSV = new Currency("BSV");

	private static final List<Pair> PAIRS = Arrays.asList(
			new Pair(USDT, EMAID),
			new Pair(USDT, BSV));
	
	private static final String JSON_DATA = "data";

	private static final HashMap<TimeRange, Long> STEP_MINUTES = new HashMap<>();
	private static final HashMap<TimeRange, TimeValues> TIME_VALUES = new HashMap<>();

	
	private final CloudContext ctx;
	
	public BitMartApi() {
		ctx = new CloudContext();
	}

	@Override
	public void initTimeRanges() {
		STEP_MINUTES.put(DAY, 15L);
		STEP_MINUTES.put(WEEK, 60L);
		STEP_MINUTES.put(TWO_MONTHS, 4L * 60L);
		STEP_MINUTES.put(YEAR, 7L * 24L * 60L);
		
		TIME_VALUES.put(DAY, new TimeValues(DAY.seconds, STEP_MINUTES.get(DAY) * 60));
		TIME_VALUES.put(WEEK, new TimeValues(WEEK.seconds, STEP_MINUTES.get(WEEK) * 60));
		TIME_VALUES.put(TWO_MONTHS, new TimeValues(TWO_MONTHS.seconds, STEP_MINUTES.get(TWO_MONTHS) * 60));
		TIME_VALUES.put(YEAR, new TimeValues(YEAR.seconds, STEP_MINUTES.get(YEAR) * 60));
	}

	@Override
	public Prices retrievePrices(Collection<Pair> pairs) {
		Call call = new Call(ctx);
		
		Map<Pair, BigDecimal> prices = new HashMap<>();
		Map<Pair, BigDecimal> volumes = new HashMap<>();
		for (Pair pair : pairs) {
			try {
				CloudResponse res = call.callCloud(new V3TickerRequest()
						.setSymbol(toString(pair)));
				var data = (JSONObject) new JSONObject(res.getResponseContent()).get(JSON_DATA);
				var ticker = new Ticker(data);

				System.out.println("(BitMart) retrievePrices ticker: " + ticker.getSymbol());
				prices.put(pair, ticker.getLastPrice());
				volumes.put(pair, ticker.getQuoteVolume());
				
				Thread.sleep(200);

			} catch (CloudException e) {
				System.out.println("(BitMart) Could not retrieve ticker for " + pair);
				e.printStackTrace();

			} catch (InterruptedException e) {
				// not a problem
			}
			
		}
		
		return new Prices(prices, volumes);
	}

	@Override
	public Prices retrievePrices() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PricesHistory.History retrievePriceHistory(Pair pair, TimeRange range) {
		Function<Kline, Point> pointCreator;
		if (pair.isReverse()) {
			pointCreator = (kline) -> new Point(kline.getCloseTime(),
					1.0 / avgPrice(kline), kline.getVolume().doubleValue());
			pair = pair.reverse();
		} else {
			pointCreator = (kline) -> new Point(kline.getCloseTime(),
					avgPrice(kline), kline.getVolume().doubleValue());
		}

		List<Point> ret = new ArrayList<>();
		Call call = new Call(ctx);
		try {
			V3LatestKlineRequest req = new V3LatestKlineRequest()
					.setSymbol(toString(pair))
					.setStep(STEP_MINUTES.get(range).intValue())
					.setLimit((int) timeValues(range, pair.base).getPointsCount())
					.setAfter(timeValues(range, pair.base).getStart());

			System.out.println(req.toString());

			CloudResponse res = call.callCloud(req);
			var data = (JSONArray) new JSONObject(res.getResponseContent()).get(JSON_DATA);

			ret = data.toList().stream()
					.map(Kline::new)
					.map(pointCreator)
					.collect(toCollection(ArrayList::new));

		} catch (CloudException e) {
			System.out.println("(BitMart) Could not retrieve klines for " + pair);
			e.printStackTrace();
		}

		return new History(pair, ret, false, this.timeValues(range, null));
	}

	@Override
	public Collection<Pair> pairsForMarket(Currency c) {
//		return c.equals(USDT) ? PAIRS : Collections.emptySet();
		return PAIRS;
	}

	@Override
	public boolean isExchangeable(Currency c) {
		return PAIRS.contains(new Pair(USDT, c));
	}

	@Override
	public TimeValues timeValues(TimeRange range, Currency currency) {
		return TIME_VALUES.get(range);
	}

	@Override
	public Map<Currency, BigDecimal> retrieveBalance() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double takerFee() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void buy(Pair pair, BigDecimal rate, BigDecimal amount) throws OperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sell(Pair pair, BigDecimal rate, BigDecimal amount) throws OperationException {
		throw new UnsupportedOperationException();
	}

	
	private static final String toString(Pair p) {
		return p.base.symbol + "_" + p.quote.symbol;
	}

	private double avgPrice(Kline kline) {
		return kline.getLowPrice().add(kline.getHighPrice()).divide(TWO, DECIMAL64).doubleValue();
	}

}
