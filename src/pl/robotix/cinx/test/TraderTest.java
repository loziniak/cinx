package pl.robotix.cinx.test;

import static java.math.BigDecimal.valueOf;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import pl.robotix.cinx.Currency;
import pl.robotix.cinx.trade.Trader;
import pl.robotix.cinx.wallet.Wallet;

public class TraderTest {

	@Test
	public void findsPriceChain() throws InterruptedException {
		
		Map<Currency, BigDecimal> balance = new HashMap<>();
		balance.put(new Currency("A"), valueOf(100.0));
		balance.put(new Currency("B"), valueOf(100.0));
		balance.put(new Currency("C"), valueOf(100.0));
		balance.put(new Currency("D"), valueOf(100.0));
		balance.put(new Currency("E"), valueOf(100.0));
		Wallet wallet = new Wallet(balance);
		
		System.out.println(wallet.getPercentChanges());
		
		try {
			Method m = Wallet.class.getDeclaredMethod("setPercentChanges", double[].class);
			m.setAccessible(true);
			m.invoke(wallet, new double[] {7, -6, 5, -6, 0} );
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		System.out.println(wallet.getPercentChanges());
		
		new Trader(null, wallet).generateOperations();
//		expected output:
//		A<-->B: -6,00000000
//		A<-->D: -1,00000000
//		D<-->C: +5,00000000
	}
	
}
