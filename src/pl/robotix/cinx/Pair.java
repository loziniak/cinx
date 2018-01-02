package pl.robotix.cinx;

public class Pair {
	
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
