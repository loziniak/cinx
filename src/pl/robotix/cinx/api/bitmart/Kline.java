package pl.robotix.cinx.api.bitmart;

import static java.time.Instant.ofEpochMilli;
import static java.time.ZoneOffset.UTC;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;

public class Kline {
	
	private ZonedDateTime closeTime;
	private BigDecimal closePrice;
	private BigDecimal lowPrice;
	private BigDecimal highPrice;
	private BigDecimal volume;

	public Kline(Object a) {
		@SuppressWarnings("unchecked")
		var arr = (ArrayList<Object>) a;

		closeTime = ZonedDateTime.ofInstant(
				ofEpochMilli(Long.parseLong((String) arr.get(Indexes.CLOSE_TIME.index))),
				UTC);
		closePrice = new BigDecimal((String) arr.get(Indexes.CLOSE_PRICE.index));
		lowPrice = new BigDecimal((String) arr.get(Indexes.LOW_PRICE.index));
		highPrice = new BigDecimal((String) arr.get(Indexes.HIGH_PRICE.index));
		volume = new BigDecimal((String) arr.get(Indexes.VOLUME.index));
	}
	
	public ZonedDateTime getCloseTime() {
		return closeTime;
	}
	
	public BigDecimal getClosePrice() {
		return closePrice;
	}
	
	public BigDecimal getLowPrice() {
		return lowPrice;
	}
	
	public BigDecimal getHighPrice() {
		return highPrice;
	}
	
	public BigDecimal getVolume() {
		return volume;
	}

	private static enum Indexes {
		CLOSE_TIME(0),
		HIGH_PRICE(2),
		LOW_PRICE(3),
		CLOSE_PRICE(4),
		VOLUME(5);
		
		private int index;
		
		private Indexes(int index) {
			this.index = index;
		}
	}
	
}
