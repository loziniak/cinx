package pl.robotix.cinx;

import static pl.robotix.cinx.Currency.BTC;
import static pl.robotix.cinx.Currency.USDT;

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
import javafx.stage.Stage;
import pl.robotix.cinx.api.Api;
import pl.robotix.cinx.graph.Graph;
import pl.robotix.cinx.trade.TradeUI;
import pl.robotix.cinx.trade.Trader;
import pl.robotix.cinx.wallet.Wallet;
import pl.robotix.cinx.wallet.WalletUI;

public class App extends Application {
	
	public static final Instant NOW = Instant.now();
	
	private static final String CONFIG_FILE = "./app.properties";

	private static final String POLONIEX_APIKEY_ENV = "POLONIEX_APIKEY";
	private static final String POLONIEX_SECRET_ENV = "POLONIEX_SECRET";


	private Api api;
	private Graph graph = new Graph();
	private Wallet wallet;
	private Trader trader;
	
	private CurrencySelector currencies = new CurrencySelector();
	private Logger log = new Logger();

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Cinx");
		
		String poloniexApiKey = System.getenv(POLONIEX_APIKEY_ENV);
		String poloniexSecret = System.getenv(POLONIEX_SECRET_ENV);

		api = new Api(poloniexApiKey, poloniexSecret);
		wallet = new Wallet(balanceWithBTCAndUSDT());
		trader = new Trader(api, wallet, log);

		layout(primaryStage);

		currencies.addDisplayListener(api, graph);

		currencies.addAll(wallet.getCurrencies());
		Config config = new Config(CONFIG_FILE);
		currencies.addAll(config.getSubscribedCurrencies());
		
	}
	
	private void layout(Stage stage) {
		Tab trade = new Tab("Trade");
		TradeUI tradePane = new TradeUI(log, trader);
		trade.setClosable(false);
		trade.setContent(tradePane);
		
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
		
		WalletUI walletUI = new WalletUI(wallet);
		walletUI.setPadding(new Insets(20));
		walletUI.setSpacing(10);
		
		VBox topRight = new VBox(20);
		topRight.getChildren().add(walletUI);
		
		Button generateOperations = new Button("Generate operations");
		generateOperations.setOnAction((event) -> {
			trade.getTabPane().getSelectionModel().select(trade);
			api.refreshPrices();
			trader.generateOperations();
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
//		Map<Currency, BigDecimal> balance = api.retrieveUSDBalanceMock();
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
