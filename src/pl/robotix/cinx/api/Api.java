package pl.robotix.cinx.api;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Prices;
import pl.robotix.cinx.TimeRange;

public interface Api {
	
	void initTimeRanges();

	Map<Currency, BigDecimal> retrieveBalance();

	Prices retrievePrices();

	Prices retrievePrices(Collection<Pair> pairs);
	
	Collection<Pair> pairsForMarket(Currency c);
	
	boolean isExchangeable(Currency c);
	
	double takerFee();
	
	TimeValues timeValues(TimeRange range, Currency currency);

}