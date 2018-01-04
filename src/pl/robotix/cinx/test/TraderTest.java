package pl.robotix.cinx.test;

import static java.math.BigDecimal.valueOf;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import javafx.application.Application;
import javafx.stage.Stage;
import pl.robotix.cinx.Currency;
import pl.robotix.cinx.WalletCurrencies;
import pl.robotix.cinx.trade.Trader;

public class TraderTest extends Application {

	final CountDownLatch toolkitInitialized = new CountDownLatch(1);

	@Test
	public void findsPriceChain() throws InterruptedException {
		
		Thread jfx = new Thread(() -> {
			launch(); // initialize JavaFX toolkit
		});
		jfx.start();
		
		toolkitInitialized.await(3, TimeUnit.SECONDS);
		
		Map<Currency, BigDecimal> balance = new HashMap<>();
		balance.put(new Currency("A"), valueOf(100.0));
		balance.put(new Currency("B"), valueOf(100.0));
		balance.put(new Currency("C"), valueOf(100.0));
		balance.put(new Currency("D"), valueOf(100.0));
		balance.put(new Currency("E"), valueOf(100.0));
		WalletCurrencies wallet = new WalletCurrencies(balance);
//		wallet.setPercentChanges(7, -6, 5, -6, 0);
		
		new Trader().generateOperations(null, wallet);
//		expected output:
//		A<-->B: -6,00000000
//		A<-->D: -1,00000000
//		D<-->C: +5,00000000
	}
	
	public static void main(String[] args) throws InterruptedException {
		new TraderTest().findsPriceChain();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		 toolkitInitialized.countDown();
	}
	
}
