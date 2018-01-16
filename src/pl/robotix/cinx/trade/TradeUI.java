package pl.robotix.cinx.trade;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import pl.robotix.cinx.Logger;

public class TradeUI extends StackPane {
	
	private VBox logArea = new VBox();
	private ScrollPane scroll = new ScrollPane(logArea);
	
	private final Trader trader;
	
	public TradeUI(Logger logger, Trader trader) {
		this.trader = trader;

		logArea.setPadding(new Insets(20));
		
		scroll.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
		getChildren().add(scroll);
		
		Button execute = new Button("Execute");
		StackPane.setAlignment(execute, Pos.BOTTOM_RIGHT);
		StackPane.setMargin(execute, new Insets(40));
		execute.setOnAction((event) -> {
			this.trader.executeOperations();
		});
		getChildren().add(execute);
		
		logger.addBuyListener((message) -> log(message, Color.BLUE, null, 0.5));
		logger.addInfoListener((message) -> log(message, null, null, 0.5));
		logger.addErrorListener((message) -> log(message, Color.RED, Color.RED, 1.0));
		logger.addSellListener((message) -> log(message, Color.RED, null, 0.5));
	}
	
	private void log(String message, Paint fill, Paint stroke, double opacity) {
		Text text = new Text(message);
		if (fill != null) text.setFill(fill);
		if (stroke != null) text.setStroke(stroke);
		text.setOpacity(0.5);
		logArea.getChildren().add(text);
		scroll.setVvalue(scroll.getVmin());
	}

}
