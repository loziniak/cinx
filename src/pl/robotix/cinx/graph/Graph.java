package pl.robotix.cinx.graph;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

import javafx.collections.MapChangeListener;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.VBox;
import pl.robotix.cinx.Currency;
import pl.robotix.cinx.ObservableArrayList;
import pl.robotix.cinx.Point;

public class Graph extends VBox {
	
	private ObservableArrayList<Series<LocalDateTime,Number>> series = new ObservableArrayList<>();

	TimeAxis dates;
	
	public Graph(final PricesHistory pricesHistory) {
		super();
		dates = new TimeAxis(
				LocalDateTime.now().minusDays(20),
				LocalDateTime.now().minusDays(0));
		
		NumberAxis percents = new NumberAxis(-100, 100, 25);
		percents.setAutoRanging(true);
		LineChart<LocalDateTime, Number> chart = new LineChart<>(dates, percents);
		chart.setCreateSymbols(false);
		chart.setData(series);
		
		getChildren().add(chart);

		pricesHistory.displayedCurrencies.addListener((MapChangeListener.Change<? extends Currency, ? extends List<Point>> change) -> {
			if (change.wasAdded()) {
				display(change.getValueAdded(), change.getKey());
			} else if (change.wasRemoved()) {
				remove(change.getKey());
			}
		});
	}

	public void display(List<Point> prices, Currency currency) {
		
		Point last = prices.get(prices.size() - 1);
		
		ObservableArrayList<Data<LocalDateTime,Number>> percents = new ObservableArrayList<>();
		prices.forEach((pt) -> percents.add(new Data<>(pt.date, (pt.value / last.value - 1) * 100)));
		
		dates.newRange(prices.get(0).date, last.date);
		
		Series<LocalDateTime,Number> s = new Series<>();
		s.setData(percents);
		s.setName(currency.symbol);
		series.add(s);
	}
	
	public void remove(Currency currency) {
		Iterator<Series<LocalDateTime, Number>> i = series.iterator();
		int index = -1;
		boolean found = false;
		while (i.hasNext() && !found) {
			index++;
			if (i.next().getName().equals(currency.symbol)) {
				found = true;
			}
		}
		if (found) {
			series.remove(index);
		}
	}

}
