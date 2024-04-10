package pl.robotix.cinx.log;

import static pl.robotix.cinx.App.fromEpochSeconds;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import pl.robotix.cinx.Currency;
import pl.robotix.cinx.TimeRange;
import pl.robotix.cinx.graph.TimeAxis;
import pl.robotix.cinx.trade.Operation.Type;

public class OperationsUI extends Canvas {
	
	private static final int BIAS_CALIBRATION_PX = 5;
	
	private static final Color BUY_COLOR = Color.LIGHTSKYBLUE.deriveColor(0, 1, 1, 0.5);
	private static final Color SELL_COLOR = Color.ORANGERED.deriveColor(0, 1, 1, 0.5);
	
	private ObjectProperty<TimeRange> range = new SimpleObjectProperty<>();
	
	private ObjectProperty<Bounds> drawBounds = new SimpleObjectProperty<Bounds>(new BoundingBox(0, 0, 0, 0));
	
	private final OperationLog log;
	
	private List<Circle> circles = new ArrayList<>();
	
	private TimeAxis xAxis;
	
	private NumberAxis yAxis;
	
	public OperationsUI(LineChart<LocalDateTime, Number> chart, OperationLog log) {
		super();
		this.log = log;

		widthProperty().bind(chart.widthProperty());
		widthProperty().addListener((e) -> redraw());
		heightProperty().bind(chart.heightProperty());
		heightProperty().addListener((e) -> redraw());
		
		Pane chartContent = (Pane) chart.getChildrenUnmodifiable().get(1);
		Group plotArea = (Group) chartContent.getChildrenUnmodifiable().get(1);
		xAxis = (TimeAxis) chartContent.getChildrenUnmodifiable().get(2);
		yAxis = (NumberAxis) chartContent.getChildrenUnmodifiable().get(3);

		plotArea.boundsInLocalProperty().addListener((property, oldVal, newVal) -> {
			drawBounds.set(new BoundingBox(
					newVal.getMinX() + BIAS_CALIBRATION_PX,  newVal.getMinY() + BIAS_CALIBRATION_PX, 
					newVal.getWidth(), newVal.getHeight()));
		});
		drawBounds.addListener((o, oldVal, newVal) -> {
			redraw();
		});
	}

	public void drawHistoryFor(List<Data<LocalDateTime, Number>> data, Currency c) {
		circles.clear();

		for (LoggedOperation op: log.pastOperationsFor(c, range.get())) {

			if (Math.abs(op.percentChange) >= 0.5) {

				Data<LocalDateTime, Number> lastData = data.get(0);
				var operationTime = fromEpochSeconds(op.operationTime.getEpochSecond());
				for (Data<LocalDateTime, Number> d: data) {
					if (d.getXValue().isBefore(operationTime)) {
						lastData = d;
					} else {
						break;
					}
				}
				
				circles.add(new Circle(
						new Point2D(
								drawBounds.get().getMinX() + xAxis.getDisplayPosition(lastData.getXValue()),
								drawBounds.get().getMinY() + yAxis.getDisplayPosition(lastData.getYValue())),
						Math.abs(op.percentChange * 2) + 2,
						op.type == Type.BUY ? BUY_COLOR : SELL_COLOR));
			}
		}
		redraw();
	}

	private void redraw() {
		GraphicsContext gc = getGraphicsContext2D();
		gc.clearRect(0, 0, getWidth(), getHeight());
		
//		drawCalibration(gc);
		
		gc.setLineWidth(2.5);
		gc.setStroke(Color.WHITE);
		for (Circle c: circles) {
			c.draw(gc);
		}
	}
	
	@SuppressWarnings("unused")
	private void drawCalibration(GraphicsContext gc) {
		gc.setFill(Color.RED);
		gc.fillRect(drawBounds.get().getMinX(),      drawBounds.get().getMinY(),      10, 10);
		gc.setFill(Color.GREEN);
		gc.fillRect(drawBounds.get().getMaxX() - 10, drawBounds.get().getMaxY() - 10, 10, 10);
		gc.setFill(Color.BLUE);
		gc.fillRect(drawBounds.get().getMinX(),      drawBounds.get().getMaxY() - 10, 10, 10);
		gc.setFill(Color.BLACK);
		gc.fillRect(drawBounds.get().getMaxX() - 10, drawBounds.get().getMinY(),      10, 10);

		gc.setFill(Color.ORANGE);
		gc.fillRect(0,               0,                10, 10);
		gc.setFill(Color.OLIVE);
		gc.fillRect(getWidth() - 10, getHeight() - 10, 10, 10);
		gc.setFill(Color.VIOLET);
		gc.fillRect(0,               getHeight() - 10, 10, 10);
		gc.setFill(Color.PINK);
		gc.fillRect(getWidth() - 10, 0,                10, 10);
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
	

	public ObjectProperty<TimeRange> getRangeProperty() {
		return range;
	}
	

	private static class Circle {
		
		private Point2D center;
		
		private double diameter;
		
		private Color color;
		
		public Circle(Point2D center, double diameter, Color color) {
			super();
			this.center = center;
			this.diameter = diameter;
			this.color = color;
		}

		public void draw(GraphicsContext gc) {
			Paint oldStroke = gc.getStroke();
			Paint oldFill = gc.getFill();

			Color col = adjust(color, diameter);
			gc.setStroke(col.darker());
			gc.setFill(adjust(col, diameter));
			gc.fillOval(center.getX() - diameter / 2, center.getY() - diameter / 2,
					diameter, diameter);
			gc.strokeOval(center.getX() - diameter / 2 - 1, center.getY() - diameter / 2 - 1,
					diameter + 2, diameter + 2);

			gc.setStroke(oldStroke);
			gc.setFill(oldFill);
		}
		
		private Color adjust(Color c, double diameter) {
			return new Color(c.getRed(), c.getGreen(), c.getBlue(), 1.0 / Math.pow(diameter, 0.3));
		}
		
	}
}
