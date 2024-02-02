package pl.robotix.cinx.api;

public class OperationException extends Exception {

	private static final long serialVersionUID = 1L;

	
	public OperationException(String message) {
		super(message);
	}

	public OperationException(Throwable cause) {
		super(cause);
	}
	
	
//	Pair pair;
//	
//	Operation.Type operation;
//
//	public OperationException(Pair pair, Type operation, String msg) {
//		super(msg);
//		this.pair = pair;
//		this.operation = operation;
//	}
//
//	public OperationException(Pair pair, Type operation, Exception e) {
//		super(e);
//		this.pair = pair;
//		this.operation = operation;
//	}

}
