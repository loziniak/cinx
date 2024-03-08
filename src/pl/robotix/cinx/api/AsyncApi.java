package pl.robotix.cinx.api;

import java.math.BigDecimal;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import pl.robotix.cinx.Pair;
import pl.robotix.cinx.TimeRange;
import pl.robotix.cinx.graph.PricesHistory;

public interface AsyncApi extends Api {

	void close();

	void buy(Pair pair, BigDecimal rate, BigDecimal amount, Consumer<OperationException> callback);

	void sell(Pair pair, BigDecimal rate, BigDecimal amount, Consumer<OperationException> callback);

	void retrievePriceHistory(Pair pair, TimeRange range, Consumer<PricesHistory.History> callback);
	
	public static OperationException handleException(Callable<Void> op) {
		try {
			op.call();
		} catch (OperationException e) {
			return e;
		} catch (Exception e) {
			e.printStackTrace();
			return new OperationException((Throwable) e);
		}
		return null;
	}

}