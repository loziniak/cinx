package pl.robotix.cinx;

import static pl.robotix.cinx.Currency.BTC;
import static pl.robotix.cinx.Currency.USDT;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pl.robotix.cinx.api.AsyncApi;
import pl.robotix.cinx.api.SyncApiWrapper;
import pl.robotix.cinx.api.binance.BinanceApi;
import pl.robotix.cinx.graph.Graph;
import pl.robotix.cinx.graph.PricesHistory;
import pl.robotix.cinx.log.OperationLog;
import pl.robotix.cinx.trade.TradeUI;
import pl.robotix.cinx.trade.Trader;
import pl.robotix.cinx.wallet.Wallet;
import pl.robotix.cinx.wallet.WalletUI;

public class App extends Application {
	
	private static final String CONFIG_FILE = "./app.properties";
	private static final String LOG_FILE = "./operation.log";

//	private static final String POLONIEX_APIKEY_ENV = "POLONIEX_APIKEY";
//	private static final String POLONIEX_SECRET_ENV = "POLONIEX_SECRET";
	private static final String BINANCE_APIKEY_ENV = "BINANCE_APIKEY";
	private static final String BINANCE_SECRET_ENV = "BINANCE_SECRET";

	public static Prices prices;

	private AsyncApi api;
	private PricesHistory pricesHistory;
	private Wallet wallet;
	private Trader trader;
	private OperationLog operationLog;

	private ObservableSet<Currency> chartCurrencies = FXCollections.observableSet();
	private ObjectProperty<Currency> highlihtCurrency = new SimpleObjectProperty<>();
	
	private Logger log = new Logger();

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Cinx");
		
//		String poloniexApiKey = System.getenv(POLONIEX_APIKEY_ENV);
//		String poloniexSecret = System.getenv(POLONIEX_SECRET_ENV);
		String binanceApiKey = System.getenv(BINANCE_APIKEY_ENV);
		String binanceSecret = System.getenv(BINANCE_SECRET_ENV);
		
		operationLog = new OperationLog(LOG_FILE);

//		api = new AsyncThrottledCachedApi(new BinanceApi(binanceApiKey, binanceSecret), 2000, 2);
		api = new SyncApiWrapper(new BinanceApi(binanceApiKey, binanceSecret));
		api.initTimeRanges();
		pricesHistory = new PricesHistory(api, chartCurrencies);

		prices = new Prices(api);
		wallet = new Wallet(balanceWithBTCAndUSDT(), chartCurrencies, prices); // retrieve balance
		trader = new Trader(prices, wallet, log, operationLog);

		layout(primaryStage);

		chartCurrencies.addAll(wallet.getCurrencies());

		Config config = new Config(CONFIG_FILE);
		var subscribed = config.getSubscribedCurrencies();
		subscribed.removeIf((c) -> !api.isExchangeable(c));
		chartCurrencies.addAll(subscribed);

		chartCurrencies.addAll(random(config.getRandomCurrenciesCount())); // retrieve api btc
		
		prices.retrieveFor(chartCurrencies); // retrieve chart (wallet, subscribed, random)
	}
	
	@Override
	public void stop() throws Exception {
		api.close();
		operationLog.close();
	}
	
	private Set<Currency> random(int count) {
		double fromFirst = 0.75;
		
		var curs = api.pairsForMarket(BTC); // TODO: trade with USDT, not BTC. bigger volumes.
		prices.retrieveFor(curs);
		
		Set<Currency> currencySet = prices.getAllCurrencies();
		currencySet.removeAll(chartCurrencies);
		List<Currency> currencyList = new ArrayList<>(currencySet);
		
		Set<Currency> random = new HashSet<>();
		if (currencyList.size() > count) {
			currencyList.sort(prices.byVolume());
			while (random.size() < count) {
				random.add(currencyList.get(
						Double.valueOf(Math.random() * currencyList.size() * fromFirst).intValue()
					));
			}
		}
		return random;
	}
	
	private void layout(Stage stage) {
		Tab trade = new Tab("Trade");
		TradeUI tradePane = new TradeUI(log, trader, api);
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
		top.getChildren().add(new Graph(pricesHistory, highlihtCurrency, operationLog));
		
		WalletUI walletUI = new WalletUI(wallet, highlihtCurrency);
		walletUI.setPadding(new Insets(20));
		walletUI.setSpacing(10);
		
		VBox topRight = new VBox(20);
		topRight.getChildren().add(walletUI);
		
		Button generateOperations = new Button("Generate operations");
		generateOperations.setOnAction((event) -> {
			trade.getTabPane().getSelectionModel().select(trade);
			prices = api.retrievePrices();
			trader.setPrices(prices);
			trader.generateOperations();
		});
		topRight.getChildren().add(generateOperations);
		top.getChildren().add(topRight);

		VBox outer = new VBox();
		outer.getChildren().add(top);
		outer.getChildren().add(new CurrencySelector(wallet, chartCurrencies));
		outer.setPadding(new Insets(10));
		return outer;
	}
	
	
	private Map<Currency, BigDecimal> balanceWithBTCAndUSDT() {
		Map<Currency, BigDecimal> balance = api.retrieveBalance();
		
		balance.keySet().removeIf((k) -> prices.getUSDFor(k).multiply(balance.get(k)).compareTo(BigDecimal.ONE) < 0);

		if (!balance.containsKey(USDT)) {
			balance.put(USDT, BigDecimal.ZERO);
		}
		if (!balance.containsKey(BTC)) {
			balance.put(BTC, BigDecimal.ZERO);
		}
		return balance;
	}
	
	
	public static void main(String[] args) {
		launch(args);
	}

}
