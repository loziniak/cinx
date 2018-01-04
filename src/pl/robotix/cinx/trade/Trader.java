package pl.robotix.cinx.trade;

import static java.lang.Math.abs;
import static java.util.Collections.reverseOrder;
import static pl.robotix.cinx.Currency.BTC;
import static pl.robotix.cinx.Currency.USDT;
import static pl.robotix.cinx.Pair.USDT_BTC;
import static pl.robotix.cinx.trade.Operation.Type.BUY;
import static pl.robotix.cinx.trade.Operation.Type.SELL;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Pair;
import pl.robotix.cinx.api.Api;
import pl.robotix.cinx.wallet.Wallet;

public class Trader {
	
	private static final String LOG_PERCENT_FORMAT = "%+.2f%%";
	private static final String LOG_USD_FORMAT = "%.2f USD";
	
	private static final double TAKER_FEE = 0.0025;
	
	private final Api api;
	private final Wallet wallet;
	
	private List<Operation> operations = new ArrayList<>();
	
	public Trader(Api api, Wallet wallet) {
		this.api = api;
		this.wallet = wallet;
	}
	
	public List<Operation> generateOperations() {
		operations.clear();

		Map<Currency, Double> changes = wallet.getPercentChanges();
		if (changes.isEmpty()) {
			return Collections.emptyList();
		}
		
		List<Currency> biggest = biggest(changes);
		correctDrift(changes, biggest.get(0));
		
		List<Currency> positive = new ArrayList<>();
		List<Currency> negative = new ArrayList<>();
		split(positive, negative, biggest, changes);
		
		while(!biggest.isEmpty()) {
			Currency processed = biggest.remove(0);
			double processedChange = changes.remove(processed);

			List<Currency> secondSource;
			if (processedChange < 0.0) {
				negative.remove(processed);
				secondSource = positive;
			} else if (processedChange > 0.0) {
				positive.remove(processed);
				secondSource = negative;
			} else {
				continue;
			}
			
			while (abs(processedChange) > 0.0) {
				if (secondSource.isEmpty()) {
					System.out.println("Percent drift left: " + String.format(LOG_PERCENT_FORMAT, processedChange));
					break;
				}
				Currency second = secondSource.get(0);
	
				double secondChange = changes.get(second);
				if (abs(processedChange) >= abs(secondChange)) {
					secondSource.remove(second);
					biggest.remove(second);
					changes.remove(second);
					
					System.out.println("" + processed + " --> " + second
							+ ": " + String.format(LOG_PERCENT_FORMAT, secondChange));
					
					operations.addAll(operationsFor(processed, second, percentToUsd(secondChange)));
					
					processedChange += secondChange;
				} else { // abs(processedChange) < abs(secondChange)
					addToChange(changes, second, processedChange);
					secondChange += processedChange;
					
					System.out.println("" + processed + " --> " + second
							+ ": " + String.format(LOG_PERCENT_FORMAT, - processedChange));
					
					operations.addAll(operationsFor(processed, second, percentToUsd(- processedChange)));
					
					processedChange = 0.0;
				}
			}
		}
		
		double[] usdOverallChangeHolder = {0.0};
		operations.forEach((op) -> {
			usdOverallChangeHolder[0] += op.amount.doubleValue() * api.getPrices().getUSDFor(op.pair.base).doubleValue();
		});
		System.out.println("Overall change: " + String.format(LOG_USD_FORMAT, usdOverallChangeHolder[0]));
		System.out.println("Overall fee: " + String.format(LOG_USD_FORMAT, 
				usdOverallChangeHolder[0] * TAKER_FEE));
		
		return operations;
	}
	
	private BigDecimal percentToUsd(double percent) {
		return new BigDecimal(percent * wallet.getWalletUSD() / 100.0);
	}
	
	private List<Operation> operationsFor(Currency from, Currency to, BigDecimal usdChange) {
		if (usdChange.compareTo(BigDecimal.ZERO) < 0) {
			Currency tmp = to;
			to = from;
			from = tmp;
			usdChange = usdChange.negate();
		}
		
		Operation[] ops;
		if (from.equals(USDT)) {
			if (to.equals(BTC)) {
				ops = new Operation[] {
						buy(          USDT_BTC, usdChange)};
			} else {
				ops = new Operation[] {
						buy(          USDT_BTC, usdChange),
						buy( new Pair(BTC, to), usdChange)};
			}
		} else if (from.equals(BTC)) {
			if (to.equals(USDT)) {
				ops = new Operation[] {
						sell(         USDT_BTC, usdChange)};
			} else {
				ops = new Operation[] {
						buy( new Pair(BTC, to), usdChange)};
			}
		} else {
			if (to.equals(BTC)) {
				ops = new Operation[] {
						sell(new Pair(BTC, from), usdChange)};
			} else if (to.equals(USDT)) {
				ops = new Operation[] {
						sell(new Pair(BTC, from), usdChange),
						sell(         USDT_BTC, usdChange)};
			} else {
				ops = new Operation[] {
						sell(new Pair(BTC, from), usdChange),
						buy( new Pair(BTC, to), usdChange)};
			}
		}
		
		return Arrays.asList(ops);
	}
	
	private Operation buy(Pair pair, BigDecimal usd) {
		Operation op = new Operation(BUY);
		op.pair = pair;
		op.amount = usd.divide(api.getPrices().getRate(new Pair(USDT, pair.base)), MathContext.DECIMAL64);
		op.rate = api.getPrices().getRate(pair);
		describe(op, usd);

		return op;
	}

	private Operation sell(Pair pair, BigDecimal usd) {
		Operation op = new Operation(SELL);
		op.pair = pair;
		op.amount = usd.divide(api.getPrices().getRate(new Pair(USDT, pair.base)), MathContext.DECIMAL64);
		op.rate = api.getPrices().getRate(pair);
		describe(op, usd);

		return op;
	}
	
	private void describe(Operation op, BigDecimal usd) {
		System.out.print(op);
		System.out.println(" (" + String.format(LOG_USD_FORMAT, usd) + ")");
	}
	
	private List<Currency> biggest(final Map<Currency, Double> changes) {
		List<Currency> all = new LinkedList<>(changes.keySet());
		all.sort(reverseOrder((Currency c1, Currency c2) -> Double.compare(
				abs(changes.get(c1)), abs(changes.get(c2)))));
		return all;
	}
	
	private void correctDrift(Map<Currency, Double> changes, final Currency currencyToCorrect) {
		double[] driftHolder = {0.0};
		changes.forEach((currency, change) -> {
			driftHolder[0] += change;
		});
		if (driftHolder[0] != 0.0) {
			addToChange(changes, currencyToCorrect, - driftHolder[0]);
			System.out.println("Percent drift corrected: " + String.format(LOG_PERCENT_FORMAT, driftHolder[0]));
		}
	}

	private void addToChange(Map<Currency, Double> changes, final Currency currencyToChange, final double amount) {
		changes.put(currencyToChange, changes.get(currencyToChange) + amount);
	}
	
	private void split(List<Currency> placeForPositive, List<Currency> placeForNegative,
			final List<Currency> source, final Map<Currency, Double> changes) {
		source.forEach((currency) -> {
			double change = changes.get(currency);

			if (change > 0.0) placeForPositive.add(currency);
			else if (change < 0.0) placeForNegative.add(currency);
		});
	}
	
	
	
	// execute operations with api

}
