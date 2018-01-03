package pl.robotix.cinx;

import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparing;
import static pl.robotix.cinx.App.USDT;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Prices {
	
	private Map<Pair, BigDecimal> prices = new HashMap<>();
	private final Map<Currency, BigDecimal> volumes = new HashMap<>();
	
	public Prices() {
	}
	
	public Prices(Map<Pair, BigDecimal> prices, Map<Pair, BigDecimal> pairVolumes) {
		this.prices.putAll(prices);
		
		pairVolumes.entrySet().forEach((entry) -> {
			addToVolumes(entry);
		});
	}
	
	
	public BigDecimal getUSDFor(Currency currency) {
		if (currency.equals(USDT)) {
			return BigDecimal.ONE;
		}
		
		BigDecimal foundPrice = prices.get(new Pair(USDT, currency));
		if (foundPrice != null) {
			return foundPrice;
		}
		
		final LinkedList<BigDecimal> priceHolder = new LinkedList<BigDecimal>();
		prices.forEach((pair, price) -> {
			if (priceHolder.isEmpty() && pair.base.equals(currency)) {
				Currency secondCurrency = pair.quote;
				BigDecimal secondPrice = getUSDFor(secondCurrency);
				if (secondPrice != null) {
					priceHolder.add(price.multiply(secondPrice));
				}
			}
		});
		
		if (priceHolder.size() > 0) {
			foundPrice = priceHolder.get(0);
		}
		
		return foundPrice;
	}

	public List<Pair> pairsToComputeUSDFor(final Currency currency) {
		if (currency.equals(USDT)) {
			return Collections.emptyList();
		}
		
		Pair currToUsd = new Pair(USDT, currency);
		BigDecimal foundPrice = prices.get(currToUsd);
		if (foundPrice != null) {
			return Arrays.asList(new Pair[]{ currToUsd });
		}
		
		final LinkedList<Pair> pairsHolder = new LinkedList<>();
		prices.forEach((pair, price) -> {
			if (pairsHolder.isEmpty() && pair.base.equals(currency)) {
				Currency secondCurrency = pair.quote;
				List<Pair> leftPairs = pairsToComputeUSDFor(secondCurrency);
				if (leftPairs != null) {
					pairsHolder.add(new Pair(secondCurrency, currency));
					pairsHolder.addAll(leftPairs);
				}
			}
		});
		
		return pairsHolder;
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
