package pl.robotix.cinx.log;

import static java.time.temporal.ChronoUnit.SECONDS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import pl.robotix.cinx.Currency;
import pl.robotix.cinx.TimeRange;
import pl.robotix.cinx.trade.Operation;

public class OperationLog {
	
	private PrintStream printer;
	
	private List<LoggedOperation> pastOperations = new ArrayList<>();
	
	private Set<Currency> alreadyLogged = new HashSet<>();

	public OperationLog(String file) throws IOException {
		File logFile = new File(file);
		if (!logFile.exists()) {
			logFile.createNewFile();
		} else {
			readPastOPerations(logFile);
		}

		printer = new PrintStream(new FileOutputStream(logFile, true));
	}
	
	
	public List<LoggedOperation> pastOperationsFor(Currency c, TimeRange time) {
		List<LoggedOperation> ret = new ArrayList<>();
		Instant startFrom = Instant.now().minus(time.periodSeconds, SECONDS);
		
		for (int i=pastOperations.size() - 1; i >= 0; i--) {
			LoggedOperation operation = pastOperations.get(i);
			if (operation.currency.equals(c)) {
				ret.add(operation);
			}
			if (operation.operationTime.isBefore(startFrom)) {
				break;
			}
		}
		
		return ret;
	}

	private void readPastOPerations(File logFile) throws FileNotFoundException {
		Scanner s = new Scanner(logFile);
		LoggedOperation operation;
		long line = 0;
		while (s.hasNextLine()) {
			line++;
			try {
				operation = new LoggedOperation(s);
				pastOperations.add(operation);

			} catch(Exception e) {
				System.err.println("Error in line "+line+": "+e.getMessage());
				e.printStackTrace(System.err);
				continue;

			} finally {
				String left = s.nextLine();
				if (!left.equals("")) {
					System.err.println("Warning in line "+line+". Text left to the end of line: "+left);
				}
			}
		}

		s.close();
	}
	
	public void initSession() {
		alreadyLogged.clear();
	}

	public void log(Currency currency, double percent, BigDecimal usdPrice, Operation.Type type) {
		if (!alreadyLogged.contains(currency)) {
			printer.println(new LoggedOperation(Instant.now(), currency, type, percent));
			printer.flush();
			alreadyLogged.add(currency);
		}
	}

	public void close() {
		printer.close();
	}

}
