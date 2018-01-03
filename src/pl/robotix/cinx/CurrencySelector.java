package pl.robotix.cinx;

import static pl.robotix.cinx.App.USDT;
import static pl.robotix.cinx.TimeRange.MONTH;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener.Change;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import pl.robotix.cinx.api.Api;
import pl.robotix.cinx.graph.Graph;

public class CurrencySelector {

	private ObservableSet<Currency> chartCurrencies = FXCollections.observableSet();
	
	public CurrencySelector() {
	}
	
	public void addDisplatListener(Api api, Graph graph) {
		chartCurrencies.addListener((Change<? extends Currency> change) -> {
			if (change.wasAdded()) {
				Currency added = change.getElementAdded();
				if (!added.equals(USDT)) {
					List<Point> priceHistory = api.retrieveUSDPriceHistory(added, MONTH);
					graph.display(priceHistory, added);
				}
			}
			if (change.wasRemoved()) {
				graph.remove(change.getElementRemoved());
			}
		});

	}

	public FlowPane buttons(Api api, WalletCurrencies wallet) {
		FlowPane currencies = new FlowPane();
		currencies.setVgap(5);
		currencies.setHgap(5);
		
		List<Currency> currencyList = new ArrayList<>(api.getPrices().getAllCurrencies());
		currencyList.sort(api.getPrices().byVolume());
		for (Currency c: currencyList) {
			currencies.getChildren().add(currencyButton(c, wallet));
		}
		return currencies;
	}
	
	private ToggleButton currencyButton(final Currency currency, WalletCurrencies wallet) {
		final ToggleButton button = new ToggleButton(currency.symbol);
		button.selectedProperty().addListener((selected) -> {
			if (((BooleanProperty) selected).get()) {
				chartCurrencies.add(currency);
			} else {
				chartCurrencies.remove(currency);
			}
		});
		chartCurrencies.addListener((Change<? extends Currency> change) -> {
			Currency added = change.getElementAdded();
			Currency removed = change.getElementRemoved();

			if (change.wasAdded() && added.equals(currency)) {
				if (!button.isSelected()) {
					button.setSelected(true);
				}
				wallet.add(currency);
			}
			if (change.wasRemoved() && removed.equals(currency)) {
				if (button.isSelected()) {
					button.setSelected(false);
				}
				wallet.remove(currency);
			}
		});
		return button;
	}


	public void addAll(Collection<Currency> c) {
		chartCurrencies.addAll(c);
	}

}
