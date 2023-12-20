package pl.robotix.cinx.graph;

import static javafx.collections.FXCollections.observableArrayList;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import pl.robotix.cinx.Currency;
import pl.robotix.cinx.ObservableArrayList;
import pl.robotix.cinx.Point;
import pl.robotix.cinx.TimeRange;
import pl.robotix.cinx.log.OperationLog;
import pl.robotix.cinx.log.OperationsUI;

public class Graph extends VBox {
	
	private ObservableArrayList<Series<LocalDateTime,Number>> series = new ObservableArrayList<>();
	private TimeAxis dates;
	
	OperationsUI history;
	
	public Graph(final PricesHistory pricesHistory, ObjectProperty<Currency> highlihtCurrency, OperationLog operationLog) {
		super();
		setFillWidth(true);

		ChoiceBox<TimeRange> timeRanges = new ChoiceBox<>(observableArrayList(TimeRange.values()));
		timeRanges.getSelectionModel().select(pricesHistory.timeRange.get());
		pricesHistory.timeRange.bind(timeRanges.getSelectionModel().selectedItemProperty());
		getChildren().add(timeRanges);
		
		dates = new TimeAxis(
				LocalDateTime.now().minusDays(20),
				LocalDateTime.now().minusDays(0));
		NumberAxis percents = new NumberAxis(-100, 100, 25);
		percents.setAutoRanging(true);
		LineChart<LocalDateTime, Number> chart = new LineChart<>(dates, percents);
		chart.setAnimated(false);
		chart.setCreateSymbols(false);
		chart.setData(series);
		
		
		StackPane chartWithHistory = new StackPane();
		setVgrow(chartWithHistory, Priority.ALWAYS);
		chartWithHistory.getChildren().add(chart);

		history = new OperationsUI(chart, operationLog);
		chartWithHistory.getChildren().add(history);
		history.getRangeProperty().bind(pricesHistory.timeRange);

		getChildren().add(chartWithHistory);
		
		pricesHistory.displayedCurrencies.addListener((MapChangeListener.Change<? extends Currency, ? extends List<Point>> change) -> {
			if (change.wasAdded()) {
				display(change.getValueAdded(), change.getKey());
				highlihtCurrency.set(change.getKey());
			} else if (change.wasRemoved()) {
				remove(change.getKey());
			}
		});
		
		highlihtCurrency.addListener((observable, oldValue, newValue) -> {
			if ((newValue == null && oldValue != null)
					|| newValue != null && !newValue.equals(oldValue)) {
				
				thick(oldValue, newValue);
			}
		});
	}

	public void display(List<Point> prices, Currency currency) {
		
		Point last = prices.get(prices.size() - 1);
		
		ObservableArrayList<Data<LocalDateTime,Number>> percents = new ObservableArrayList<>();
		prices.forEach((pt) -> percents.add(new Data<>(pt.date, (pt.value / last.value - 1) * 100)));
		
		dates.newRange(prices.get(0).date, last.date);
		
		display(percents, currency);
	}

	private void display(ObservableList<Data<LocalDateTime,Number>> data, Currency currency) {
		Series<LocalDateTime,Number> s = new Series<>();
		s.setData(data);
		s.setName(currency.symbol);
		series.add(s);
		s.nodeProperty().get().setStyle("-fx-stroke-width: 1px;");
	}
	
	public Series<LocalDateTime, Number> remove(Currency currency) {
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
			return series.remove(index);
		}
		return null;
	}
	
	private void thick(Currency thin, Currency thick) {
		series.forEach((s) -> {
			if (thin != null && s.getName().equals(thin.symbol)) {
				s.nodeProperty().get().setStyle("-fx-stroke-width: 1px;");
			}
			if (thick != null && s.getName().equals(thick.symbol)) {
				s.nodeProperty().get().setStyle("-fx-stroke-width: 3px;");
			}
		});
	}

}
