package pl.robotix.cinx.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.scene.chart.Axis;

public abstract class MyAxis<T> extends Axis<T> {
	
	private T lower;
	
	private T upper;
	
	
	public MyAxis(T low, T up) {
		setAutoRanging(false);
		lower = low;
		upper = up;
	}
	
	
	public void newRange(T low, T up) {
		lower = low;
		upper = up;
		invalidateRange();
	}
	
	
	
	@Override
	public double getZeroPosition() {
		if (!isValueOnAxis(toRealValue(0.0))) {
			return Double.NaN;
		}
		return getDisplayPosition(toRealValue(0.0));
	}

	@Override
	public double getDisplayPosition(T value) {
		double length = getSide().isVertical() ? getHeight() :getWidth();
		double low = toNumericValue(lower);
		double up = toNumericValue(upper);
		double val = toNumericValue(value);
		double scale = length/(up - low);

		double pos = scale * (val - low);
		return pos;
	}

	@Override
	public T getValueForDisplay(double displayPosition) {
		return toRealValue(displayPosition);
	}

	@Override
	public boolean isValueOnAxis(T value) {
		return toNumericValue(lower) <= toNumericValue(value)
				&& toNumericValue(upper) >= toNumericValue(value);
	}
	
	@Override
	protected Object autoRange(double length) {
		if (isAutoRanging()) {
			throw new IllegalStateException("No auto range.");
		}
		return getRange();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void setRange(Object range, boolean animate) {
		lower = ((List<T>) range).get(0);
		upper = ((List<T>) range).get(1);
	}

	@Override
	protected Object getRange() {
		return Arrays.asList(lower, upper);
	}

	@Override
	protected List<T> calculateTickValues(double length, Object range) {
		@SuppressWarnings("unchecked")
		double low = toNumericValue(((List<T>) range).get(0));
		@SuppressWarnings("unchecked")
		double up = toNumericValue(((List<T>) range).get(1));

		List<T> tickValues = new ArrayList<>();
		tickValues.add(toRealValue(low));
		tickValues.add(toRealValue(low + (up - low)/4));
		tickValues.add(toRealValue(low + (up - low)/2));
		tickValues.add(toRealValue(low + (up - low)/4*3));
		tickValues.add(toRealValue(up));
		
		return tickValues;
	}
	
	public T getLower() {
		return lower;
	}
	
	public T getUpper() {
		return upper;
	}

}
