package pl.robotix.cinx.graph;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TimeAxis extends MyAxis<LocalDateTime> {

	public TimeAxis(LocalDateTime low, LocalDateTime up) {
		super(low, up);
	}

	@Override
	public double toNumericValue(LocalDateTime value) {
		return value.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
	}

	@Override
	public LocalDateTime toRealValue(double value) {
		return Instant.ofEpochSecond(Double.valueOf(value).longValue())
				.atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

	@Override
	protected String getTickMarkLabel(LocalDateTime value) {
		return value.toString();
	}

}
