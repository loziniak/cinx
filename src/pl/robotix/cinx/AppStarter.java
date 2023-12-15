package pl.robotix.cinx;

/**
 * Workaround for this problem:
 * https://stackoverflow.com/questions/52653836/maven-shade-javafx-runtime-components-are-missing
 */
public class AppStarter {

	public static void main(String[] args) {
		App.main(args);
	}

}
