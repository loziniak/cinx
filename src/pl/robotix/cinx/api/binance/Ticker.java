package pl.robotix.cinx.api.binance;

import java.math.BigDecimal;

import org.json.JSONObject;

public class Ticker {

	private String symbol;
	private BigDecimal quoteVolume;
	private BigDecimal lastPrice;
	private long count;

	public Ticker(JSONObject o) {
		symbol = o.getString(Keys.symbol.name());
		quoteVolume = new BigDecimal(o.getString(Keys.quoteVolume.name()));
		lastPrice = new BigDecimal(o.getString(Keys.lastPrice.name()));
		count = o.getLong(Keys.count.name());
	}
	
	public String getSymbol() {
		return symbol;
	}

	public BigDecimal getQuoteVolume() {
		return quoteVolume;
	}

	public BigDecimal getLastPrice() {
		return lastPrice;
	}
	
	public long getCount() {
		return count;
	}


	private static enum Keys {
		symbol,
		quoteVolume,
		lastPrice,
		count
	}
	
}
