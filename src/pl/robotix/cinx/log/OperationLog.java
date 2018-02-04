package pl.robotix.cinx.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.Locale;

import pl.robotix.cinx.Currency;
import pl.robotix.cinx.trade.Operation;

public class OperationLog {
	
	private PrintStream printer;

	public OperationLog(String file) throws IOException {
		File logFile = new File(file);
		if (!logFile.exists()) {
			logFile.createNewFile();
		}
		printer = new PrintStream(new FileOutputStream(logFile, true));
	}
	
	public void log(Currency currency, double percent, BigDecimal usdPrice, Operation.Type type) {
		String percentStr = String.format(Locale.US, "%+.1f", percent);
		printer.println(System.currentTimeMillis()+" "+currency+" "+type+" "+percentStr);
		printer.flush();
	}

	public void close() {
		printer.close();
	}

}
