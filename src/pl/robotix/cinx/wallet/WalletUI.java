package pl.robotix.cinx.wallet;

import static java.util.Collections.reverseOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.beans.property.ObjectProperty;
import javafx.collections.MapChangeListener.Change;
import javafx.scene.layout.HBox;
import pl.robotix.cinx.Currency;

public class WalletUI extends HBox {
	
	private Consumer<WalletSlider> checkAllFreeze = (sliderToUncheck) -> {
		getChildren().forEach((slider) -> {
			((WalletSlider) slider).freeze(
					((WalletSlider) slider).isFreeze() == (slider == sliderToUncheck));
		});
	};
	
	public WalletUI(Wallet wallet, ObjectProperty<Currency> highlihtCurrency) {
		super();
		
		List<WalletEntry> sliders = new ArrayList<>(wallet.sliders.values());
		sliders.sort(reverseOrder((s1, s2) -> {
			return Double.compare(s1.percent.get(), s2.percent.get());
		}));
		sliders.forEach((slider) -> {
			getChildren().add(new WalletSlider(slider, highlihtCurrency, checkAllFreeze));
		});
		
		wallet.sliders.addListener((Change<? extends Currency,? extends WalletEntry> change) -> {
			if (change.wasAdded()) {
				getChildren().add(new WalletSlider(change.getValueAdded(), highlihtCurrency, checkAllFreeze));
			}
			if (change.wasRemoved()) {
				getChildren().remove(new WalletSlider(change.getValueRemoved(), highlihtCurrency, checkAllFreeze));
			}
			
		});
	}

}
