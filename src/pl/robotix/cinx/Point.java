package pl.robotix.cinx;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class Point {
	
	public LocalDateTime date;
	
	public double value;

	/**
	 * Convert to system default timezone
	 * @param date
	 * @param value
	 */
	public Point(ZonedDateTime date, double value) {
		this(date.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(), value);
	}
	
	public Point(LocalDateTime date, double value) {
		super();
		this.date = date;
		this.value = value;
	}
	
	@Override
	public String toString() {
		return String.valueOf(value);
	}
	
}
