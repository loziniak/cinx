package pl.robotix.cinx;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TimeRange {
	
	public static TimeRange DAY;
	public static TimeRange WEEK;
	public static TimeRange MONTH;
	public static TimeRange YEAR;
	
	public static void init(long dayDensity, long weekDensity, long monthDensity, long yearDensity) {
		DAY = new TimeRange(24 * 60 * 60, dayDensity);
		WEEK = new TimeRange(7 * DAY.periodSeconds, weekDensity);
		MONTH = new TimeRange(30 * DAY.periodSeconds, monthDensity);
		YEAR = new TimeRange(365 * DAY.periodSeconds, yearDensity);
	}
	
	public static List<TimeRange> values() {
		return Collections.unmodifiableList(Arrays.asList(DAY, WEEK, MONTH, YEAR));
	}

	
	public final long periodSeconds;
	
	public final long densitySeconds;
	
	
	private TimeRange(long periodSeconds, long densitySeconds) {
		this.periodSeconds = periodSeconds;
		this.densitySeconds = densitySeconds;
	}
	
	public long getStart() {
		return System.currentTimeMillis() / 1000 - periodSeconds;
	}
	
	public long getPointsCount() {
		return periodSeconds / densitySeconds;
	}

}
