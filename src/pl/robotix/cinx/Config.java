package pl.robotix.cinx;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Config {
	
//	private static final String PRICES_KEY = "prices";
	private static final String SUBSCRIBED_CURRENCIES_KEY = "subscribed.currencies";
	
	private Properties data;
	private File file;

	public Config(String configFile) throws IOException {
		data = new Properties();
		file = new File(configFile);
		if (!file.exists()) {
			file.createNewFile();
		}
		
		InputStreamReader input = new InputStreamReader(new FileInputStream(file));
		data.load(input);
		input.close();
		
	}

	public void saveToDisk() throws IOException {
		OutputStreamWriter output = new OutputStreamWriter(new FileOutputStream(file));
		data.store(output, null);
		output.close();
	}
	
//	public void setPrices(Map<Pair, BigDecimal> prices) {
//		String pricesStr = prices.entrySet().stream()
//		.map((entry) -> entry.getKey() + ":" + entry.getValue()) 
//		.reduce((one, two) -> one + ";" + two)
//		.orElse(null);
//		data.put(PRICES_KEY, pricesStr);
//	}

	public List<Currency> getSubscribedCurrencies() {
		return Arrays.asList(data.getProperty(SUBSCRIBED_CURRENCIES_KEY).split(",")).stream()
				.map((symbol) -> new Currency(symbol)).collect(toList());
	}

}
