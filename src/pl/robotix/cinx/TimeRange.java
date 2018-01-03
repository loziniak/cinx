package pl.robotix.cinx;

public enum TimeRange {
	
	DAY(24 * 60 * 60, 5 * 60),
	WEEK(7 * DAY.periodSeconds, 30 * 60),
	MONTH(30 * DAY.periodSeconds, 4 * 60 * 60),
	YEAR(365 * DAY.periodSeconds, 24 * 60 * 60);
	
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
