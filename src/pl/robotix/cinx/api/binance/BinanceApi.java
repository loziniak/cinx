package pl.robotix.cinx.api.binance;

import static pl.robotix.cinx.Currency.BTC;
import static pl.robotix.cinx.Currency.USDT;
import static pl.robotix.cinx.Pair.USDT_BTC;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.binance.connector.client.SpotClient;
import com.binance.connector.client.enums.DefaultUrls;
import com.binance.connector.client.impl.SpotClientImpl;

import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Point;
import pl.robotix.cinx.Prices;
import pl.robotix.cinx.TimeRange;
import pl.robotix.cinx.api.SyncApi;

public class BinanceApi implements SyncApi {
	
	private static final Currency MARKET_QUOTE = BTC; // TODO: trade with USDT pairs (bigger volumes)
	
	private static final String USDT_BTC_SYMBOL = pairStringStatic(USDT_BTC);
	
	private static final Map<TimeRange, TimeRngInterval> TIME_INTERVALS = new HashMap<>();
	
	private SpotClient client;
	
	private ExchangeInfo exchange;

	private Set<Currency> pairsForMarket;


	public BinanceApi(String apiKey, String secret) {
		
		client = new SpotClientImpl("wgxkUzsuKuOor7YLkDripGXrJDMLBnvpfqmiaBAPJoeN7KYXRb9gOTNxAIzr8Y9A",
				"WxHeNqDLVgVRNSS3u9ANKiTKvJAIXYi2dx95XEOCLrwEfRUR1SVOkMWC9Y9k7f8X", DefaultUrls.TESTNET_URL);
//		client = new SpotClientImpl(apiKey, secret, DefaultUrls.TESTNET_URL);
		
		exchange = new ExchangeInfo(client.createMarket().exchangeInfo(emptyParams()));
		pairsForMarket = pairsForMarket(MARKET_QUOTE);
		pairsForMarket.add(BTC);
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
		var json = client.createTrade().account(emptyParams());
		var account = new Account(json);

		var balance = new HashMap<Currency, BigDecimal>();
		account.getBalances().forEach((bal) -> {
			var cur = new Currency(bal.getAsset());
			if (isExchangeable(cur)) {
				System.out.println("retrieveBalance: " + cur.symbol);
				balance.put(cur, bal.getFree());
			}
		});

		return balance;
	}

	@Override
	public boolean buy(Pair pair, BigDecimal rate, BigDecimal amount) {
		// TODO implement
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public boolean sell(Pair pair, BigDecimal rate, BigDecimal amount) {
		// TODO implement
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public List<Point> retrievePriceHistory(Pair pair, TimeRange range) {
		// TODO Auto-generated method stub
		return null;
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

//		// Retrieve each symbol separately.
//		// Good for debug in case of "Invalid symbol" response:
//		var symbolsParam = new ArrayList<String>();
//		symbols.forEach((s) -> {
//			System.out.println("retrievePrices: "+s);
//			symbolsParam.clear();
//			symbolsParam.add(s);
//			params.put("symbols", symbolsParam);
//			params.put("type", "MINI");
//			var json = client.createMarket().ticker24H(params);
//			System.out.println(json);
//		});
		
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
				volumes.put(pair, ticker.getVolume());
			} catch (IllegalArgumentException e) {
				System.out.println("Illegal ticker symbol: "+ticker.getSymbol());
			}
		});
		
		return new Prices(prices, volumes);
	}
	
	@Override
	public Set<Currency> pairsForMarket(Currency c) {
		return exchange.getSymbols().stream()
				.filter((s) -> s.toPair().quote.equals(c))
				.map((s) -> s.toPair().base)
				.collect(Collectors.toSet());
	}

	@Override
	public String pairString(Pair pair) {
		return pairStringStatic(pair);
	}
	
	@Override
	public Pair pair(String pairString) {
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
	
	@Override
	public boolean isExchangeable(Currency c) {
		return c.equals(USDT) || c.equals(BTC) ||  pairsForMarket.contains(c);
	}
	
	private HashMap<String, Object> emptyParams() {
		return new HashMap<String, Object>();
	}

	private static String pairStringStatic(Pair pair) {
		return pair.base.symbol + pair.quote.symbol;
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
		
		public String val() {
			return this.name().substring(1);
		}
	}

}
