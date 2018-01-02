package pl.robotix.cinx;

public class Currency {
	
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

}
