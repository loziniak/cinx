package pl.robotix.cinx.graph;

import java.time.LocalDateTime;
import java.util.List;

import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import pl.robotix.cinx.ObservableArrayList;
import pl.robotix.cinx.Point;

public class Graph {
	
	private LineChart<LocalDateTime, Number> chart;
	private ObservableArrayList<Series<LocalDateTime,Number>> l = new ObservableArrayList<>();
	NumberAxis percents = new NumberAxis(-100, 100, 25);

	TimeAxis dates = new TimeAxis(
			LocalDateTime.now().minusDays(20),
			LocalDateTime.now().minusDays(0));
	
	public Graph() {
		chart = new LineChart<>(dates, percents);
		chart.setCreateSymbols(false);
		chart.setData(l);
	}

	public Chart getChart() {
		return chart;
	}
	
	public void display(List<Point> prices, String name) {
		
		Point last = prices.get(prices.size() - 1);
		
		ObservableArrayList<Data<LocalDateTime,Number>> percents = new ObservableArrayList<>();
		prices.forEach((pt) -> percents.add(new Data<>(pt.date, (pt.value / last.value - 1) * 100)));
		
		dates.newRange(prices.get(0).date, last.date);
		
		Series<LocalDateTime,Number> s = new Series<>();
		s.setData(percents);
		s.setName(name);
		l.add(s);
	}

}
