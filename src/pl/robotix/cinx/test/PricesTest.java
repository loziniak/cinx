package pl.robotix.cinx.test;

import static java.math.BigDecimal.valueOf;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Prices;

public class PricesTest {
	
	public static final Pair USDT_ETH = new Pair("USDT_ETH");
	public static final Pair ETH_LTC = new Pair("ETH_LTC");

	public static final Currency LTC = new Currency("LTC");
	public static final Currency USDT = new Currency("USDT");
	
	@Test
	public void findsPriceChain() {
		
		Map<Pair, BigDecimal> prices = new HashMap<>();
		prices.put(USDT_ETH, valueOf(1.2));
		prices.put(ETH_LTC, valueOf(1.1));
		
		Map<Pair, BigDecimal> volumes = new HashMap<>();
		volumes.put(USDT_ETH, valueOf(100));
		volumes.put(ETH_LTC, valueOf(0.200));
		
		Assert.assertEquals(valueOf(1.32),
				new Prices(prices, volumes).getUSDFor(LTC));
		
		Assert.assertEquals(Arrays.asList(ETH_LTC, USDT_ETH),
				new Prices(prices, volumes).pairsToComputeUSDFor(LTC));

		Assert.assertEquals(Arrays.asList(new Pair[] {}),
				new Prices(prices, volumes).pairsToComputeUSDFor(USDT));

	}

}
