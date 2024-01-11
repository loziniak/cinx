package pl.robotix.cinx;

import static pl.robotix.cinx.Currency.BTC;
import static pl.robotix.cinx.Currency.USDT;

public class Pair {
	
	public static final Pair USDT_BTC = new Pair(USDT, BTC);
	
	/**
	 * e.g. USDT
	 */
	public final Currency quote;

	/**
	 * e.g. BCH
	 */
	public final Currency base;
	
	
	public Pair(Currency quote, Currency base) {
		this.quote = quote;
		this.base = base;
	}

	public Pair(String quote, String base) {
		this.quote = new Currency(quote);
		this.base = new Currency(base);
	}

	public Pair reverse() {
		return new Pair(base, quote);
	}
	
	public boolean isReverse() { // TODO: depend on system's MARKET_QUOTE
		return base.equals(USDT)
				|| base.equals(BTC) && !quote.equals(USDT);
	}

	@Override
	public String toString() {
		return quote.symbol + "/" + base.symbol;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Pair)) {
			return false;
		}
		Pair other = (Pair) obj;
		return this.base.equals(other.base) && this.quote.equals(other.quote);
	}
	
	@Override
	public int hashCode() {
		return base.hashCode() + quote.hashCode();
	}

}
