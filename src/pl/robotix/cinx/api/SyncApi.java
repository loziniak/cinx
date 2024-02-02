package pl.robotix.cinx.api;

import java.math.BigDecimal;
import java.util.List;

import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Point;
import pl.robotix.cinx.TimeRange;

public interface SyncApi extends Api {

	void buy(Pair pair, BigDecimal rate, BigDecimal amount) throws OperationException;

	void sell(Pair pair, BigDecimal rate, BigDecimal amount) throws OperationException;

	List<Point> retrievePriceHistory(Pair pair, TimeRange range);

}