package pl.robotix.cinx;

import static pl.robotix.cinx.Currency.BTC;
import static pl.robotix.cinx.Currency.USDT;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pl.robotix.cinx.api.AsyncApi;
import pl.robotix.cinx.api.AsyncThrottledCachedApi;
import pl.robotix.cinx.api.MultiApi;
import pl.robotix.cinx.api.SyncApiWrapper;
import pl.robotix.cinx.api.binance.BinanceApi;
import pl.robotix.cinx.api.bitmart.BitMartApi;
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
	private static final String CSS_FILE = "style.css";

	private static final String BINANCE_APIKEY_ENV = "BINANCE_APIKEY";
	private static final String BINANCE_SECRET_ENV = "BINANCE_SECRET";

	public static Prices prices;

	private AsyncApi api;
	private PricesHistory pricesHistory;
	private Wallet wallet;
	private Trader trader;
	private OperationLog operationLog;

	private ObservableSet<Currency> chartCurrencies = FXCollections.observableSet();
	private ObjectProperty<Currency> highlightCurrency = new SimpleObjectProperty<>();
	private ObjectProperty<Map<Currency, Double>> walletCurrencies = new SimpleObjectProperty<>(null);
	
	private Logger log = new Logger();

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Cinx");
		
		String binanceApiKey = System.getenv(BINANCE_APIKEY_ENV);
		String binanceSecret = System.getenv(BINANCE_SECRET_ENV);
		
		operationLog = new OperationLog(LOG_FILE);

		var binanceApi = new AsyncThrottledCachedApi(new BinanceApi(binanceApiKey, binanceSecret), 2000, 2, 100);
		var multi = new MultiApi(binanceApi);
//		multi.addApi(new AsyncThrottledCachedApi(new BitMartApi(), 2000, 2, 200));
		multi.addApi(new SyncApiWrapper(new BitMartApi()));
		api = multi;
//		api = new SyncApiWrapper(new BinanceApi(binanceApiKey, binanceSecret));
		api.initTimeRanges();
		
		pricesHistory = new PricesHistory(api, chartCurrencies, walletCurrencies);

		prices = new Prices(api);
		wallet = new Wallet(balanceWithBTCAndUSDT(), chartCurrencies, prices, walletCurrencies); // retrieve balance
		trader = new Trader(prices, wallet, log, operationLog);

		layout(primaryStage);

		Config config = new Config(CONFIG_FILE);
		var subscribed = config.getSubscribedCurrencies();
		subscribed.removeIf((c) -> !api.isExchangeable(c));
		chartCurrencies.addAll(subscribed);
		chartCurrencies.addAll(random(config)); // retrieve api btc

		chartCurrencies.addAll(wallet.getCurrencies());
		wallet.updateWalletCurrencies();

		prices.retrieveFor(chartCurrencies); // retrieve chart (wallet, subscribed, random)
	}
	
	@Override
	public void stop() throws Exception {
		api.close();
		operationLog.close();
	}
	
	private Set<Currency> random(Config config) {
		int count = config.getRandomCurrenciesCount();
		double fromFirst = 0.97;
		
		var banned = new HashSet<Currency>(config.getBannedCurrencies());
		var curs = api.pairsForMarket(BTC); // TODO: trade with USDT, not BTC. bigger volumes.
		prices.retrieveFor(curs.stream().map(p -> p.base).collect(Collectors.toSet()));
		
		Set<Currency> currencySet = prices.getAllCurrencies();
		currencySet.removeAll(chartCurrencies);
		List<Currency> currencyList = new ArrayList<>(currencySet);
		
		Set<Currency> random = new HashSet<>();
		if (currencyList.size() > count) {
			currencyList.sort(prices.byVolume());
			while (random.size() < count) {
				double position = Math.random();
				position = position * position * position; // make currencies on top of list to appear more frequently
				position = 1.0 - fromFirst + (fromFirst * position);
				Currency c = currencyList.get(
						Double.valueOf(position * currencyList.size()).intValue()
					);
				if (!banned.contains(c)) {
					random.add(c);
				}
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

		var scene = new Scene(tabs);
		scene.getStylesheets().add(AppStarter.class.getResource(CSS_FILE).toExternalForm());
		stage.setScene(scene);
		stage.show();
	}

	private Pane analyzeLayout(Tab trade) {
		WalletUI walletUI = new WalletUI(wallet, chartCurrencies, highlightCurrency);
		walletUI.setPadding(new Insets(20));
		walletUI.setSpacing(10);
		
		VBox bottomRight = new VBox(20);
		
		Button generateOperations = new Button("Generate Operations");
		generateOperations.setOnAction((event) -> {
			trade.getTabPane().getSelectionModel().select(trade);
			trader.generateOperations(api.takerFee());
		});
		
		var selector = new CurrencySelector(wallet, chartCurrencies, highlightCurrency);
		ToggleButton sortBySymbol = new ToggleButton("Sort By Symbol");
		sortBySymbol.setOnAction(event -> {
			if (((ToggleButton)event.getSource()).isSelected()) {
				selector.sortByVolume();
			} else {
				selector.sortBySymbol();
			}
		});
		bottomRight.getChildren().add(generateOperations);
		bottomRight.getChildren().add(sortBySymbol);
		bottomRight.getChildren().add(selector);

		HBox bottom = new HBox();
		bottom.getChildren().add(new Graph(pricesHistory, highlightCurrency, operationLog));
		bottom.getChildren().add(bottomRight);
		HBox.setHgrow(bottomRight, Priority.ALWAYS);

		VBox outer = new VBox();
		outer.getChildren().add(new ScrollPane(walletUI));
		outer.getChildren().add(bottom);
		outer.setPadding(new Insets(10));
		return outer;
	}
	
	
	private Map<Currency, BigDecimal> balanceWithBTCAndUSDT() {
		Map<Currency, BigDecimal> balance = api.retrieveBalance();
		
		final BigDecimal MIN_SHOW = BigDecimal.valueOf(5); 
		balance.entrySet().removeIf((e) -> prices.getUSDFor(e.getKey()).multiply(e.getValue()).compareTo(MIN_SHOW) < 0);

		if (!balance.containsKey(USDT)) {
			balance.put(USDT, BigDecimal.ZERO);
		}
		if (!balance.containsKey(BTC)) {
			balance.put(BTC, BigDecimal.ZERO);
		}
		return balance;
	}
	
	
	public static LocalDateTime fromEpochSeconds(long epochSeconds) {
		return LocalDateTime.ofEpochSecond(epochSeconds, 0, ZoneOffset.UTC);
	}

	public static void main(String[] args) {
		launch(args);
	}

}
