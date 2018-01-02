package pl.robotix.cinx;

import static pl.robotix.cinx.PriceRange.MONTH;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pl.robotix.cinx.api.Api;
import pl.robotix.cinx.graph.Graph;

public class App extends Application {
	
	public static final Instant NOW = Instant.now();
	
	private static final String CONFIG_FILE = "./app.properties";

	private static final String POLONIEX_APIKEY_ENV = "POLONIEX_APIKEY";
	private static final String POLONIEX_SECRET_ENV = "POLONIEX_SECRET";

	public static final Currency USDT = new Currency("USDT");
	public static final Currency BTC = new Currency("BTC");
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Cinx");
		
		String poloniexApiKey = System.getenv(POLONIEX_APIKEY_ENV);
		String poloniexSecret = System.getenv(POLONIEX_SECRET_ENV);
		Api poloniex = new Api(poloniexApiKey, poloniexSecret);
		
		Map<Pair, BigDecimal> prices = poloniex.getPrices();
		if (prices != null) {
			poloniex.setPrices(new Prices(prices));
		}
		
		Graph graph = new Graph();
		Group root = new Group();
		root.getChildren().add(graph.getChart());
		primaryStage.setScene(new Scene(root));
		primaryStage.show();

		Config config = new Config(CONFIG_FILE);
		Map<Currency, BigDecimal> balance = balanceWithBTCAndUSDT(poloniex);
		
		balance.keySet().forEach((currency) -> {
//		config.getSubscribedCurrencies().forEach((currency) -> {
			List<Point> priceHistory = poloniex.getUSDPriceHistory(currency, MONTH);
			graph.display(priceHistory, currency.symbol);
		});
	}

	private Map<Currency, BigDecimal> balanceWithBTCAndUSDT(Api poloniex) {
		Map<Currency, BigDecimal> balance = poloniex.getUSDBalance();
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
