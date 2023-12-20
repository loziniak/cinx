package pl.robotix.cinx.api.binance;

import java.math.BigDecimal;

import org.json.JSONObject;

public class Ticker {

	private String symbol;
	private BigDecimal volume;
	private BigDecimal lastPrice;
	private long count;

	public Ticker(JSONObject o) {
		symbol = o.getString(Keys.symbol.name());
		volume = new BigDecimal(o.getString(Keys.volume.name()));
		lastPrice = new BigDecimal(o.getString(Keys.lastPrice.name()));
		count = o.getLong(Keys.count.name());
	}
	
	public String getSymbol() {
		return symbol;
	}

	public BigDecimal getVolume() {
		return volume;
	}

	public BigDecimal getLastPrice() {
		return lastPrice;
	}
	
	public long getCount() {
		return count;
	}


	private static enum Keys {
		symbol,
		volume,
		lastPrice,
		count
	}
	
}
