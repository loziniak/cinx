package pl.robotix.cinx.api;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Prices;

public interface Api {
	
	void initTimeRanges();

	Map<Currency, BigDecimal> retrieveBalance();

	Prices retrievePrices();

	Prices retrievePrices(Collection<Pair> pairs);
	
	Collection<Currency> pairsForMarket(Currency c);
	
	String pairString(Pair pair);
	
	Pair pair(String pairString);

	boolean isExchangeable(Currency c);

}