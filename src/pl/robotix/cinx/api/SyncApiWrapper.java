package pl.robotix.cinx.api;

import static pl.robotix.cinx.trade.Operation.Type.BUY;
import static pl.robotix.cinx.trade.Operation.Type.SELL;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Point;
import pl.robotix.cinx.Prices;
import pl.robotix.cinx.TimeRange;

public class SyncApiWrapper implements AsyncApi {

	private SyncApi api;

	public SyncApiWrapper(SyncApi api) {
		this.api = api;
	}
	
	@Override
	public void initTimeRanges() {
		api.initTimeRanges();
	}

	@Override
	public void close() {}

	@Override
	public void buy(Pair pair, BigDecimal rate, BigDecimal amount, Consumer<OperationException> callback) {
		callback.accept(AsyncApi.handleException(
				() -> { api.buy(pair, rate, amount); return null; }
			)
		);
	}

	@Override
	public void sell(Pair pair, BigDecimal rate, BigDecimal amount, Consumer<OperationException> callback) {
		callback.accept(AsyncApi.handleException(
				() -> { api.sell(pair, rate, amount); return null; }
			)
		);
	}

	@Override
	public void retrievePriceHistory(Pair pair, TimeRange range, Consumer<List<Point>> callback) {
		callback.accept(api.retrievePriceHistory(pair, range));
	}

	@Override
	public Map<Currency, BigDecimal> retrieveBalance() {
		return api.retrieveBalance();
	}

	@Override
	public Prices retrievePrices() {
		return api.retrievePrices();
	}

	@Override
	public Prices retrievePrices(Collection<Pair> pairs) {
		return api.retrievePrices(pairs);
	}

	@Override
	public Collection<Currency> pairsForMarket(Currency c) {
		return api.pairsForMarket(c);
	}

	@Override
	public boolean isExchangeable(Currency c) {
		return api.isExchangeable(c);
	}
	
	@Override
	public double takerFee() {
		return api.takerFee();
	}

}
