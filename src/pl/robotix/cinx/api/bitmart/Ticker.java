package pl.robotix.cinx.api.bitmart;

import java.math.BigDecimal;

import org.json.JSONObject;

public class Ticker {

	private String symbol;
	private BigDecimal quoteVolume;
	private BigDecimal lastPrice;

	public Ticker(JSONObject o) {
		symbol = o.getString(Keys.symbol.name());
		quoteVolume = new BigDecimal(o.getString(Keys.qv_24h.name()));
		lastPrice = new BigDecimal(o.getString(Keys.last.name()));
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


	private static enum Keys {
		symbol,
		qv_24h,
		last
	}
	
}
