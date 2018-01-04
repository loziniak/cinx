package pl.robotix.cinx.wallet;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import pl.robotix.cinx.Currency;

public class Wallet {
	
	private final PercentChangeConsumer onPercentChange = new PercentChangeConsumer();
			
	private final double walletUSD;
	
	final ObservableMap<Currency, WalletEntry> sliders;
	

	public Wallet(Map<Currency, BigDecimal> balance) {
		sliders = FXCollections.observableMap(new HashMap<>());

		double[] walletUSDHolder = { 0.0 };
		balance.forEach((currency, usd) -> {
			walletUSDHolder[0] += usd.doubleValue();
		});
		this.walletUSD = walletUSDHolder[0];

		balance.forEach((currency, usd) -> {
			add(currency, usd.doubleValue());
		});
	}

	private void add(Currency c, double usd) {
		WalletEntry slider = new WalletEntry(c, walletUSD, usd);
		if (sliders.putIfAbsent(c, slider) == null) {
			slider.setPercentChangeHandler(onPercentChange);
		}
	}
	
	public void add(Currency c) {
		this.add(c, 0.0);
	}
	
	public boolean canRemove(Currency c) {
		WalletEntry slider = sliders.get(c);
		return slider != null && slider.getPercent() == 0.0;
	}
	
	public void remove(Currency c) {
		WalletEntry removed = sliders.remove(c);
		changePercentsProportionally(removed, -removed.getPercent(), true);
	}
	
	private void changePercentsProportionally(WalletEntry slider, double percentChange, boolean removed) {
		onPercentChange.disable();
		if (percentChange == -100.0) {
			double percent = 100.0 / sliders.size() - 1;
			sliders.forEach((currency, otherSlider) -> {
				if (!currency.equals(slider.getCurrency())) {
					otherSlider.setPercent(percent);
				}
			});
			return;
		}
		
		double previousPercent;
		if (removed) {
			previousPercent = -percentChange;
		} else {
			previousPercent = slider.getPercent() - percentChange;
		}
		double x = (100.0 - previousPercent - percentChange) / (100.0 - previousPercent);
		
		sliders.forEach((currency, otherSlider) -> {
			if (!currency.equals(slider.getCurrency())) {
				otherSlider.setPercent(otherSlider.getPercent() * x);
				if (otherSlider.getPercent() != 100.0) {
					otherSlider.enable();
				}
			}
		});
		onPercentChange.enable();
	}
	
	
	public Set<Currency> getCurrencies() {
		return sliders.keySet();
	}
	
	public Map<Currency, Double> getPercentChanges() {
		Map<Currency, Double> percentChanges = new HashMap<>();
		sliders.forEach((c, s) -> {
			percentChanges.put(c, s.getPercentChange());
		});
		return percentChanges;
	}
	
	@SuppressWarnings("unused") // used in tests with reflection
	private void setPercentChanges(double[] changes) {
		int[] changeNoHolder = {0};
		onPercentChange.disable();
		sliders.forEach((c, s) -> {
			s.setPercent(s.getPercent() + changes[changeNoHolder[0]]);
			changeNoHolder[0]++;
		});
		onPercentChange.enable();
	}
	
	
	private final class PercentChangeConsumer implements BiConsumer<Double, WalletEntry> {
		
		private boolean bypass = false;

		@Override
		public void accept(Double percentChange, WalletEntry slider) {
			if (!bypass) {
				changePercentsProportionally(slider, percentChange, false);
			}
		}
		
		public void disable() {
			this.bypass = true;
		}
		
		public void enable() {
			this.bypass = false;
		}
	}

}
