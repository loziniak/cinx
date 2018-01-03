package pl.robotix.cinx;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class WalletCurrencies {
	
	private final PercentChangeConsumer onPercentChange = new PercentChangeConsumer();
			
	private final double walletUSD;
	
	private ObservableMap<Currency, WalletSlider> sliders;
	
	private boolean paneIsSet = false;
	

	public WalletCurrencies(Map<Currency, BigDecimal> balance) {
		sliders = FXCollections.observableMap(new HashMap<>());

		DoubleProperty walletUSD = new SimpleDoubleProperty(0.0);
		balance.forEach((currency, usd) -> {
			walletUSD.set(walletUSD.get() + usd.doubleValue());
		});
		this.walletUSD = walletUSD.get();

		balance.forEach((currency, usd) -> {
			add(currency, usd.doubleValue());
		});
	}

	private void add(Currency c, double usd) {
		WalletSlider slider = new WalletSlider(c, walletUSD);
		if (sliders.putIfAbsent(c, slider) == null) {
			slider.setPercent(100.0 * usd / this.walletUSD);
			slider.setPercentChangeHandler(onPercentChange);
		}
	}
	
	public void add(Currency c) {
		this.add(c, 0.0);
	}
	
	public void remove(Currency c) {
		WalletSlider removed = sliders.remove(c);
		changePercentsProportionally(removed, -removed.getPercent(), true);
	}
	
	private void changePercentsProportionally(WalletSlider slider, double percentChange, boolean removed) {
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
	
	
	public void setSlidersPane(Pane slidersPane) {
		if (paneIsSet) {
			throw new IllegalStateException("Pane is already set.");
		}
		paneIsSet = true;
		final ObservableList<Node> children = slidersPane.getChildren();

		sliders.forEach((currency, slider) -> {
			children.add(slider.getNode());
		});
		
		sliders.addListener((Change<? extends Currency,? extends WalletSlider> change) -> {
			if (change.wasAdded()) {
				children.add(change.getValueAdded().getNode());
			}
			if (change.wasRemoved()) {
				children.remove(change.getValueRemoved().getNode());
			}
			
		});
	}
	
	public Set<Currency> getCurrencies() {
		return sliders.keySet();
	}
	
	
	private final class PercentChangeConsumer implements BiConsumer<Double, WalletSlider> {
		
		private boolean bypass = false;

		@Override
		public void accept(Double percentChange, WalletSlider slider) {
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
