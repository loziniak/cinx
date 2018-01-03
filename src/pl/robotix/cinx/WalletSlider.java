package pl.robotix.cinx;

import static javafx.geometry.Orientation.VERTICAL;

import java.util.function.BiConsumer;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.When;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.StringConverter;

public class WalletSlider {
	
	private static final StringConverter<Double> SINGLE_DIGIT = new StringConverter<Double>() {
		@Override public String toString(Double value) { return String.format("%.1f", value); }
		@Override public Double fromString(String label) { return new Double(label); }
	};
	
	private static final EventHandler<MouseEvent> DRAG_FILTER = (event) -> {
		event.consume();
	};
	
	private final Currency currency;
	private final double originalPercent;
	private final DoubleBinding changeBinding;
	
	private Slider slider = new Slider();
	private Label percentChange = new Label(SINGLE_DIGIT.toString(0.0));
	
	private VBox pane = new VBox();
	
	
	public WalletSlider(Currency c, double walletUSD, double originalPrice) {
		this.currency = c;
		this.originalPercent = 100.0 * originalPrice / walletUSD;

		pane.getChildren().add(new Text(c.symbol));
		pane.getChildren().add(percentChange);
		pane.getChildren().add(slider);
		slider.setOrientation(VERTICAL);
		slider.setMin(0.0);
		slider.setMax(100.0);
		slider.setShowTickLabels(true);
		slider.setLabelFormatter(SINGLE_DIGIT);
		slider.setMinHeight(250);
		slider.setValue(originalPercent);
		
		changeBinding = slider.valueProperty().subtract(originalPercent);
		slider.valueProperty().addListener(
				(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
			if (newValue.doubleValue() == slider.getMax()) {
				disable();
			}
		});
		
		percentChange.textProperty().bind(changeBinding.asString("%+.1f"));
		percentChange.textFillProperty().bind(
				new When(changeBinding.lessThan(0.0))
				.then(Color.RED)
				.otherwise(new When(changeBinding.greaterThan(0.0))
						.then(Color.BLUE)
						.otherwise(Color.GREY)));
	}
	
	
	public double getPercent() {
		return slider.getValue();
	}
	
	public void setPercent(double percent) {
		if (percent > 99.0) percent = 100.0;
		if (percent < 1.0) percent = 0.0;
		slider.setValue(percent);
	}
	
	public Node getNode() {
		return pane;
	}
	
	public Currency getCurrency() {
		return currency;
	}
	
	public double getPercentChange() {
		return changeBinding.doubleValue();
	}
	
	public void disable() {
		slider.setDisable(true);
		slider.setMouseTransparent(true);
		
		slider.addEventFilter(MouseEvent.MOUSE_DRAGGED, DRAG_FILTER);
	}

	public void enable() {
		slider.setDisable(false);
		slider.setMouseTransparent(false);
		
		slider.removeEventFilter(MouseEvent.MOUSE_DRAGGED, DRAG_FILTER);
	}
	
	public void setPercentChangeHandler(BiConsumer<Double, WalletSlider> onPercentChange) {
		slider.valueProperty().addListener(
				(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
			onPercentChange.accept(newValue.doubleValue() - oldValue.doubleValue(), this);
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
