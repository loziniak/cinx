package pl.robotix.cinx;

import static javafx.scene.layout.BorderStroke.MEDIUM;
import static javafx.scene.layout.BorderStrokeStyle.NONE;
import static javafx.scene.layout.BorderStrokeStyle.SOLID;
import static javafx.scene.paint.Color.RED;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener.Change;
import javafx.geometry.Insets;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import pl.robotix.cinx.wallet.Wallet;

public class CurrencySelector extends FlowPane {
	
	private static final BorderWidths BORDER_WIDTH = MEDIUM;
	
	private static final Border HIGHLIGHT_BORDER = new Border(new BorderStroke(RED, SOLID, new CornerRadii(0), BORDER_WIDTH));

	private static final Border DEFAULT_BORDER = new Border(new BorderStroke(null, NONE, new CornerRadii(0), BORDER_WIDTH));
	
	private static final Map<String, Currency> CURRENCIES = new HashMap<>();
	private static final List<ToggleButton> BUTTONS = new ArrayList<>();
	private static final Comparator<ToggleButton> BY_VOLUME = (b1, b2) -> 
		App.prices.byVolume().compare(
			CURRENCIES.get(b1.getText()),
			CURRENCIES.get(b2.getText())
		);
	private static final Comparator<ToggleButton> BY_SYMBOL = (b1, b2) -> 
		CURRENCIES.get(b1.getText()).symbol.compareTo(
		CURRENCIES.get(b2.getText()).symbol);

		
	public CurrencySelector(Wallet wallet, ObservableSet<Currency> chartCurrencies, ObjectProperty<Currency> highlightCurrency) {
		super();
		setVgap(5);
		setHgap(5);
		
		for (Currency c: App.prices.getAllCurrencies()) {
			CURRENCIES.put(c.symbol, c);
			BUTTONS.add(currencyButton(c, wallet, chartCurrencies, highlightCurrency));
		}
		BUTTONS.sort(BY_VOLUME);
		getChildren().addAll(BUTTONS);

		chartCurrencies.addListener((Change<? extends Currency> change) -> {
			Currency added = change.getElementAdded();
			Currency removed = change.getElementRemoved();

			Currency changed = added != null ? added : removed;
			getChildren().stream()
				.filter(node -> node instanceof ToggleButton 
						&& ((ToggleButton) node).getText().equals(changed.symbol))
				.map(node -> (ToggleButton) node)
				.forEach(button -> {
					if (change.wasAdded()) { button.selectedProperty().set(true); }

					else if (change.wasRemoved()) { button.selectedProperty().set(false); }
				});
		});
	}
	
	public void sortByVolume() {
		getChildren().clear();
		BUTTONS.sort(BY_VOLUME);
		getChildren().addAll(BUTTONS);
	}
	
	public void sortBySymbol() {
		getChildren().clear();
		BUTTONS.sort(BY_SYMBOL);
		getChildren().addAll(BUTTONS);
	}
	
	private ToggleButton currencyButton(final Currency currency, Wallet wallet, ObservableSet<Currency> chartCurrencies, ObjectProperty<Currency> highlightCurrency) {
		final ToggleButton button = new ToggleButton(currency.symbol);
		button.setBorder(DEFAULT_BORDER);
		button.setPadding(new Insets(1, 1, 1, 1));

		button.selectedProperty().addListener(new ChangeListener<Boolean>() {
			private boolean bypass = false;

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (bypass) {
					return;
				}

				bypass = true;
				boolean selected = newValue.booleanValue(); 
				if (selected) {
					chartCurrencies.add(currency);
				} else {
					if (wallet.canRemove(currency)) {
						chartCurrencies.remove(currency);
					} else {
						button.selectedProperty().set(true);
					}
				}
				bypass = false;
			}
		});
		
		highlightCurrency.addListener((observable, oldValue, newValue) -> {
			if (currency.equals(newValue)) {
				button.setBorder(HIGHLIGHT_BORDER);
			}
			if (currency.equals(oldValue)) {
				button.setBorder(DEFAULT_BORDER);
			}
		});
		
		return button;
	}

}
