package pl.robotix.cinx.wallet;

import static javafx.geometry.Orientation.VERTICAL;

import javafx.beans.binding.When;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.StringConverter;
import pl.robotix.cinx.Currency;

public class WalletSlider extends VBox {

	private static final EventHandler<MouseEvent> DRAG_FILTER = (event) -> {
		event.consume();
	};
	
	private static final StringConverter<Double> SINGLE_DIGIT = new StringConverter<Double>() {
		@Override public String toString(Double value) { return String.format("%.1f", value); }
		@Override public Double fromString(String label) { return new Double(label); }
	};
	
	private final Currency currency;
	
	private final Slider slider = new Slider();

	private final Label percentChange = new Label(String.format("%+.1f", 0.0));
	
	private final CheckBox freeze = new CheckBox();
	
	public WalletSlider(WalletEntry s) {
		super();
		currency = s.getCurrency();
		
		getChildren().add(new Text(s.getCurrency().symbol));
		getChildren().add(percentChange);
		getChildren().add(slider);
		getChildren().add(freeze);
		
		slider.setOrientation(VERTICAL);
		slider.setMin(0.0);
		slider.setMax(100.0);
		slider.setShowTickLabels(true);
		slider.setLabelFormatter(SINGLE_DIGIT);
		slider.setMinHeight(250);
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
				enable();
			} else {
				disable();
			}
		});
		
		s.bindIsChanging(slider.valueChangingProperty());
	}

	public void disable() {
		slider.setDisable(true);
		slider.addEventFilter(MouseEvent.MOUSE_DRAGGED, DRAG_FILTER);
	}

	public void enable() {
		slider.setDisable(false);
		slider.removeEventFilter(MouseEvent.MOUSE_DRAGGED, DRAG_FILTER);
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
