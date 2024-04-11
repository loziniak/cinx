package pl.robotix.cinx;

public enum TimeRange {
	
	DAY(            24 * 60 * 60),
	WEEK(       7 * 24 * 60 * 60),
	TWO_MONTHS(60 * 24 * 60 * 60),
	YEAR(     356 * 24 * 60 * 60);
	
	public long seconds;

	private TimeRange(long seconds) {
		this.seconds = seconds;
	}

}
