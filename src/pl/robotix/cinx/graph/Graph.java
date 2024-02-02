package pl.robotix.cinx.graph;

import static javafx.collections.FXCollections.observableArrayList;
import static pl.robotix.cinx.Currency.WALLET;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.chart.AreaChart;
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
	private ObservableArrayList<Series<LocalDateTime,Number>> volume = new ObservableArrayList<>();

	private TimeAxis dates;
	
	OperationsUI history;
	
	public Graph(final PricesHistory pricesHistory, ObjectProperty<Currency> highlightCurrency, OperationLog operationLog) {
		super();
		setFillWidth(true);

		ChoiceBox<TimeRange> timeRanges = new ChoiceBox<>(observableArrayList(TimeRange.values()));
		timeRanges.getSelectionModel().select(pricesHistory.timeRange.get());
		pricesHistory.timeRange.bind(timeRanges.getSelectionModel().selectedItemProperty());
		getChildren().add(timeRanges);
		
		dates = new TimeAxis(
				LocalDateTime.now().minusDays(20),
				LocalDateTime.now().minusDays(0));
		
		AreaChart<LocalDateTime, Number> volumeChart = volumeChart();
		
		LineChart<LocalDateTime, Number> priceChart = priceChart();
		
		
		StackPane chartWithHistory = new StackPane();
		setVgrow(chartWithHistory, Priority.ALWAYS);
		chartWithHistory.getChildren().add(volumeChart);
		chartWithHistory.getChildren().add(priceChart);

		history = new OperationsUI(priceChart, operationLog);
		chartWithHistory.getChildren().add(history);
		history.getRangeProperty().bind(pricesHistory.timeRange);

		getChildren().add(chartWithHistory);
		
		pricesHistory.displayedCurrencies.addListener((MapChangeListener.Change<? extends Currency, ? extends List<Point>> change) -> {
			if (change.wasAdded()) {
				display(change.getValueAdded(), change.getKey());
				highlightCurrency.set(change.getKey());
			} else if (change.wasRemoved()) {
				remove(change.getKey());
			}
		});
		
		highlightCurrency.addListener((observable, oldCurrency, newCurrency) -> {
			if (newCurrency != null && !newCurrency.equals(oldCurrency)) {
				
				var oldSeries = seriesFor(oldCurrency);
				var newSeries = seriesFor(newCurrency);

				mark(oldSeries, newSeries);
				this.history.drawHistoryFor(newSeries.getData(), newCurrency);
				displayVolume(newCurrency, pricesHistory);
			}
		});
	}

	private LineChart<LocalDateTime, Number> priceChart() {
		NumberAxis percents = new NumberAxis(-100, 100, 25);
		percents.setAutoRanging(true);
		percents.setPrefWidth(40);
		LineChart<LocalDateTime, Number> priceChart = new LineChart<>(dates, percents);
		priceChart.setAnimated(false);
		priceChart.setCreateSymbols(false);
		priceChart.setData(series);
		return priceChart;
	}

	private AreaChart<LocalDateTime, Number> volumeChart() {
		NumberAxis volumes = new NumberAxis(0, 0, 12.5);
		volumes.setTickLabelsVisible(false);
		volumes.setTickMarkVisible(false);
		volumes.setMinorTickVisible(false);
		AreaChart<LocalDateTime, Number> volumeChart = new AreaChart<>(dates, volumes);
		volumeChart.setAnimated(false);
		volumeChart.setCreateSymbols(false);
		volumeChart.setData(volume);
		volumeChart.setLegendVisible(false);
		volumeChart.setHorizontalGridLinesVisible(false);
		volumeChart.setHorizontalZeroLineVisible(false);
		volumeChart.setVerticalGridLinesVisible(false);
		volumeChart.setVerticalZeroLineVisible(false);
		volumeChart.setPadding(new Insets(5, 4, 50, 42));
		return volumeChart;
	}
	
	private void display(List<Point> prices, Currency currency) {
		
		Point last = prices.get(prices.size() - 1);
		
		ObservableArrayList<Data<LocalDateTime,Number>> percents = new ObservableArrayList<>();
		prices.forEach((pt) -> percents.add(new Data<>(pt.date, (pt.price / last.price - 1) * 100)));
		
		dates.newRange(prices.get(0).date, last.date);
		
		display(percents, currency);
	}

	private void display(ObservableList<Data<LocalDateTime,Number>> data, Currency currency) {
		Series<LocalDateTime,Number> s = new Series<>();
		s.setData(data);
		s.setName(currency.symbol);
		series.add(s);
		normal(s);
	}
	
	private void displayVolume(Currency newValue, final PricesHistory pricesHistory) {
		var hist = pricesHistory.displayedCurrencies.get(newValue);
		ObservableArrayList<Data<LocalDateTime,Number>> volumeAvgData = new ObservableArrayList<>();
		double maxVolume = 0;
		double avg = 0;
		for (Point pt : hist) {
			avg = (avg * 2 + pt.volume) / 3;
			if (maxVolume < avg) {
				maxVolume = avg;
			}
			volumeAvgData.add(new Data<>(pt.date, - avg));
		}
		volume.clear();
		volume.add(new Series<LocalDateTime, Number>(volumeAvgData));
		NumberAxis yAxis = (NumberAxis) volume.get(0).getChart().getYAxis();
		yAxis.setUpperBound(0.0);
		yAxis.setLowerBound(- maxVolume * 5);
		yAxis.setTickUnit(maxVolume / 4);
	}

	private Series<LocalDateTime, Number> remove(Currency currency) {
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
	
	private Series<LocalDateTime,Number> seriesFor(Currency c) {
		for (Series<LocalDateTime, Number> s : series) {
			if (s.getName().equals(c.symbol)) {
				return s;
			}
		}
		throw new IndexOutOfBoundsException("Currency "+c+" not found in series.");
	}
	
	private void mark(Series<LocalDateTime, Number> thin, Series<LocalDateTime, Number> thick) {
		if (thin != null) {
			normal(thin);
		}
		
		thick(thick);
		series.remove(thick);
		series.add(thick);
	}
	
	private void normal(Series<LocalDateTime, Number> s) {
		if (s.getName().equals(WALLET.symbol)) {
			width(s, 4);
		} else {
			width(s, 1);
		}
	}
	
	private void thick(Series<LocalDateTime, Number> s) {
		width(s, 3);
	}
	
	private void width(Series<LocalDateTime, Number> s, int width) {
		s.nodeProperty().get().setStyle("-fx-stroke-width: "+width+"px;");
	}

}
