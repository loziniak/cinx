package pl.robotix.cinx;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
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


	private Api api;
	private Graph graph = new Graph();
	private WalletCurrencies wallet;
	private CurrencySelector currencies = new CurrencySelector();
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Cinx");
		
		String poloniexApiKey = System.getenv(POLONIEX_APIKEY_ENV);
		String poloniexSecret = System.getenv(POLONIEX_SECRET_ENV);
		api = new Api(poloniexApiKey, poloniexSecret);
		
		wallet = new WalletCurrencies(balanceWithBTCAndUSDT());

		layout(primaryStage);

		currencies.addDisplayListener(api, graph);

		currencies.addAll(wallet.getCurrencies());
		Config config = new Config(CONFIG_FILE);
		currencies.addAll(config.getSubscribedCurrencies());
	}
	
	private void layout(Stage stage) {
		Tab trade = new Tab("Trade");
		trade.setClosable(false);
		trade.setContent(new Text("TRADE"));
		
		Tab analyze = new Tab("Analzye");
		analyze.setClosable(false);
		analyze.setContent(analyzeLayout(trade));

		TabPane tabs = new TabPane();
		tabs.getTabs().addAll(analyze, trade);

		stage.setScene(new Scene(tabs));
		stage.show();
	}

	private VBox analyzeLayout(Tab trade) {
		HBox top = new HBox();
		top.getChildren().add(graph.getChart());
		
		HBox sliders = new HBox();
		sliders.setPadding(new Insets(20));
		sliders.setSpacing(10);
		wallet.setSlidersPane(sliders);

		VBox topRight = new VBox(20);
		topRight.getChildren().add(sliders);
		
		Button generateOperations = new Button("Generate operations");
		generateOperations.setOnAction((event) -> {
			trade.getTabPane().getSelectionModel().select(trade);
		});
		topRight.getChildren().add(generateOperations);
		top.getChildren().add(topRight);

		VBox outer = new VBox();
		outer.getChildren().add(top);
		outer.getChildren().add(currencies.buttons(api, wallet));
		outer.setPadding(new Insets(10));
		return outer;
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
