package pl.robotix.cinx;

import java.util.Comparator;

public class Currency {
	
	public static final Currency USDT = new Currency("USDT");
	public static final Currency BTC = new Currency("BTC");

	public static final Currency WALLET = new Currency("v-WALLET");

	public final String symbol;

	public Currency(String symbol) {
		this.symbol = symbol;
	}
	
	@Override
	public String toString() {
		return symbol;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Currency)) {
			return false;
		}
		Currency other = (Currency) obj;
		return this.symbol.equals(other.symbol);
	}
	
	@Override
	public int hashCode() {
		return symbol.hashCode();
	}
	
	public static Comparator<Currency> bySymbol() {
		return Comparator.comparing((Currency c) -> { return c.symbol; });
	}

}
