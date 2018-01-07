package pl.robotix.cinx.wallet;

import static java.util.Collections.reverseOrder;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.MapChangeListener.Change;
import javafx.scene.layout.HBox;
import pl.robotix.cinx.Currency;

public class WalletUI extends HBox {
	
	public WalletUI(Wallet wallet) {
		super();
		
		List<WalletEntry> sliders = new ArrayList<>(wallet.sliders.values());
		sliders.sort(reverseOrder((s1, s2) -> {
			return Double.compare(s1.percent.get(), s2.percent.get());
		}));
		sliders.forEach((slider) -> {
			getChildren().add(new WalletSlider(slider));
		});
		
		wallet.sliders.addListener((Change<? extends Currency,? extends WalletEntry> change) -> {
			if (change.wasAdded()) {
				getChildren().add(new WalletSlider(change.getValueAdded()));
			}
			if (change.wasRemoved()) {
				getChildren().remove(new WalletSlider(change.getValueRemoved()));
			}
			
		});
	}

}