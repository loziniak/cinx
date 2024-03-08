package pl.robotix.cinx;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparing;
import static pl.robotix.cinx.Currency.BTC;
import static pl.robotix.cinx.Currency.USDT;
import static pl.robotix.cinx.Pair.USDT_BTC;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import pl.robotix.cinx.api.AsyncApi;

public class Prices {
	
	private Map<Pair, BigDecimal> prices = new HashMap<>();
	private final Map<Currency, BigDecimal> volumes = new HashMap<>();
	
	private AsyncApi api;

	public Prices() {
	}

	public Prices(AsyncApi api) {
		this.api = api;
		retrieveFor(api.pairsForMarket(BTC)
				.stream().map(p -> p.base).collect(Collectors.toSet())); // TODO: trade with USDT, not BTC. bigger volumes.
	}

	public Prices(Map<Pair, BigDecimal> prices, Map<Pair, BigDecimal> pairVolumes) {
		System.out.println("Prices(prices, pairVolumes)...");
		prices.forEach((pair, price) -> {
			try {
				this.prices.put(pair, price);
				this.prices.put(pair.reverse(),
						price.doubleValue() == 0.0 ? ZERO : ONE.divide(price, MathContext.DECIMAL64));
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		});
		
		pairVolumes.entrySet().forEach((entry) -> {
			addToVolumes(entry.getKey(), entry.getValue());
			addToVolumes(entry.getKey().reverse(), entry.getValue());
		});
	}
	
	
	public BigDecimal getUSDFor(Currency currency) {
		return getRate(new Pair(USDT, currency));
	}
	
	public BigDecimal getRate(Pair pair) {
//		System.out.println("getRate: "+pair);
		if (pair.base.equals(pair.quote)) {
			return ONE;
		}
		
		BigDecimal foundPrice = prices.get(pair);
		if (foundPrice != null) {
			return foundPrice;
		}
		
		BigDecimal foundReversePrice = prices.get(pair.reverse());
		if (foundReversePrice != null) {
			foundPrice = ONE.divide(foundReversePrice, MathContext.DECIMAL64);
			prices.put(pair, foundPrice);
			return foundPrice;
		}
		
		if (pair.quote.equals(BTC)) {
			return null;
		}
		
		return getRate(new Pair(pair.quote, BTC)  ).multiply(  getRate(new Pair(BTC, pair.base)));
	}

	public List<Pair> pairsToComputeUSDFor(final Currency currency) {
		return pairsToComputePrice(USDT, currency);
	}
	
	public List<Pair> pairsToComputeBTCFor(final Currency currency) {
		return pairsToComputePrice(BTC, currency);
	}
	
	public Set<Currency> getAllCurrencies() {
		Set<Currency> currencies = new HashSet<>();
		prices.keySet().forEach((pair) -> {
			currencies.add(pair.base);
			currencies.add(pair.quote);
		});
		return currencies;
	}
	
	public void retrieveFor(Collection<Currency> currencies) {
		System.out.println("retrieveFor...");
		var pairs = currencies.stream()
				.map((cur) -> new Pair(BTC, cur)) // TODO: trade with USDT, not BTC. bigger volumes.
				.collect(Collectors.toList());

		pairs.add(USDT_BTC);
		
		retrieveMissing(pairs);
	}
	
	public void switchQuotes(BigDecimal usdtBtc) {
		HashMap<Pair, BigDecimal> newPrices = new HashMap<>();
		for (Entry<Pair, BigDecimal> price : prices.entrySet()) {

			if        (price.getKey().quote.equals(BTC)) {
				newPrices.put(
						new Pair(USDT, price.getKey().base),
						price.getValue().divide(usdtBtc, MathContext.DECIMAL64));

			} else if (price.getKey().quote.equals(USDT)) {
				newPrices.put(
						new Pair(BTC, price.getKey().base),
						price.getValue().multiply(usdtBtc));
				
			} else {
				newPrices.put(
						price.getKey(),
						price.getValue()
					);
			}
		}
		prices = newPrices;
	}
	
	public Comparator<Currency> byVolume() {
		return reverseOrder(comparing((Currency c) -> { return volumes.get(c); }));
	}
	
	public void join(Prices other) {
		this.prices.putAll(other.prices);
		this.volumes.putAll(other.volumes);
		
		for (Map.Entry<Pair, BigDecimal> entry : other.prices.entrySet()) {
			Pair pair = entry.getKey();

			if (!pair.quote.equals(BTC)) { // TODO: trade with USDT, not BTC. bigger volumes.
				Pair toBtc = new Pair(BTC, pair.base);
				prices.put(toBtc, getRate(toBtc));
			}
		}
	}
	
	public void setApi(AsyncApi api) {
		this.api = api;
	}

	
	private List<Pair> pairsToComputePrice(final Currency quote, final Currency base) {
		if (base.equals(quote)) {
			return Collections.emptyList();
		}
		
		Pair pair = new Pair(quote, base);
		BigDecimal foundPrice = prices.get(pair);
		if (foundPrice != null) {
			return Arrays.asList(pair);
		}
		
		Pair revPair = pair.reverse();
		BigDecimal foundReversePrice = prices.get(revPair);
		if (foundReversePrice != null) {
			foundPrice = ONE.divide(foundReversePrice, MathContext.DECIMAL64);
			prices.put(pair, foundPrice);
			return Arrays.asList(pair);
		}
		
		List<Pair> intermediates = Arrays.asList(new Pair(quote, BTC), new Pair(BTC, base));
		retrieveMissing(intermediates);
		
		return intermediates;
	}
	
	private void addToVolumes(Pair pair, BigDecimal volume) {
		Currency base = pair.base;
		Currency quote = pair.quote;
		BigDecimal baseVolume = volume;
		BigDecimal usdBaseVolume = baseVolume.multiply(getUSDFor(quote));

		addToVolumes(base, usdBaseVolume);
		addToVolumes(quote, usdBaseVolume);
	}
	
	private void addToVolumes(Currency c, BigDecimal usdVolume) {
		BigDecimal volumeSum = volumes.get(c);
		if (volumeSum == null) {
			volumes.put(c, usdVolume);
		} else {
			volumes.put(c, volumeSum.add(usdVolume));
		}
	}
	
	private void retrieveMissing(Collection<Pair> toRetrieve) {
		System.out.println("retrieveMissing...");
		var toRet = new HashSet<Pair>(toRetrieve);
		toRet.removeAll(this.prices.keySet());
		toRet.removeIf((p) -> p.base.equals(p.quote));

		if (!toRet.isEmpty()) {
			join(api.retrievePrices(toRet));
		}
	}

}
