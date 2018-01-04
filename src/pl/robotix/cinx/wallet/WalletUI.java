package pl.robotix.cinx.wallet;

import javafx.collections.MapChangeListener.Change;
import javafx.scene.layout.HBox;
import pl.robotix.cinx.Currency;

public class WalletUI extends HBox {
	
	public WalletUI(Wallet wallet) {
		super();
		
		wallet.sliders.forEach((currency, slider) -> {
			getChildren().add(new Sliderx(slider));
		});
		
		wallet.sliders.addListener((Change<? extends Currency,? extends WalletSlider> change) -> {
			if (change.wasAdded()) {
				getChildren().add(new Sliderx(change.getValueAdded()));
			}
			if (change.wasRemoved()) {
				getChildren().remove(new Sliderx(change.getValueRemoved()));
			}
			
		});
	}

}
