package pl.robotix.cinx.api.binance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import pl.robotix.cinx.Pair;

public class ExchangeInfo {

	private static final String MARKET_TYPE = "MARKET";
	private static final String SPOT_PERMISSION = "SPOT";
	
	private List<Symbol> symbols;

	public ExchangeInfo(String json) {
		var s = new JSONObject(json).getJSONArray(Keys.symbols.name());
		var sym = new ArrayList<Symbol>(20);
		for (Object o : s) {
			if (o instanceof JSONObject) {
				sym.add(new Symbol((JSONObject) o));
			}
		}
		symbols = Collections.unmodifiableList(sym);
	}
	
	public List<Symbol> getSymbols() {
		return symbols;
	}
	
	public Symbol getSymbol(Pair p) {
		var i = symbols.iterator();
		while (i.hasNext()) {
			var s = i.next();
			if (s.baseAsset.equals(p.base.symbol) && s.quoteAsset.equals(p.quote.symbol)) {
				return s;
			}
		}
		throw new IllegalStateException("Symbol for pair "+p+" not found.");
	}
	
	public static class Symbol {
		
		private String baseAsset;
		private String quoteAsset;
		private boolean isMarket;
		private boolean isSpot;
		private BigDecimal stepSize;
		
		public Symbol(JSONObject o) {
			baseAsset = o.getString(Keys.baseAsset.name());
			quoteAsset = o.getString(Keys.quoteAsset.name());
			
			isMarket = contains(o.getJSONArray(Keys.orderTypes.name()), MARKET_TYPE);
			isSpot = contains(o.getJSONArray(Keys.permissions.name()), SPOT_PERMISSION);
			var filters = o.getJSONArray(Keys.filters.name());
			var i = filters.iterator();
			while (i.hasNext()) {
				var filter = (JSONObject) i.next();
				String name = filter.getString(Keys.filterType.name());
				if (name.equals("LOT_SIZE")) {
					stepSize = new BigDecimal(filter.getString(Keys.stepSize.name()));
				}
			}
		}
		
		public Pair toPair() {
			return new Pair(quoteAsset, baseAsset);
		}
		
		public boolean isMarket() {
			return isMarket;
		}
		
		public boolean isSpot() {
			return isSpot;
		}
		
		public BigDecimal getStepSize() {
			return stepSize;
		}
		
		private static enum Keys {
			baseAsset,
			quoteAsset,
			orderTypes,
			permissions,
			filters,
				filterType,
				stepSize
		}
		
		private boolean contains(JSONArray array, String value) {
			var i = array.iterator();
			while (i.hasNext()) {
				if (value.equals((String) i.next())) {
					return true;
				}
			}
			return false;
		}
	}

	private static enum Keys {
		symbols
	}
	
}
