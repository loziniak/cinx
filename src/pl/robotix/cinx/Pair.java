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
	
	/**
	 * @param symbol e.g. USDT_BCH
	 */
	public Pair(String symbol) {
		String[] currencySymbols = symbol.split("_");
		quote = new Currency(currencySymbols[0]);
		base = new Currency(currencySymbols[1]);
	}
	
	public Pair(Currency quote, Currency base) {
		this.quote = quote;
		this.base = base;
	}

	public Pair reverse() {
		return new Pair(base, quote);
	}

	@Override
	public String toString() {
		return quote.symbol + "_" + base.symbol;
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
