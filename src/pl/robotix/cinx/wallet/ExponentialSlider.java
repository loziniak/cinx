package pl.robotix.cinx.wallet;

import static javafx.geometry.Orientation.VERTICAL;

import java.util.function.Function;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.util.StringConverter;

public class ExponentialSlider {
	
	private static final EventHandler<MouseEvent> DRAG_FILTER = (event) -> {
		event.consume();
	};
	
	private final StringConverter<Double> singleDigit = new StringConverter<Double>() {
		@Override public String toString(Double value) { return String.format("%.1f", func.apply(value)); }
		@Override public Double fromString(String label) { return reverseFunc.apply(new Double(label)); }
	};
	
	private Slider slider = new Slider(0, 1, 0);
	{
		slider.setOrientation(VERTICAL);
		slider.setShowTickLabels(true);
		slider.setLabelFormatter(singleDigit);
		slider.setMinHeight(250);
		slider.setMajorTickUnit(0.125);
	}
	
	private DoubleProperty exponentialValue = new SimpleDoubleProperty(0);
	
	private double A, B, C;
	
	private Function<Double, Double> func;
	
	private Function<Double, Double> reverseFunc;

	public ExponentialSlider() {
		this(0, 100);
	}

	public ExponentialSlider(double min, double max) {
		constructLinear(min, max);
	}

	public ExponentialSlider(double min, double mid, double max) {
		if (min + max == 2 * mid) {
			constructLinear(min, max);
		} else {
			constructExp(min, mid, max);
		}
	}
	
	private void constructLinear(double min, double max) {
		if (min >=  max) {
			throw new IllegalArgumentException("Max has to be higher than min.");
		}
		
		A = max - min;
		B = min;
		
		func = (val) -> B + val * A;
		reverseFunc = (val) -> (val - B) / A;
		
		bindValue();
	}

	private void constructExp(double min, double mid, double max) {
		if (min >= mid || mid >= max) {
			throw new IllegalArgumentException("Mid has to be between min and max.");
		}
		
		A = (min * max - mid * mid) / (min - 2 * mid + max);
		B = (mid - min) * (mid - min) / (min - 2 * mid + max);
		C = 2 * Math.log((max - mid) / (mid - min));
		
		func = (val) -> A + B * Math.exp(C * val);
		reverseFunc = (val) -> Math.log((val - A) / B) / C;

		bindValue();
	}
	
	private void bindValue() {
		ChangeListener<Number> bidirectionalListener = new ChangeListener<Number>() {

			boolean changing = false;

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				if (changing) return;
				
				changing = true;
				if (observable == exponentialValue) {
					slider.setValue(reverseFunc.apply(newValue.doubleValue()));
				} else if (observable == slider.valueProperty()) {
					exponentialValue.set(func.apply(newValue.doubleValue()));
				}
				changing = false;
			}
		};

		exponentialValue.addListener(bidirectionalListener);
		slider.valueProperty().addListener(bidirectionalListener);
	}
	
	public Node node() {
		return slider;
	}
	
	public DoubleProperty valueProperty() {
		return exponentialValue;
	}
	
	public void disable() {
		slider.setDisable(true);
		slider.addEventFilter(MouseEvent.MOUSE_DRAGGED, DRAG_FILTER);
	}

	public void enable() {
		slider.setDisable(false);
		slider.removeEventFilter(MouseEvent.MOUSE_DRAGGED, DRAG_FILTER);
	}
	
	
	public BooleanProperty valueChangingProperty() {
		return slider.valueChangingProperty();
	}

}
