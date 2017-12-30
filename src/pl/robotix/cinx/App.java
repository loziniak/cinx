package pl.robotix.cinx;

import java.util.Arrays;
import java.util.List;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pl.robotix.cinx.api.Api;
import pl.robotix.cinx.graph.Graph;

public class App extends Application {
	
	private static final String[] PAIRS = {
		"BTC_ETH",
		"BTC_LTC"
	}; 

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Cinx");
		
		Api poloniex = new Api();
		
		Graph graph = new Graph();
		Group root = new Group();
		root.getChildren().add(graph.getChart());
		primaryStage.setScene(new Scene(root));
		primaryStage.show();

		Arrays.asList(PAIRS).forEach((pair) -> {
			List<Point> prices = poloniex.getPriceHistory(pair, PriceRange.MONTH);
			graph.display(prices, pair);
		});
	}
	
	
	public static void main(String[] args) {
		launch(args);
	}
}
