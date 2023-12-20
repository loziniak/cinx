package pl.robotix.cinx.wallet;

import static java.util.Collections.reverseOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.beans.property.ObjectProperty;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.ObservableSet;
import javafx.scene.layout.HBox;
import pl.robotix.cinx.Currency;

public class WalletUI extends HBox {
	
	private Consumer<WalletSlider> checkAllFreeze = (sliderToUncheck) -> {
		getChildren().forEach((slider) -> {
			((WalletSlider) slider).freeze(
					((WalletSlider) slider).isFreeze() == (slider == sliderToUncheck));
		});
	};
	
	public WalletUI(Wallet wallet, ObservableSet<Currency> chartCurrencies, ObjectProperty<Currency> highlightCurrency) {
		super();
		
		List<WalletEntry> sliders = new ArrayList<>(wallet.sliders.values());
		sliders.sort(reverseOrder((s1, s2) -> {
			return Double.compare(s1.percent.get(), s2.percent.get());
		}));
		sliders.forEach((slider) -> {
			getChildren().add(new WalletSlider(slider, highlightCurrency, checkAllFreeze, chartCurrencies));
		});
		
		wallet.sliders.addListener((Change<? extends Currency,? extends WalletEntry> change) -> {
			if (change.wasAdded()) {
				getChildren().add(new WalletSlider(change.getValueAdded(), highlightCurrency, checkAllFreeze, chartCurrencies));
			}
			if (change.wasRemoved()) {
				getChildren().remove(new WalletSlider(change.getValueRemoved(), highlightCurrency, checkAllFreeze, chartCurrencies));
			}
			
		});
	}

}
