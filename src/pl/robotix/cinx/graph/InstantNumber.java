package pl.robotix.cinx.graph;

import java.time.Instant;

public class InstantNumber extends Number {
	private static final long serialVersionUID = 1L;

	private Instant instant;
	
	public InstantNumber(Instant instant) {
		super();
		this.instant = instant;
	}


	@Override
	public long longValue() {
		return instant.getEpochSecond();
	}

	@Override
	public int intValue() { return (int) longValue(); }
	@Override
	public float floatValue() { return longValue(); }
	@Override
	public double doubleValue() { return longValue(); }

}
