package pl.robotix.cinx.wallet;

import java.util.function.BiConsumer;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import pl.robotix.cinx.Currency;

public class WalletEntry {
	
	private final Currency currency;
	private final double originalPercent;

	final DoubleProperty percent = new SimpleDoubleProperty();
	final DoubleBinding percentChange;
	final BooleanProperty enabled = new SimpleBooleanProperty(true);

	public WalletEntry(Currency c, double walletUSD, double originalPrice) {
		this.currency = c;
		this.originalPercent = 100.0 * originalPrice / walletUSD;
		percent.set(originalPercent);

		percentChange = percent.subtract(originalPercent);

		percent.addListener(
				(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
			if (newValue.doubleValue() == 100.0) {
				enabled.set(false);
			}
		});

	}
	
	public void enable() {
		this.enabled.set(true);
	}
	
	public double getPercent() {
		return percent.getValue();
	}
	
	public double getOriginalPercent() {
		return originalPercent;
	}
	
	public void setPercent(double percent) {
		if (percent > 99.0) percent = 100.0;
		if (percent < 1.0) percent = 0.0;
		this.percent.setValue(percent);
	}
	
	public Currency getCurrency() {
		return currency;
	}
	
	public double getPercentChange() {
		return percentChange.doubleValue();
	}
	
	public void setPercentChangeHandler(BiConsumer<Double, WalletEntry> onPercentChange) {
		percent.addListener(
				(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
			onPercentChange.accept(newValue.doubleValue() - oldValue.doubleValue(), this);
		});
	}
	
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof WalletEntry && ((WalletEntry) obj).currency.equals(this.currency);
	}
	
	@Override
	public int hashCode() {
		return currency.hashCode();
	}

}
