package pl.robotix.cinx.api.binance;

import static java.math.MathContext.DECIMAL64;
import static java.util.stream.Collectors.toCollection;
import static pl.robotix.cinx.Currency.BTC;
import static pl.robotix.cinx.Currency.USDT;
import static pl.robotix.cinx.Pair.USDT_BTC;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.binance.connector.client.SpotClient;
import com.binance.connector.client.enums.DefaultUrls;
import com.binance.connector.client.exceptions.BinanceClientException;
import com.binance.connector.client.impl.SpotClientImpl;
import com.binance.connector.client.utils.signaturegenerator.Ed25519SignatureGenerator;

import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Point;
import pl.robotix.cinx.Prices;
import pl.robotix.cinx.TimeRange;
import pl.robotix.cinx.api.OperationException;
import pl.robotix.cinx.api.SyncApi;
import pl.robotix.cinx.trade.Operation;

public class BinanceApi implements SyncApi {
	
	private static final Currency MARKET_QUOTE = BTC; // TODO: trade with USDT pairs (bigger volumes)
	
	private static final String USDT_BTC_SYMBOL = pairString(USDT_BTC);
	
	private static final HashMap<TimeRange, TimeRngInterval> TIME_INTERVALS = new HashMap<TimeRange, TimeRngInterval>();
	
	private static final BigDecimal TWO = BigDecimal.valueOf(2);
	
	private SpotClient client;
	
	private ExchangeInfo exchange;

	private Set<Currency> pairsForMarket;

	private Account account;


	public BinanceApi(String apiKey, String secret) throws FileNotFoundException, IOException {
		
		client = new SpotClientImpl("wgxkUzsuKuOor7YLkDripGXrJDMLBnvpfqmiaBAPJoeN7KYXRb9gOTNxAIzr8Y9A",
				"WxHeNqDLVgVRNSS3u9ANKiTKvJAIXYi2dx95XEOCLrwEfRUR1SVOkMWC9Y9k7f8X", DefaultUrls.TESTNET_URL);
//		client = new SpotClientImpl(apiKey, secret, DefaultUrls.TESTNET_URL);
		
		account = accountInfo();
		exchange = new ExchangeInfo(client.createMarket().exchangeInfo(emptyParams()));
		pairsForMarket = pairsForMarket(MARKET_QUOTE);
		pairsForMarket.add(MARKET_QUOTE);
	}
	
	@Override
	public void initTimeRanges() {
		TimeRange.init(
				TimeRngInterval._5m.seconds,
				TimeRngInterval._30m.seconds,
				TimeRngInterval._2h.seconds,
				TimeRngInterval._1d.seconds);

		TIME_INTERVALS.put(TimeRange.DAY, TimeRngInterval._5m);
		TIME_INTERVALS.put(TimeRange.WEEK, TimeRngInterval._30m);
		TIME_INTERVALS.put(TimeRange.MONTH, TimeRngInterval._2h);
		TIME_INTERVALS.put(TimeRange.YEAR, TimeRngInterval._1d);
	}

	@Override
	public Map<Currency, BigDecimal> retrieveBalance() {
		var balance = new HashMap<Currency, BigDecimal>();
		account.getBalances().stream()
//				.skip(5).limit(10) // TODO: just for testnet
				.forEach((bal) -> {
			var cur = new Currency(bal.getAsset());
			if (bal.getFree().signum() != 0 && isExchangeable(cur)) {
				System.out.println("retrieveBalance: " + cur.symbol);
				balance.put(cur, bal.getFree());
			}
		});

		return balance;
	}

	@Override
	public void buy(Pair pair, BigDecimal rate, BigDecimal amount) throws OperationException {
		marketOrder(pair, amount, Operation.Type.BUY);
	}

	@Override
	public void sell(Pair pair, BigDecimal rate, BigDecimal amount) throws OperationException {
		marketOrder(pair, amount, Operation.Type.SELL);
	}

	@Override
	public ArrayList<Point> retrievePriceHistory(Pair pair, TimeRange range) {
		Function<Kline, Point> pointCreator;
		if (pair.isReverse()) {
			pointCreator = (kline) -> new Point(kline.getCloseTime(),
					1.0 / avgPrice(kline), kline.getVolume().doubleValue());
			pair = pair.reverse();
		} else {
			pointCreator = (kline) -> new Point(kline.getCloseTime(),
					avgPrice(kline), kline.getVolume().doubleValue());
		}
		
		var params = emptyParams();
		params.put("symbol", pairString(pair));
		params.put("interval", TIME_INTERVALS.get(range).paramVal());
		params.put("limit", range.getPointsCount());
		var json = client.createMarket().klines(params);
		var points = new JSONArray(json).toList().stream()
				.map((obj) -> {
					return pointCreator.apply(new Kline(obj));
				})
				.collect(toCollection(ArrayList::new));

		return points;
	}

	@Override
	public Prices retrievePrices() {
		throw new UnsupportedOperationException("Not implemented");
	}
	
	@Override
	public Prices retrievePrices(Collection<Pair> pairs) {
		var params = emptyParams();
		var symbols = pairs.stream()
				.map((p) -> p.isReverse() ? pairString(p.reverse()) : pairString(p))
				.collect(Collectors.toSet());
		symbols.add(pairString(USDT_BTC));

		params.put("symbols", new ArrayList<String>(symbols));
		params.put("type", "MINI");
		var json = client.createMarket().ticker24H(params);
		var tickers = new JSONArray(json);
		
		Map<Pair, BigDecimal> prices = new HashMap<>();
		Map<Pair, BigDecimal> volumes = new HashMap<>();
		tickers.forEach((o) -> {
			var ticker = new Ticker((JSONObject) o);
			System.out.println("retrievePrices ticker: " + ticker.getSymbol());
			try {
				var pair = pair(ticker.getSymbol());
				prices.put(pair, ticker.getLastPrice());
				volumes.put(pair, ticker.getQuoteVolume());
			} catch (IllegalArgumentException e) {
				System.out.println("Illegal ticker symbol: "+ticker.getSymbol());
			}
		});
		
		return new Prices(prices, volumes);
	}
	
	@Override
	public Set<Currency> pairsForMarket(Currency c) {
		if (c.equals(MARKET_QUOTE) && pairsForMarket != null) {
			return pairsForMarket;
		}
		return exchange.getSymbols().stream()
				.filter((s) -> s.isSpot() && s.isMarket())
				.filter((s) -> s.toPair().quote.equals(c))
				.map((s) -> s.toPair().base)
				.collect(Collectors.toSet());
	}

	@Override
	public boolean isExchangeable(Currency c) {
		return c.equals(USDT) || c.equals(BTC) ||  pairsForMarket.contains(c);
	}

	@Override
	public double takerFee() {
		return account.getTakerRate();
	}

	
	private Account accountInfo() {
		var json = client.createTrade().account(emptyParams());
		return new Account(json);
	}
	
	private void marketOrder(Pair pair, BigDecimal baseAmount, Operation.Type operation) throws OperationException {
		var params = emptyParams();
		params.put("symbol", pairString(pair));
		params.put("side", operation.name());
		params.put("type", "MARKET");
		params.put("newOrderRespType", "RESULT");

		BigDecimal stepSize = exchange.getSymbol(pair).getStepSize();
		params.put("quantity", formatDouble(
				baseAmount.divide(stepSize).setScale(0, RoundingMode.DOWN).multiply(stepSize)
			));
		
		try {
			String response = client.createTrade().newOrder(params);
			String status = new JSONObject(response).getString("status");
			if (!status.equals("FILLED")) {
				throw new OperationException("Unexpected response status: "+status);
			}
		} catch (BinanceClientException e) {
			System.err.println(pair.toString() + " " + operation + " client error: " +  e.getMessage());
			throw new OperationException(e.getErrMsg());
		}
	}
	
	private HashMap<String, Object> emptyParams() {
		return new HashMap<String, Object>();
	}

	private static Pair pair(String pairString) {
		if (!pairString.endsWith(MARKET_QUOTE.symbol)) {
			if (pairString.equals(USDT_BTC_SYMBOL)) {
				return USDT_BTC;
			} else {
				throw new IllegalArgumentException("We deal only with "+MARKET_QUOTE+" pairs.");
			}
		}
		return new Pair(
				MARKET_QUOTE,
				new Currency(pairString.substring(0, pairString.length() - MARKET_QUOTE.symbol.length()))
			);
	}
	
	private static String pairString(Pair pair) {
		return pair.base.symbol + pair.quote.symbol;
	}
	
	private static String formatDouble(BigDecimal value) {
		return String.format(Locale.ROOT, "%.8f", value);
	}
	
	private double avgPrice(Kline kline) {
		return kline.getLowPrice().add(kline.getHighPrice()).divide(TWO, DECIMAL64).doubleValue();
	}

	private static enum TimeRngInterval {
		_5m(5 * 60),
		_30m(30 * 60),
		_2h(2 * 60 * 60),
		_1d(24 * 60 * 60);
		
		private final long seconds;
		
		private TimeRngInterval(long seconds) {
			this.seconds = seconds;
		}
		
		public String paramVal() {
			return this.name().substring(1);
		}
	}

}
