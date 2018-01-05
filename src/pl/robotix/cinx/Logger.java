package pl.robotix.cinx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Logger {
	
	public Logger() {
	}
	
	public Logger(Consumer<String> listener) {
		addAllTypesListener(listener);
	}
	
	private Map<Type, List<Consumer<String>>> listeners = new HashMap<>();
	{
		for (Type type: Type.values()) {
			listeners.put(type, new ArrayList<>());
		}
	}
	
	public void addAllTypesListener(Consumer<String> listener) {
		for (Type type: Type.values()) {
			listeners.get(type).add(listener);
		}
	}
	
	public void addInfoListener(Consumer<String> listener) {
		listeners.get(Type.INFO).add(listener);
	}

	public void addBuyListener(Consumer<String> listener) {
		listeners.get(Type.BUY).add(listener);
	}

	public void addSellListener(Consumer<String> listener) {
		listeners.get(Type.SELL).add(listener);
	}

	public void addErrorListener(Consumer<String> listener) {
		listeners.get(Type.ERROR).add(listener);
	}

	public void info(String message) {
		listeners.get(Type.INFO).forEach((listener) -> listener.accept(message));
	}

	public void buy(String message) {
		listeners.get(Type.BUY).forEach((listener) -> listener.accept(message));
	}

	public void sell(String message) {
		listeners.get(Type.SELL).forEach((listener) -> listener.accept(message));
	}

	public void error(String message) {
		listeners.get(Type.ERROR).forEach((listener) -> listener.accept(message));
	}
	

	private static enum Type {
		INFO, ERROR, BUY, SELL
	}
	
}
