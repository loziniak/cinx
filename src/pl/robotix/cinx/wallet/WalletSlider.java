package pl.robotix.cinx.wallet;

import javafx.beans.binding.When;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import pl.robotix.cinx.Currency;

public class WalletSlider extends VBox {

	private final Currency currency;
	
	private final ExponentialSlider slider = new ExponentialSlider(0, 20, 100);

	private final Label percentChange = new Label(String.format("%+.1f", 0.0));
	
	private final CheckBox freeze = new CheckBox();
	

	public WalletSlider(WalletEntry s, ObjectProperty<Currency> highlihtCurrency) {
		super();
		currency = s.getCurrency();
		
		getChildren().add(new Text(s.getCurrency().symbol));
		getChildren().add(percentChange);
		getChildren().add(slider.node());
		getChildren().add(freeze);
		
		slider.valueProperty().bindBidirectional(s.percent);

		percentChange.textProperty().bind(s.percentChange.asString("%+.1f"));
		percentChange.textFillProperty().bind(
				new When(s.percentChange.lessThan(0.0))
				.then(Color.RED)
				.otherwise(new When(s.percentChange.greaterThan(0.0))
						.then(Color.BLUE)
						.otherwise(Color.GREY)));
		
		s.freeze.bind(freeze.selectedProperty());
		
		s.enabled.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
			boolean enabled = newValue;
			if (enabled) {
				slider.enable();
			} else {
				slider.disable();
			}
		});
		
		s.bindIsChanging(slider.valueChangingProperty());
		
		setOnMouseEntered((event) -> {
			highlihtCurrency.set(currency);
		});
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof WalletSlider && ((WalletSlider) obj).currency.equals(this.currency);
	}
	
	@Override
	public int hashCode() {
		return currency.hashCode();
	}

}
