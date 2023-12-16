package pl.robotix.cinx.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Point;
import pl.robotix.cinx.TimeRange;

public interface AsyncApi extends Api {

	void close();

	void buy(Pair pair, BigDecimal rate, BigDecimal amount, Consumer<Boolean> callback);

	void sell(Pair pair, BigDecimal rate, BigDecimal amount, Consumer<Boolean> callback);

	void retrievePriceHistory(Pair pair, TimeRange range, Consumer<List<Point>> callback);

}