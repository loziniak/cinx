package pl.robotix.cinx.wallet;

import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import javafx.beans.property.ObjectProperty;
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
	
	private ObjectProperty<Map<Currency, Double>> walletCurrencies;
	
	final ObservableMap<Currency, WalletEntry> sliders;
	

	public Wallet(Map<Currency, BigDecimal> balance, ObservableSet<Currency> chartCurrencies, Prices prices, ObjectProperty<Map<Currency, Double>> walletCurrencies) {
		this.prices = prices;
		this.walletCurrencies = walletCurrencies;

		sliders = FXCollections.observableMap(new HashMap<>());
		
		walletUSD = setBalance(balance);

		chartCurrencies.addListener((Change<? extends Currency> change) -> {
			if (change.wasAdded()) {
				add(change.getElementAdded());
			}
			if (change.wasRemoved()) {
				remove(change.getElementRemoved());
			}
		});
	}
	
	private double setBalance(Map<Currency, BigDecimal> balance) {
		System.out.println("setBalance...");
		prices.retrieveFor(balance.keySet());
		
		double[] walletUSDHolder = { 0.0 };
		balance.forEach((currency, amount) -> {
			System.out.println("setBalance: " + currency.symbol + " " + amount.toString());
			walletUSDHolder[0] += amount.doubleValue() * prices.getUSDFor(currency).doubleValue();
		});

		balance.forEach((currency, amount) -> {
			add(currency, amount, walletUSDHolder[0]);
		});
		
		return walletUSDHolder[0];
	}

	private void add(Currency c, BigDecimal originalAmount, double walletUSD) {
		double originalPercent = 100 * originalAmount.doubleValue() * prices.getUSDFor(c).doubleValue() / walletUSD;
		WalletEntry slider = new WalletEntry(c, originalPercent, originalAmount, this);
		
		if (sliders.putIfAbsent(c, slider) == null) {
			slider.setPercentChangeHandler(onPercentChange);
		}
	}
	
	public void add(Currency c) {
		this.add(c, BigDecimal.ZERO, walletUSD);
	}
	
	public boolean canRemove(Currency c) {
		WalletEntry slider = sliders.get(c);
		return slider != null && slider.getPercent() == 0.0;
	}
	
	public void remove(Currency c) {
		WalletEntry removed = sliders.remove(c);
		if (removed != null) {
			changePercentsProportionally(removed, 0.0, removed.getPercent(), true);
			updateWalletCurrencies();
		}
	}
	
	private void changePercentsProportionally(WalletEntry movingSlider,
			double newPercent, double oldPercent, boolean removed) {
		onPercentChange.disable();

		Set<WalletEntry> slidersToMove = new HashSet<>();
		double[] movedPercentHolder = {0.0};
		double[] freezedPercentHolder = {0.0};
		sliders.forEach((currency, slider) -> {
			if (!slider.equals(movingSlider)) {
				if (slider.freeze.get()) {
					freezedPercentHolder[0] += slider.getPercent();
				} else {
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
				double freezed = freezedPercentHolder[0];
				double moved = movedPercentHolder[0];
				double x = (100.0 - newPercent - freezed) / (moved == 0.0 ? slidersToMove.size() : moved);
				double y = moved == 0.0 ? 1.0 : 0.0;
				slidersToMove.forEach((otherSlider) -> {
					otherSlider.setPercent((otherSlider.getPercent() + y) * x);
					if (otherSlider.getPercent() != 100.0) {
						otherSlider.enable();
					}
				});
				
			}
		}

		onPercentChange.enable();
	}
	
	public void updateWalletCurrencies() {
		var newCurrencies = new HashMap<Currency, Double>();
		sliders.forEach((currency, entry) -> {
			if (entry.getPercent() > 0.0) {
				newCurrencies.put(currency, entry.getPercent());
			}
		});
		walletCurrencies.set(newCurrencies);
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
				updateWalletCurrencies();
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
