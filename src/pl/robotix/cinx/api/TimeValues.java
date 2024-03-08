package pl.robotix.cinx.api;

public class TimeValues {

//	public static void init(long dayDensity, long weekDensity, long monthDensity, long yearDensity) {
//		DAY = new TimeRange("DAY", 24 * 60 * 60, dayDensity);
//		WEEK = new TimeRange("WEEK", 7 * DAY.periodSeconds, weekDensity);
//		MONTH = new TimeRange("MONTH", 30 * DAY.periodSeconds, monthDensity);
//		YEAR = new TimeRange("YEAR", 365 * DAY.periodSeconds, yearDensity);
//	}
	
	public final long periodSeconds;
	
	public final long densitySeconds;
	

	public TimeValues(long periodSeconds, long densitySeconds) {
		this.periodSeconds = periodSeconds;
		this.densitySeconds = densitySeconds;
	}
	
	public long getStart() {
		return System.currentTimeMillis() / 1000 - periodSeconds;
	}
	
	public long getPointsCount() {
		return periodSeconds / densitySeconds;
	}
	
	@Override
		public String toString() {
			return "T_" + periodSeconds + "/" + densitySeconds;
		}
	
}
