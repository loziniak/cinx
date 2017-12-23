package pl.robotix.cinx;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class App extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Cinx");

		NumberAxis percents = new NumberAxis(0, 100, 25);

		MyAxis<LocalDateTime> dates = new MyAxis<LocalDateTime>(
				LocalDateTime.now().minusDays(20),
				LocalDateTime.now().minusDays(0)) {

			@Override
			public double toNumericValue(LocalDateTime value) {
				return value.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
			}

			@Override
			public LocalDateTime toRealValue(double value) {
				return Instant.ofEpochSecond(Double.valueOf(value).longValue())
						.atZone(ZoneId.systemDefault()).toLocalDateTime();
			}

			@Override
			protected String getTickMarkLabel(LocalDateTime value) {
				return value.toString();
			}
		};
		
		
		LineChart<LocalDateTime, Number> chart = new LineChart<>(dates, percents);

		Series<LocalDateTime,Number> s = new Series<>();
		MyList<Data<LocalDateTime,Number>> points = new MyList<>();
		points.add(new Data<LocalDateTime,Number>(LocalDateTime.now().minusDays(15), 25));
		points.add(new Data<LocalDateTime,Number>(LocalDateTime.now().minusDays(11), 63));
		points.add(new Data<LocalDateTime,Number>(LocalDateTime.now().minusDays(5), 25));
		s.setData(points);
		
		MyList<Series<LocalDateTime,Number>> l = new MyList<>();
		l.add(s);
		chart.setData(l);
		

		Group root = new Group();
		root.getChildren().add(chart);
		primaryStage.setScene(new Scene(root));
		primaryStage.show();
	}

	private void drawShapes(GraphicsContext gc) {
		gc.setFill(Color.GREEN);
		gc.setStroke(Color.BLUE);
		gc.setLineWidth(5);
		gc.strokeLine(40, 10, 10, 40);
	}

	public static void main(String[] args) {
		launch(args);
	}
}
