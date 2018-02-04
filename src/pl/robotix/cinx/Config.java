package pl.robotix.cinx;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Properties;

public class Config {
	
	private static final String SUBSCRIBED_CURRENCIES_KEY = "subscribed.currencies";
	private static final String RANDOM_CURRENCIES_COUNT_KEY = "random.currencies.count";
	
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
	
	public List<Currency> getSubscribedCurrencies() {
		return asList(data.getProperty(SUBSCRIBED_CURRENCIES_KEY).split(",")).stream()
				.filter((symbol) -> !symbol.isEmpty())
				.map((symbol) -> new Currency(symbol.trim())).collect(toList());
	}
	
	public int getRandomCurrenciesCount() {
		return parseInt(data.getProperty(RANDOM_CURRENCIES_COUNT_KEY));
	}

}
