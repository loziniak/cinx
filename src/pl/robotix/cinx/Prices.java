package pl.robotix.cinx;

import static java.math.BigDecimal.ONE;
import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparing;
import static pl.robotix.cinx.Currency.BTC;
import static pl.robotix.cinx.Currency.USDT;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Prices {
	
	private Map<Pair, BigDecimal> prices = new HashMap<>();
	private final Map<Currency, BigDecimal> volumes = new HashMap<>();
	
	public Prices(Map<Pair, BigDecimal> prices, Map<Pair, BigDecimal> pairVolumes) {
		this.prices.putAll(prices);
		prices.forEach((pair, price) -> {
			try {
				this.prices.put(pair.reverse(), ONE.divide(price, MathContext.DECIMAL64));
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		});
		
		pairVolumes.entrySet().forEach((entry) -> {
			addToVolumes(entry);
		});
	}
	
	
	public BigDecimal getUSDFor(Currency currency) {
		return getRate(new Pair(USDT, currency));
	}
	
	public BigDecimal getRate(Pair pair) {
		if (pair.base.equals(pair.quote)) {
			return ONE;
		}
		
		BigDecimal foundPrice = prices.get(pair);
		if (foundPrice != null) {
			return foundPrice;
		}
		
		return getRate(new Pair(pair.quote, BTC)  ).multiply(  getRate(new Pair(BTC, pair.base)));
	}

	public List<Pair> pairsToComputeUSDFor(final Currency currency) {
		return pairsToComputePrice(USDT, currency);
	}
	
	public List<Pair> pairsToComputeBTCFor(final Currency currency) {
		return pairsToComputePrice(BTC, currency);
	}
	
	private List<Pair> pairsToComputePrice(final Currency quote, final Currency base) {
		if (base.equals(quote)) {
			return Collections.emptyList();
		}
		
		Pair currToX = new Pair(quote, base);
		BigDecimal foundPrice = prices.get(currToX);
		if (foundPrice != null) {
			return Arrays.asList(currToX);
		}
		
		return Arrays.asList(new Pair(quote, BTC), new Pair(BTC, base));
	}
	
	public Set<Currency> getAllCurrencies() {
		Set<Currency> currencies = new HashSet<>();
		prices.keySet().forEach((pair) -> {
			currencies.add(pair.base);
			currencies.add(pair.quote);
		});
		return currencies;
	}
	
	public Comparator<Currency> byVolume() {
		return reverseOrder(comparing((Currency c) -> { return volumes.get(c); }));
	}

	
	private void addToVolumes(Entry<Pair, BigDecimal> entry) {
		Currency base = entry.getKey().base;
		Currency quote = entry.getKey().quote;
		BigDecimal baseVolume = entry.getValue();
		BigDecimal usdBaseVolume = baseVolume.multiply(getUSDFor(quote));

		BigDecimal volumeSum = volumes.get(base);
		if (volumeSum == null) {
			volumes.put(base, usdBaseVolume);
		} else {
			volumes.put(base, volumeSum.add(usdBaseVolume));
		}
		
		BigDecimal quoteVolumeSum = volumes.get(quote);
		if (quoteVolumeSum == null) {
			volumes.put(quote, usdBaseVolume);
		} else {
			volumes.put(quote, quoteVolumeSum.add(usdBaseVolume));
		}
	}

}
