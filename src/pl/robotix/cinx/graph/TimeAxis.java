package pl.robotix.cinx.graph;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import pl.robotix.cinx.TimeRange;

public class TimeAxis extends MyAxis<LocalDateTime> {
	
	private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");

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
		TimeRange timeRange = getTimeRange();
		if (timeRange == TimeRange.DAY) {
			return value.format(HOUR_FORMATTER);
		} else {
			return value.format(DAY_FORMATTER);
		}
	}
	
	@Override
	protected List<LocalDateTime> calculateTickValues(double length, Object range) {
		TimeRange timeRange = getTimeRange();
		List<LocalDateTime> tickValues = new ArrayList<>();
		tickValues.add(getLower());
		if (timeRange == TimeRange.DAY) {
			LocalDateTime tick = getLower().plusHours(4).withMinute(0);
			for (; tick.isBefore(getUpper()); tick = tick.plusHours(4)) {
				tickValues.add(tick);
			}
		} else if (timeRange == TimeRange.WEEK) {
			LocalDateTime tick = getLower().plusDays(1).withHour(0);
			for (; tick.isBefore(getUpper()); tick = tick.plusDays(1)) {
				tickValues.add(tick);
			}
		} else if (timeRange == TimeRange.MONTH) {
			LocalDateTime tick = getLower().plusDays(5).withHour(0);
			for (; tick.isBefore(getUpper()); tick = tick.plusDays(5)) {
				tickValues.add(tick);
			}
		} else if (timeRange == TimeRange.YEAR) {
			LocalDateTime tick = getLower().plusMonths(2).withHour(0);
			for (; tick.isBefore(getUpper()); tick = tick.plusMonths(2)) {
				tickValues.add(tick);
			}
		}
		tickValues.add(getUpper());
		return tickValues;
	}
	
	private TimeRange getTimeRange() {
		long secondsDifference = getLower().until(getUpper(), ChronoUnit.SECONDS);

		if (secondsDifference > TimeRange.DAY.periodSeconds / 2 && secondsDifference <= TimeRange.WEEK.periodSeconds / 2) {
			return TimeRange.DAY;
		} else if (secondsDifference > TimeRange.WEEK.periodSeconds / 2 && secondsDifference <= TimeRange.MONTH.periodSeconds / 2) {
			return TimeRange.WEEK;
		} else if (secondsDifference > TimeRange.MONTH.periodSeconds / 2 && secondsDifference <= TimeRange.YEAR.periodSeconds / 2) {
			return TimeRange.MONTH;
		} else if (secondsDifference > TimeRange.YEAR.periodSeconds / 2) {
			return TimeRange.YEAR;
		}
		
		return null;
	}

}
