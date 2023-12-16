package pl.robotix.cinx.api;

import java.math.BigDecimal;
import java.util.List;

import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Point;
import pl.robotix.cinx.TimeRange;

public interface SyncApi extends Api {

	boolean buy(Pair pair, BigDecimal rate, BigDecimal amount);

	boolean sell(Pair pair, BigDecimal rate, BigDecimal amount);

	List<Point> retrievePriceHistory(Pair pair, TimeRange range);

}