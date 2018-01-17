package pl.robotix.cinx.test;

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
	public static final Pair USDT_BTC = new Pair("USDT_BTC");
	public static final Pair BTC_LTC = new Pair("BTC_LTC");

	public static final Currency LTC = new Currency("LTC");
	public static final Currency USDT = new Currency("USDT");
	public static final Currency BTC = new Currency("BTC");
	
	@Test
	public void findsPriceChain() {
		
		Map<Pair, BigDecimal> prices = new HashMap<>();
		prices.put(USDT_ETH, new BigDecimal("1.1"));
		prices.put(ETH_LTC, new BigDecimal("1.2"));
		prices.put(USDT_BTC, new BigDecimal("1.32"));
		prices.put(BTC_LTC, new BigDecimal("1.0"));
		
		Map<Pair, BigDecimal> volumes = new HashMap<>();
		volumes.put(USDT_ETH, new BigDecimal("100"));
		volumes.put(ETH_LTC, new BigDecimal("0.200"));
		volumes.put(USDT_BTC, new BigDecimal("1.0"));
		volumes.put(BTC_LTC, new BigDecimal("10.0"));
		
		Assert.assertEquals(null, new BigDecimal("1.32").doubleValue(),
				new Prices(prices, volumes).getUSDFor(LTC).doubleValue(), 0.0001);
		
//		Assert.assertEquals(Arrays.asList(ETH_LTC, USDT_ETH),
//				new Prices(prices, volumes).pairsToComputeUSDFor(LTC));
		Assert.assertEquals(Arrays.asList(USDT_BTC, BTC_LTC),
				new Prices(prices, volumes).pairsToComputeUSDFor(LTC));

		Assert.assertEquals(Arrays.asList(new Pair[] {}),
				new Prices(prices, volumes).pairsToComputeUSDFor(USDT));

		Assert.assertEquals(Arrays.asList(new Pair[] {}),
				new Prices(prices, volumes).pairsToComputeBTCFor(BTC));

		Assert.assertEquals(Arrays.asList(USDT_BTC.reverse()),
				new Prices(prices, volumes).pairsToComputeBTCFor(USDT));

		Assert.assertEquals(Arrays.asList(BTC_LTC),
				new Prices(prices, volumes).pairsToComputeBTCFor(LTC));

	}

}
