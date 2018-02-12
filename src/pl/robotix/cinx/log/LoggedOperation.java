package pl.robotix.cinx.log;

import java.time.Instant;
import java.util.Locale;
import java.util.Scanner;

import pl.robotix.cinx.Currency;
import pl.robotix.cinx.trade.Operation;
import pl.robotix.cinx.trade.Operation.Type;

public class LoggedOperation {
	
	public Instant operationTime;
	
	public Currency currency;
	
	public Operation.Type type;
	
	public double percentChange;


	public LoggedOperation(Scanner scanner) {
		this(
			Instant.ofEpochMilli(scanner.nextLong()),
			new Currency(scanner.next("[A-Z0-9]*")),
			Type.valueOf(scanner.next("[A-Z]*")),
			Double.valueOf(scanner.next())
		);
	}
	
	public LoggedOperation(Instant operationTime, Currency currency, Type type, double percentChange) {
		super();
		this.operationTime = operationTime;
		this.currency = currency;
		this.type = type;
		this.percentChange = percentChange;
	}
	
	@Override
	public String toString() {
		String percentStr = String.format(Locale.US, "%+.1f", percentChange);
		return operationTime.toEpochMilli()+" "+currency+" "+type+" "+percentStr;
	}
	
}
