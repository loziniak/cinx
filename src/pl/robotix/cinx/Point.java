package pl.robotix.cinx;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class Point {
	
	public LocalDateTime date;
	
	public double price;
	public double volume;

	/**
	 * Convert to system default timezone
	 * @param date
	 * @param value
	 */
	public Point(ZonedDateTime date, double price, double volume) {
		this(date.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(), price, volume);
	}
	
	public Point(LocalDateTime date, double price, double volume) {
		super();
		this.date = date;
		this.price = price;
		this.volume = volume;
	}
	
	@Override
	public String toString() {
		return String.valueOf(price);
	}
	
}
