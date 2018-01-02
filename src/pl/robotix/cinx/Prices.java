package pl.robotix.cinx;

import static pl.robotix.cinx.App.USDT;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Prices {
	
	Map<Pair, BigDecimal> prices = new HashMap<>();
	
	public Prices() {
	}
	
	public Prices(Map<Pair, BigDecimal> prices) {
		this.prices.putAll(prices);
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

}
