package pl.robotix.cinx.api.binance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONObject;

import pl.robotix.cinx.Pair;

public class ExchangeInfo {

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
	
	public static class Symbol {
		
//		private String symbol;
		private String baseAsset;
		private String quoteAsset;
		
		public Symbol(JSONObject o) {
//			symbol = o.getString(Keys.symbol.name());
			baseAsset = o.getString(Keys.baseAsset.name());
			quoteAsset = o.getString(Keys.quoteAsset.name());
		}
		
		public Pair toPair() {
			return new Pair(quoteAsset, baseAsset);
		}
		
		private static enum Keys {
//			symbol,
			baseAsset,
			quoteAsset
		}
		
	}

	private static enum Keys {
		symbols
	}
	
}
