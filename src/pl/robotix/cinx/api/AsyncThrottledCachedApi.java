package pl.robotix.cinx.api;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.concurrent.Task;
import pl.robotix.cinx.Currency;
import pl.robotix.cinx.Pair;
import pl.robotix.cinx.Prices;
import pl.robotix.cinx.TimeRange;
import pl.robotix.cinx.graph.PricesHistory.History;

public class AsyncThrottledCachedApi implements AsyncApi {
	
	private static final long THROTTLE_QUEUES_COUNT = 2; // sync and async
	private static final long THROTTLE_SAFETY_MARGIN_MS = 100;
	
	private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);

	private SyncApi api;
	private long timeoutMs;
	private int maxRetries;
	private int delayMs;

	private Map<String, Object> cache = new ConcurrentHashMap<>();
	private AtomicLong lastOpMillis;
	private AtomicLong lastOpScheduledMillis;

	public AsyncThrottledCachedApi(SyncApi api, long timeoutMs, int maxRetries, int delayMs) {
		this.api = api;
		this.timeoutMs = timeoutMs;
		this.maxRetries = maxRetries;
		this.delayMs = delayMs;
		lastOpMillis = new AtomicLong(System.currentTimeMillis());
		lastOpScheduledMillis = new AtomicLong(System.currentTimeMillis());
	}
	
	@Override
	public void initTimeRanges() {
		api.initTimeRanges();
	}
	
	@Override
	public void close() {
		executor.shutdown();
		try {
			executor.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public Map<Currency, BigDecimal> retrieveBalance() {
		syncThrottleControl();
		return api.retrieveBalance();
	}
	
	@Override
	public Prices retrievePrices() {
		syncThrottleControl();
		return api.retrievePrices();
	}
	
	@Override
	public Prices retrievePrices(Collection<Pair> pairs) {
		syncThrottleControl();
		return api.retrievePrices(pairs);
	}
	
	@Override
	public void buy(Pair pair, BigDecimal rate, BigDecimal amount, final Consumer<OperationException> callback) {
		asyncRunRecursive(() -> AsyncApi.handleException(
				() -> { api.buy(pair, rate, amount); return null; }
			), callback, 0, false);
	}
	
	@Override
	public void sell(Pair pair, BigDecimal rate, BigDecimal amount, final Consumer<OperationException> callback) {
		asyncRunRecursive(() -> AsyncApi.handleException(
				() -> { api.sell(pair, rate, amount); return null; }
			), callback, 0, false);
	}

	@Override
	public void retrievePriceHistory(Pair pair, TimeRange range, final Consumer<History> callback) {
		asyncCache(() -> api.retrievePriceHistory(pair, range), 
				"retrievePriceHistory_" + pair + "_" + range,
				callback, true);
	}
	
	@Override
	public Collection<Pair> pairsForMarket(Currency c) {
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
	
	@Override
	public TimeValues timeValues(TimeRange range, Currency currency) {
		return api.timeValues(range, currency);
	}
	
	
	
	private <T> void asyncRunRecursive(Callable<T> operation, Consumer<T> callback, int depth, boolean timeout) {
		if (timeout
				&& depth >= maxRetries) {
			System.out.println(LocalTime.now().toString()+" timeout: "+depth+" retries.");
			return;
		}

		Task<T> task = new Task<T>() {
			@Override
			protected T call() throws Exception {
				T ret = operation.call();
				return ret;
			}
			
			@Override
			protected void succeeded() {
				try {
					callback.accept(get());
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}
			}
			
			@Override
			protected void failed() {
				System.out.println(LocalTime.now().toString()+" failed.");
				getException().printStackTrace();
			}
			
			@Override
			protected void cancelled() {
				if (timeout) {
					System.out.println(LocalTime.now().toString()+" timeout: "+depth+" retrying.");
					asyncRunRecursive(operation, callback, depth + 1, true);
				}
			}
		};
		
		Task<Void> timeoutTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				return null; // do nothing
			}
			
			@Override
			protected void succeeded() {
				if (task.isRunning()) {
					task.cancel(true);
				}
			}
			
		};
		
		long asyncWaitMs = throttleMs() - (System.currentTimeMillis() - lastOpScheduledMillis.get());
		if (asyncWaitMs < 0) {
			asyncWaitMs = 0;
		}
		lastOpScheduledMillis.set(System.currentTimeMillis() + asyncWaitMs);

		executor.schedule(task, asyncWaitMs, TimeUnit.MILLISECONDS);
		if (timeout) {
			executor.schedule(timeoutTask, asyncWaitMs + timeoutMs, TimeUnit.MILLISECONDS);
		}
	}

	private void syncThrottleControl() {
		try {
			long waitMs = throttleMs() - (System.currentTimeMillis() - lastOpMillis.get());
			if (waitMs > 0) {
				Thread.sleep(waitMs);
			}
		} catch (InterruptedException e) {
		}
		lastOpMillis.set(System.currentTimeMillis());
	}
	
	private long throttleMs() {
		return delayMs * THROTTLE_QUEUES_COUNT + THROTTLE_SAFETY_MARGIN_MS;
	}
	
	private <R> R syncCache(Supplier<R> operation, String key) {
		@SuppressWarnings("unchecked")
		R value = (R) cache.get(key);
		if (value == null) {
			syncThrottleControl();
			value = operation.get();
			cache.put(key, value);
		} else {
			System.out.println("From cache: "+key);
		}
		return value;
	}

	private <R> void asyncCache(Supplier<R> operation, String key, Consumer<R> callback, boolean timeout) {
		@SuppressWarnings("unchecked")
		R value = (R) cache.get(key);
		if (value == null) {
			asyncRunRecursive(() -> {
				R val = operation.get();
				cache.put(key, val);
				return val;
			}, callback, 0, timeout);
		} else {
			System.out.println("From cache: "+key);
			callback.accept(value);
		}
	}

}
