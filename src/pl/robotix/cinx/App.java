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

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Cinx");
		
		Api poloniex = new Api();
		
		Config config = new Config(CONFIG_FILE);
		Map<Pair, BigDecimal> prices = poloniex.getPrices();
		if (prices != null) {
			config.setPrices(prices);
			poloniex.setPrices(new Prices(prices));
		}
		config.saveToDisk();
		
		Graph graph = new Graph();
		Group root = new Group();
		root.getChildren().add(graph.getChart());
		primaryStage.setScene(new Scene(root));
		primaryStage.show();

		config.getSubscribedCurrencies().forEach((currency) -> {
			List<Point> priceHistory = poloniex.getUSDPriceHistory(currency, MONTH);
			graph.display(priceHistory, currency.symbol);
		});
	}
	
	
	public static void main(String[] args) {
		launch(args);
	}
}
