package pl.robotix.cinx;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener.Change;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import pl.robotix.cinx.wallet.Wallet;

public class CurrencySelector extends FlowPane {

	public CurrencySelector(Wallet wallet, ObservableSet<Currency> chartCurrencies) {
		super();
		setVgap(5);
		setHgap(5);
		
		List<Currency> currencyList = new ArrayList<>(App.prices.getAllCurrencies());
		currencyList.sort(App.prices.byVolume());
		for (Currency c: currencyList) {
			getChildren().add(currencyButton(c, wallet, chartCurrencies));
		}
	}
	
	private ToggleButton currencyButton(final Currency currency, Wallet wallet, ObservableSet<Currency> chartCurrencies) {
		final ToggleButton button = new ToggleButton(currency.symbol);
		
		button.selectedProperty().addListener(new ChangeListener<Boolean>() {
			private boolean bypass = false;

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (bypass) {
					return;
				}

				boolean selected = newValue.booleanValue(); 
				if (selected) {
					chartCurrencies.add(currency);
				} else {
					if (wallet.canRemove(currency)) {
						chartCurrencies.remove(currency);
					} else {
						bypass = true;
						button.selectedProperty().set(true);
						bypass = false;
					}
				}
			}
		});
		
		chartCurrencies.addListener((Change<? extends Currency> change) -> {
			Currency added = change.getElementAdded();
			Currency removed = change.getElementRemoved();

			if (change.wasAdded() && added.equals(currency)) {
				if (!button.isSelected()) {
					button.setSelected(true);
				}
			}
			if (change.wasRemoved() && removed.equals(currency)) {
				if (button.isSelected()) {
					button.setSelected(false);
				}
			}
		});
		return button;
	}

}
