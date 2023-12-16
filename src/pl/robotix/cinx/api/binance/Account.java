package pl.robotix.cinx.api.binance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONObject;

public class Account {
	
	private List<Balance> balances;

	public Account(String json) {
		var b = new JSONObject(json).getJSONArray(Keys.balances.name());
		balances = new ArrayList<>(20);
		for (Object o : b) {
			if (o instanceof JSONObject) {
				balances.add(new Balance((JSONObject) o));
			}
		}
	}
	
	public List<Balance> getBalances() {
		return Collections.unmodifiableList(balances);
	}
	
	public static class Balance {
		private String asset;
		private BigDecimal free;
		
		public Balance(JSONObject o) {
			this.asset = o.getString(Keys.asset.name());
			this.free = o.getBigDecimal(Keys.free.name());
		}
		
		public String getAsset() {
			return asset;
		}
		
		public BigDecimal getFree() {
			return free;
		}

		private static enum Keys {
			asset,
			free
		}
		
	}

	private static enum Keys {
		balances
	}
	
}
