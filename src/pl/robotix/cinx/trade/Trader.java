package pl.robotix.cinx.trade;

import static java.lang.Math.abs;
import static java.util.Collections.reverseOrder;
import static pl.robotix.cinx.Currency.BTC;
import static pl.robotix.cinx.Currency.USDT;
import static pl.robotix.cinx.Pair.USDT_BTC;
import static pl.robotix.cinx.trade.Operation.Type.BUY;
import static pl.robotix.cinx.trade.Operation.Type.SELL;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Logger;
import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Prices;
import pl.robotix.cinx.api.AsyncThrottledCachedApi;
import pl.robotix.cinx.log.OperationLog;
import pl.robotix.cinx.wallet.Wallet;

public class Trader {
	
	private static final String LOG_PERCENT_FORMAT = "%+.2f%%";
	private static final String LOG_USD_FORMAT = "%.2f USD";
	
	private static final BigDecimal BUY_RATE_MOD = BigDecimal.valueOf(11, 1); // 1.1
	private static final BigDecimal SELL_RATE_MOD = BigDecimal.valueOf(9, 1); // 0.9
	private static final BigDecimal TRANSITION_RATE_MOD = BigDecimal.valueOf(99, 2); // 0.99
	
	private static final double TAKER_FEE = 0.0025;
	
	private Prices prices;
	private final Wallet wallet;
	private final Logger log;
	
	private OperationLog operationLog;
	
	private List<Operation> operations = new ArrayList<>();
	
	public Trader(Prices prices, Wallet wallet, Logger log, String operationLogFile) {
		this.prices = prices;
		this.wallet = wallet;
		this.log = log;
		try {
			this.operationLog = new OperationLog(operationLogFile);
		} catch (IOException e) {
			System.err.println("Could not open operation log: "+e.getMessage());
		}
	}
	
	public void close() {
		operationLog.close();
	}
	
	public List<Operation> generateOperations() {
		operations.clear();

		Map<Currency, Double> changes = wallet.getPercentChanges();
		
		List<Currency> toProcess = altcoinClearingsFirst(changes);
		correctDrift(changes);

		List<Currency> positive = new ArrayList<>();
		List<Currency> negative = new ArrayList<>();
		split(positive, negative, toProcess, changes);
		
		while(!toProcess.isEmpty()) {
			Currency processed = toProcess.remove(0);
			double processedChange = changes.get(processed);
			boolean clearedToBTC = false;
			if (isAltcoinClearing(processed, processedChange)) {
				operations.add(sellAll(new Pair(BTC, processed)));
				clearedToBTC = true;
			}
			changes.remove(processed);

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
					toProcess.remove(second);
					changes.remove(second);
					
					log.info("" + processed + " --> " + second
							+ ": " + String.format(LOG_PERCENT_FORMAT, secondChange));
					
					operations.addAll(operationsFor(processed, second, percentToUsd(secondChange), clearedToBTC));
					
					processedChange += secondChange;
				} else { // abs(processedChange) < abs(secondChange)
					addToChange(changes, second, processedChange);
					secondChange += processedChange;
					
					log.info("" + processed + " --> " + second
							+ ": " + String.format(LOG_PERCENT_FORMAT, - processedChange));
					
					operations.addAll(operationsFor(processed, second, percentToUsd(- processedChange), clearedToBTC));
					
					processedChange = 0.0;
				}
			}
		}
		
		double[] usdOverallChangeHolder = {0.0};
		operations.forEach((op) -> {
			if (op == null) {
				return;
			}
			usdOverallChangeHolder[0] += op.amount.doubleValue() * prices.getUSDFor(op.pair.base).doubleValue();
		});
		log.info("Overall change: " + String.format(LOG_USD_FORMAT, usdOverallChangeHolder[0]));
		log.info("Overall fee: " + String.format(LOG_USD_FORMAT, 
				usdOverallChangeHolder[0] * TAKER_FEE));
		log.info("=================================");
		
		return operations;
	}
	
	public void executeOperations(AsyncThrottledCachedApi api) {
		for (Entry<Currency, Double> entry: wallet.getPercentChanges().entrySet()) {
			double percent = entry.getValue();
			operationLog.log(entry.getKey(), percent, percentToUsd(percent),
					percent < 0 ? SELL : BUY);
		}
		operations.forEach((operation) -> {
			if (operation == null) {
				return;
			}
			
			switch (operation.type) {
			case BUY:
				api.buy(operation.pair, operation.rate.multiply(BUY_RATE_MOD), operation.amount, (buySuccess) -> {
					if (buySuccess) {
						log.info("finished buy: "+operation.pair);
					} else {
						log.info("ERROR buying "+operation.pair);
					}
				});
				break;
			case SELL:
				api.sell(operation.pair, operation.rate.multiply(SELL_RATE_MOD), operation.amount, (sellSuccess)-> {
					if (sellSuccess) {
						log.info("finished sell: "+operation.pair);
					} else {
						log.info("ERROR selling "+operation.pair);
					}
				});
				break;
			}
			
		});
	}
	
	private List<Operation> operationsFor(Currency from, Currency to, BigDecimal usdChange, boolean clearedToBTC) {
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
						buy( new Pair(BTC, to), usdChange.multiply(TRANSITION_RATE_MOD))};
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
						clearedToBTC ? null :
						sell(new Pair(BTC, from), usdChange)};
			} else if (to.equals(USDT)) {
				ops = new Operation[] {
						clearedToBTC ? null :
						sell(new Pair(BTC, from), usdChange),
						sell(         USDT_BTC, usdChange.multiply(TRANSITION_RATE_MOD))};
			} else {
				ops = new Operation[] {
						clearedToBTC ? null :
						sell(new Pair(BTC, from), usdChange),
						buy( new Pair(BTC, to), usdChange.multiply(TRANSITION_RATE_MOD))};
			}
		}
		
		return Arrays.asList(ops);
	}
	
	private Operation buy(Pair pair, BigDecimal usd) {
		Operation op = new Operation(BUY);
		op.pair = pair;
		op.amount = usd.divide(prices.getRate(new Pair(USDT, pair.base)), MathContext.DECIMAL64);
		op.rate = prices.getRate(pair);
		describe(op, usd, false);

		return op;
	}

	private Operation sell(Pair pair, BigDecimal usd) {
		Operation op = new Operation(SELL);
		op.pair = pair;
		op.amount = usd.divide(prices.getRate(new Pair(USDT, pair.base)), MathContext.DECIMAL64);
		op.rate = prices.getRate(pair);
		describe(op, usd, false);

		return op;
	}
	
	private Operation sellAll(Pair pair) {
		Operation op = new Operation(SELL);
		op.pair = pair;
		op.amount = wallet.getOriginalAmount(pair.base);
		op.rate = prices.getRate(pair);
		describe(op, prices.getUSDFor(pair.base).multiply(op.amount), true);

		return op;
	}
	
	private List<Currency> altcoinClearingsFirst(final Map<Currency, Double> changes) {
		List<Currency> clearings = new LinkedList<>();
		List<Currency> rest = new LinkedList<>();
		
		changes.forEach((currency, change) -> {
			if (isAltcoinClearing(currency, change)) {
				clearings.add(currency);
			} else {
				rest.add(currency);
			}
		});
		
		clearings.addAll(rest);
		return clearings;
	}
	
	private boolean isAltcoinClearing(Currency c, double change) {
		return !c.equals(BTC) && !c.equals(USDT)
				&& abs(wallet.getOriginalPercent(c) + change) < 0.01;
	}
	
	private void correctDrift(Map<Currency, Double> changes) {
		double[] driftHolder = {0.0};
		Currency[] biggestHolder = { null };
		double[] biggestChangeHolder = { 0.0 };
		changes.forEach((currency, change) -> {
			driftHolder[0] += change;
			if (biggestHolder[0] == null || biggestChangeHolder[0] < change) {
				biggestChangeHolder[0] = change;
				biggestHolder[0] = currency;
			}
		});
		if (driftHolder[0] != 0.0) {
			addToChange(changes, biggestHolder[0], - driftHolder[0]);
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
	
	private void describe(Operation op, BigDecimal usd, boolean clear) {
		String message = op.toString() + " (" + String.format(LOG_USD_FORMAT, usd) + ")";
		if (clear) message += " CLEAR";
		if (op.type == BUY) log.buy(message);
		if (op.type == SELL) log.sell(message);
	}
	
	private BigDecimal percentToUsd(double percent) {
		return new BigDecimal(percent * wallet.getWalletUSD() / 100.0);
	}
	
	public void setPrices(Prices prices) {
		this.prices = prices;
	}
	
}
