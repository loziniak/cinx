package pl.robotix.cinx.wallet;

import java.math.BigDecimal;
import java.util.function.BiConsumer;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import pl.robotix.cinx.Currency;

public class WalletEntry {
	
	private final Currency currency;
	private final double originalPercent;
	private final BigDecimal originalAmount;
	private final Wallet wallet;

	final DoubleProperty percent = new SimpleDoubleProperty();
	final DoubleBinding percentChange;
	final BooleanProperty enabled = new SimpleBooleanProperty(true);
	final BooleanProperty freeze = new SimpleBooleanProperty(false);
	final BooleanProperty isChanging = new SimpleBooleanProperty(false);

	public WalletEntry(Currency c, double originalPercent, BigDecimal originalAmount, Wallet wallet) {
		this.currency = c;
		this.originalPercent = originalPercent;
		percent.set(originalPercent);
		this.originalAmount = originalAmount;
		this.wallet = wallet;

		percentChange = percent.subtract(originalPercent);

		percent.addListener((observable, oldValue, newValue) -> {
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
	
	public BigDecimal getOriginalAmount() {
		return originalAmount;
	}
	
	public double getOriginalPercent() {
		return originalPercent;
	}
	
	public void setPercent(double percent) {
		if (percent > 99.0) percent = 100.0;
		if (percent < 0.5) percent = 0.0;
		this.percent.setValue(percent);
	}
	
	public Currency getCurrency() {
		return currency;
	}
	
	public double getPercentChange() {
		return percentChange.doubleValue();
	}
	
	public boolean canRemove() {
		return wallet.canRemove(currency);
	}
	
	public void setPercentChangeHandler(BiConsumer<double[], WalletEntry> onPercentChange) {
		final WalletEntry self = this;
		isChanging.addListener(new ChangeListener<Boolean>() {
			
			private Double oldPercent;
			
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue) {
					this.oldPercent = self.percent.get();
				} else {
					onPercentChange.accept( new double[] { self.percent.get(), oldPercent }, self);
				}
			}
		});
	}
	
	public void bindIsChanging(BooleanProperty isChanging) {
		this.isChanging.bind(isChanging);
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
