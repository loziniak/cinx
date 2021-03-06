package pl.robotix.cinx.wallet;

import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener.Change;
import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Prices;

public class Wallet {
	
	private final PercentChangeConsumer onPercentChange = new PercentChangeConsumer();
			
	private final double walletUSD;
	
	private final Prices prices;
	
	final ObservableMap<Currency, WalletEntry> sliders;
	

	public Wallet(Map<Currency, BigDecimal> balance, ObservableSet<Currency> chartCurrencies, Prices prices) {
		this.prices = prices;
		
		sliders = FXCollections.observableMap(new HashMap<>());

		double[] walletUSDHolder = { 0.0 };
		balance.forEach((currency, amount) -> {
			walletUSDHolder[0] += amount.doubleValue() * prices.getUSDFor(currency).doubleValue();
		});
		this.walletUSD = walletUSDHolder[0];

		balance.forEach((currency, amount) -> {
			add(currency, amount);
		});
		
		chartCurrencies.addListener((Change<? extends Currency> change) -> {
			if (change.wasAdded()) {
				add(change.getElementAdded());
			}
			if (change.wasRemoved()) {
				remove(change.getElementRemoved());
			}
		});		
	}

	private void add(Currency c, BigDecimal originalAmount) {
		WalletEntry slider = new WalletEntry(c,
				100 * originalAmount.doubleValue() * prices.getUSDFor(c).doubleValue() / walletUSD,
				originalAmount);
		
		if (sliders.putIfAbsent(c, slider) == null) {
			slider.setPercentChangeHandler(onPercentChange);
		}
	}
	
	public void add(Currency c) {
		this.add(c, BigDecimal.ZERO);
	}
	
	public boolean canRemove(Currency c) {
		WalletEntry slider = sliders.get(c);
		return slider != null && slider.getPercent() == 0.0;
	}
	
	public void remove(Currency c) {
		WalletEntry removed = sliders.remove(c);
		changePercentsProportionally(removed, 0.0, removed.getPercent(), true);
	}
	
	private void changePercentsProportionally(WalletEntry movingSlider,
			double newPercent, double oldPercent, boolean removed) {
		onPercentChange.disable();

		Set<WalletEntry> slidersToMove = new HashSet<>();
		double[] movedPercentHolder = {0.0};
		double[] freezedPercentHolder = {0.0};
		sliders.forEach((currency, slider) -> {
			if (slider.freeze.get()) {
				freezedPercentHolder[0] += slider.getPercent();
			} else {
				if (!slider.equals(movingSlider)) {
					slidersToMove.add(slider);
					movedPercentHolder[0] += slider.getPercent();
				}
			}
		});

		
		if (slidersToMove.size() == 0) {
			movingSlider.setPercent(oldPercent);

		} else {
			if (oldPercent == 100.0) {
				double percent = 100.0 / slidersToMove.size();
				slidersToMove.forEach((otherSlider) -> {
					otherSlider.setPercent(percent);
				});

			} else {
				double x = (100.0 - newPercent - freezedPercentHolder[0]) / movedPercentHolder[0];
				slidersToMove.forEach((otherSlider) -> {
					otherSlider.setPercent(otherSlider.getPercent() * x);
					if (otherSlider.getPercent() != 100.0) {
						otherSlider.enable();
					}
				});
				
			}
		}

		onPercentChange.enable();
	}
	
	public Set<Currency> getCurrencies() {
		return sliders.keySet();
	}
	
	public Map<Currency, Double> getPercentChanges() {
		Map<Currency, Double> percentChanges = new HashMap<>();
		sliders.forEach((currency, slider) -> {
			percentChanges.put(currency, slider.getPercentChange());
		});
		return percentChanges;
	}
	
	public BigDecimal getOriginalAmount(Currency c) {
		WalletEntry slider = sliders.get(c);
		return slider != null ? slider.getOriginalAmount() : ZERO;
	}
	
	public double getOriginalPercent(Currency c) {
		WalletEntry slider = sliders.get(c);
		return slider != null ? slider.getOriginalPercent() : 0.0;
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
	
	public double getWalletUSD() {
		return walletUSD;
	}
	
	private final class PercentChangeConsumer implements BiConsumer<double[], WalletEntry> {
		
		private boolean bypass = false;

		@Override
		public void accept(double[] values, WalletEntry slider) {
			if (!bypass) {
				changePercentsProportionally(slider, values[0], values[1], false);
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
