package pl.robotix.cinx.wallet;

import java.util.function.Consumer;

import javafx.beans.binding.When;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
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
	
	private final Button remove = new Button("X");
	

	public WalletSlider(WalletEntry entry, ObjectProperty<Currency> highlightCurrency, Consumer<WalletSlider> onFreezeWithCTRL, ObservableSet<Currency> chartCurrencies) {
		super();
		currency = entry.getCurrency();
		
		getChildren().add(new Text(entry.getCurrency().symbol));
		getChildren().add(percentChange);
		getChildren().add(slider.node());
		getChildren().add(freeze);
		getChildren().add(remove);
		
		slider.valueProperty().bindBidirectional(entry.percent);

		percentChange.textProperty().bind(entry.percentChange.asString("%+.1f"));
		percentChange.textFillProperty().bind(
				new When(entry.percentChange.lessThan(0.0))
				.then(Color.RED)
				.otherwise(new When(entry.percentChange.greaterThan(0.0))
						.then(Color.BLUE)
						.otherwise(Color.GREY)));
		
		freeze.setPadding(new Insets(3));
		entry.freeze.bind(freeze.selectedProperty());
		freeze.setOnMouseClicked((event) -> {
			if (event.isControlDown()) {
				onFreezeWithCTRL.accept(this);
			}
		});
		
		entry.enabled.addListener((observable, oldValue, newValue) -> {
			boolean enabled = newValue;
			if (enabled) {
				slider.enable();
			} else {
				slider.disable();
			}
		});
		
		freeze.setPadding(new Insets(3));
		remove.setOnAction(event -> {
			if (entry.canRemove()) {
				chartCurrencies.remove(currency);
			}
		});
		
		entry.bindIsChanging(slider.valueChangingProperty());
		
		setOnMouseEntered((event) -> {
			highlightCurrency.set(currency);
		});
		
		setOnScroll(event -> {
			slider.valueChangingProperty().set(true);

			double percent = event.getDeltaY() / event.getMultiplierY();

			if (event.isControlDown()) {
				percent *= 0.1;
			}

			double current = entry.percent.doubleValue();
			if (- percent > current) { percent = - current; }
			else if (current + percent > 100.0) { percent = 100 - current; } 
			System.out.println("scroll " + percent + " " + current + " " + slider.valueProperty().doubleValue());

			slider.valueProperty().set(current + percent);

			slider.valueChangingProperty().set(false);
		});
	}
	
	public void freeze(boolean freeze) {
		this.freeze.setSelected(freeze);
	}
	
	public boolean isFreeze() {
		return this.freeze.isSelected();
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
