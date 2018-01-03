package pl.robotix.cinx;

import static pl.robotix.cinx.PriceRange.MONTH;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pl.robotix.cinx.api.Api;
import pl.robotix.cinx.graph.Graph;

public class App extends Application {
	
	public static final Instant NOW = Instant.now();
	
	public static final Currency USDT = new Currency("USDT");
	public static final Currency BTC = new Currency("BTC");
	
	private static final String CONFIG_FILE = "./app.properties";

	private static final String POLONIEX_APIKEY_ENV = "POLONIEX_APIKEY";
	private static final String POLONIEX_SECRET_ENV = "POLONIEX_SECRET";

	public Set<Currency> currencies = new HashSet<>();

	private Api api;
	private Graph graph = new Graph();
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Cinx");
		
		String poloniexApiKey = System.getenv(POLONIEX_APIKEY_ENV);
		String poloniexSecret = System.getenv(POLONIEX_SECRET_ENV);
		api = new Api(poloniexApiKey, poloniexSecret);
		
		Config config = new Config(CONFIG_FILE);
		Map<Currency, BigDecimal> balance = balanceWithBTCAndUSDT();

		currencies.addAll(balance.keySet());
		currencies.addAll(config.getSubscribedCurrencies());
		
		layoutUi(primaryStage);

		currencies.forEach((currency) -> display(currency));
	}
	
	private void layoutUi(Stage stage) {
//		Group root = new Group();
//		root.getChildren().add(graph.getChart());
		
		VBox outer = new VBox();
		HBox top = new HBox();
		outer.getChildren().add(top);
		FlowPane currencies = new FlowPane();
		currencies.setVgap(5);
		currencies.setHgap(5);
		outer.getChildren().add(currencies);
		
		List<Currency> currencyList = new ArrayList<>(api.getPrices().getAllCurrencies());
		currencyList.sort(api.getPrices().byVolume());
		for (Currency c: currencyList) {
			currencies.getChildren().add(currencyButton(c));
		}
		
		top.getChildren().add(graph.getChart());
		
		stage.setScene(new Scene(outer));
		stage.show();		
	}
	
	private Node currencyButton(Currency currency) {
		ToggleButton button = new ToggleButton(currency.symbol);
		if (currencies.contains(currency)) {
			button.setSelected(true);
		}
		button.selectedProperty().addListener((selected) -> {
			if (((BooleanProperty) selected).get()) {
				currencies.add(currency);
				display(currency);
			} else {
				currencies.remove(currency);
				graph.remove(currency);
			}
		});
		return button;
	}

	private void display(Currency currency) {
		List<Point> priceHistory = api.retrieveUSDPriceHistory(currency, MONTH);
		graph.display(priceHistory, currency);
	}

	private Map<Currency, BigDecimal> balanceWithBTCAndUSDT() {
		Map<Currency, BigDecimal> balance = api.retrieveUSDBalance();
		if (!balance.containsKey(USDT)) {
			balance.put(USDT, BigDecimal.valueOf(0.0));
		}
		if (!balance.containsKey(BTC)) {
			balance.put(BTC, BigDecimal.valueOf(0.0));
		}
		return balance;
	}
	
	
	public static void main(String[] args) {
		launch(args);
	}
}
