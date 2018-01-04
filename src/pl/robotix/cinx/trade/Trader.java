package pl.robotix.cinx.trade;

import static java.lang.Math.abs;
import static java.util.Collections.reverseOrder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pl.robotix.cinx.Currency;
import pl.robotix.cinx.api.Api;
import pl.robotix.cinx.wallet.Wallet;

public class Trader {
	
	public void generateOperations(Api api, Wallet wallet) {
//		List<Operation> operations = new ArrayList<>();

		Map<Currency, Double> changes = wallet.getPercentChanges();
		if (changes.isEmpty()) {
			return;
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
					System.err.println("Drift left: " + processedChange);
					break;
				}
				Currency second = secondSource.get(0);
	
				double secondChange = changes.get(second);
				if (abs(processedChange) >= abs(secondChange)) {
					secondSource.remove(second);
					biggest.remove(second);
					changes.remove(second);
					
					System.out.println("" + processed + "<-->" + second
							+ ": " + String.format("%+.8f", secondChange));
					
					processedChange += secondChange;
				} else { // abs(processedChange) < abs(secondChange)
					addToChange(changes, second, processedChange);
					secondChange += processedChange;
					
					System.out.println("" + processed + "<-->" + second
							+ ": " + String.format("%+.8f", - processedChange));
					
					processedChange = 0.0;
				}
			}
		}
		
		
//		changes.forEach((currency, percentChange) -> {
//			
//		});
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
		addToChange(changes, currencyToCorrect, - driftHolder[0]);
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
