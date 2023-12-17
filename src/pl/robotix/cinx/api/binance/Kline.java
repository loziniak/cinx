package pl.robotix.cinx.api.binance;

import static java.time.Instant.ofEpochMilli;
import static java.time.ZoneOffset.UTC;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;

public class Kline {
	
	private ZonedDateTime closeTime;
	private BigDecimal closePrice;

	public Kline(Object a) {
		var arr = (ArrayList<Object>) a;
		closeTime = ZonedDateTime.ofInstant(
				ofEpochMilli((Long) arr.get(Indexes.CLOSE_TIME.index)),
				UTC);
		closePrice = new BigDecimal((String) arr.get(Indexes.CLOSE_PRICE.index));
	}
	
	public ZonedDateTime getCloseTime() {
		return closeTime;
	}
	
	public BigDecimal getClosePrice() {
		return closePrice;
	}

	private static enum Indexes {
		CLOSE_TIME(6),
		CLOSE_PRICE(4);
		
		private int index;
		
		private Indexes(int index) {
			this.index = index;
		}
	}
	
}
