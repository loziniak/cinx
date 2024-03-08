package pl.robotix.cinx.api;

import java.math.BigDecimal;

import pl.robotix.cinx.Pair;
import pl.robotix.cinx.TimeRange;
import pl.robotix.cinx.graph.PricesHistory;

public interface SyncApi extends Api {

	void buy(Pair pair, BigDecimal rate, BigDecimal amount) throws OperationException;

	void sell(Pair pair, BigDecimal rate, BigDecimal amount) throws OperationException;

	PricesHistory.History retrievePriceHistory(Pair pair, TimeRange range);

}