package pl.robotix.cinx.test;

import static java.math.BigDecimal.valueOf;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Logger;
import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Prices;
import pl.robotix.cinx.log.OperationLog;
import pl.robotix.cinx.trade.Trader;
import pl.robotix.cinx.wallet.Wallet;

public class TraderTest {

	@Test
	public void findsPriceChain() throws InterruptedException, IOException {
		
		Logger log = new Logger((message) -> System.out.println(message));
		
		Map<Pair, BigDecimal> pairVolumes = new HashMap<>();
		pairVolumes.put(new Pair("USDT", "A"), valueOf(10.0));
		pairVolumes.put(new Pair("USDT", "B"), valueOf(10.0));
		pairVolumes.put(new Pair("USDT", "C"), valueOf(10.0));
		pairVolumes.put(new Pair("USDT", "D"), valueOf(10.0));
		pairVolumes.put(new Pair("USDT", "E"), valueOf(10.0));
		
		Map<Pair, BigDecimal> pricesData = new HashMap<>();
		pricesData.put(new Pair("USDT", "A"), valueOf(1.0));
		pricesData.put(new Pair("USDT", "B"), valueOf(1.0));
		pricesData.put(new Pair("USDT", "C"), valueOf(1.0));
		pricesData.put(new Pair("USDT", "D"), valueOf(1.0));
		pricesData.put(new Pair("USDT", "E"), valueOf(1.0));
		pricesData.put(new Pair("BTC", "A"), valueOf(2.0));
		pricesData.put(new Pair("BTC", "B"), valueOf(2.0));
		pricesData.put(new Pair("BTC", "C"), valueOf(2.0));
		pricesData.put(new Pair("BTC", "D"), valueOf(2.0));
		pricesData.put(new Pair("BTC", "E"), valueOf(2.0));
		
		Prices prices = new Prices(pricesData, pairVolumes);
		
		Map<Currency, BigDecimal> balance = new HashMap<>();
		balance.put(new Currency("A"), valueOf(100.0));
		balance.put(new Currency("B"), valueOf(100.0));
		balance.put(new Currency("C"), valueOf(100.0));
		balance.put(new Currency("D"), valueOf(100.0));
		balance.put(new Currency("E"), valueOf(100.0));
		Wallet wallet = new Wallet(balance, FXCollections.observableSet(), prices, new SimpleObjectProperty<>());
		
		System.out.println(wallet.getPercentChanges());
		
		try {
			Method m = Wallet.class.getDeclaredMethod("setPercentChanges", double[].class);
			m.setAccessible(true);
			m.invoke(wallet, new double[] {17, -6, 14.99, -6, -19.99} );
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		System.out.println(wallet.getPercentChanges());
		
		new Trader(prices, wallet, log, new OperationLog("/dev/null")).generateOperations(0.0025);
//		expected output:
//		
//		A --> B: -6,00%
//		BTC_B: SELL 30,00000000 at rate 2,00000000 (30,00 USD)
//		BTC_A: BUY 29,70000000 at rate 2,00000000 (29,70 USD)
//		A --> D: -1,00%
//		BTC_D: SELL 5,00000000 at rate 2,00000000 (5,00 USD)
//		BTC_A: BUY 4,95000000 at rate 2,00000000 (4,95 USD)
//		D --> C: +5,00%
//		BTC_D: SELL 25,00000000 at rate 2,00000000 (25,00 USD)
//		BTC_C: BUY 24,75000000 at rate 2,00000000 (24,75 USD)
//		Overall change: 119,40 USD
//		Overall fee: 0,30 USD
		
	}
	
}
