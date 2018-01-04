package pl.robotix.cinx.trade;

import static pl.robotix.cinx.trade.State.NEW;

import java.math.BigDecimal;

import pl.robotix.cinx.Pair;

public class Operation {
	
	public Pair pair;
	
	public BigDecimal rate;
	
	public BigDecimal amount;

	public State state = NEW;
	
	public Type type;
	
	public Operation(Type type) {
		this.type = type;
	}
	
	
	@Override
	public String toString() {
		return pair + ": "+type+" "+String.format("%.8f", amount)+" at rate "+String.format("%.8f", rate);
	}
	
	public static enum Type {
		BUY, SELL
	}
	
}
