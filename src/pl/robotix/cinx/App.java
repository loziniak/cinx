package pl.robotix.cinx;

import static pl.robotix.cinx.TimeRange.MONTH;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener.Change;
import javafx.geometry.Insets;
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

	public ObservableSet<Currency> chartCurrencies = FXCollections.observableSet();

	private Api api;
	private Graph graph = new Graph();
	private WalletCurrencies wallet;
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Cinx");
		
		String poloniexApiKey = System.getenv(POLONIEX_APIKEY_ENV);
		String poloniexSecret = System.getenv(POLONIEX_SECRET_ENV);
		api = new Api(poloniexApiKey, poloniexSecret);
		
		wallet = new WalletCurrencies(balanceWithBTCAndUSDT());

		layoutUi(primaryStage);
		
		chartCurrencies.addListener((Change<? extends Currency> change) -> {
			if (change.wasAdded()) {
				Currency added = change.getElementAdded();
				if (!added.equals(USDT)) {
					List<Point> priceHistory = api.retrieveUSDPriceHistory(added, MONTH);
					graph.display(priceHistory, added);
				}
			}
			if (change.wasRemoved()) {
				graph.remove(change.getElementRemoved());
			}
		});

		chartCurrencies.addAll(wallet.getCurrencies());
		Config config = new Config(CONFIG_FILE);
		chartCurrencies.addAll(config.getSubscribedCurrencies());
	}
	
	private void layoutUi(Stage stage) {
		HBox top = new HBox();
		top.getChildren().add(graph.getChart());
		
		HBox sliders = new HBox();
		sliders.setPadding(new Insets(20));
		sliders.setSpacing(10);
		wallet.setSlidersPane(sliders);
		top.getChildren().add(sliders);

		VBox outer = new VBox();
		outer.getChildren().add(top);
		outer.getChildren().add(currencyButtons());
		outer.setPadding(new Insets(10));
		
		stage.setScene(new Scene(outer));
		stage.show();		
	}
	
	

	private FlowPane currencyButtons() {
		FlowPane currencies = new FlowPane();
		currencies.setVgap(5);
		currencies.setHgap(5);
		
		List<Currency> currencyList = new ArrayList<>(api.getPrices().getAllCurrencies());
		currencyList.sort(api.getPrices().byVolume());
		for (Currency c: currencyList) {
			currencies.getChildren().add(currencyButton(c));
		}
		return currencies;
	}
	
	private ToggleButton currencyButton(final Currency currency) {
		final ToggleButton button = new ToggleButton(currency.symbol);
		button.selectedProperty().addListener((selected) -> {
			if (((BooleanProperty) selected).get()) {
				chartCurrencies.add(currency);
			} else {
				chartCurrencies.remove(currency);
			}
		});
		chartCurrencies.addListener((Change<? extends Currency> change) -> {
			Currency added = change.getElementAdded();
			Currency removed = change.getElementRemoved();

			if (change.wasAdded() && added.equals(currency)) {
				if (!button.isSelected()) {
					button.setSelected(true);
				}
				wallet.add(currency);
			}
			if (change.wasRemoved() && removed.equals(currency)) {
				if (button.isSelected()) {
					button.setSelected(false);
				}
				wallet.remove(currency);
			}
		});
		return button;
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
