package pl.robotix.cinx;

import java.time.LocalDateTime;

public class Point {
	
	public LocalDateTime date;
	
	public double value;

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
