package pl.robotix.cinx.trade;

import java.util.List;

public class Operations implements Operation {
	
	private List<OperationInfo> operations;

	@Override
	public boolean canRollback() {
		return true;
	}

	@Override
	public void rollback() {
		
		for (OperationInfo operation: operations) {
			if (operation.state == State.IN_PROGRESS) {
				throw new UnsupportedOperationException("Cannot rollback because operation is in progress.");
			}			
		}		
		for (OperationInfo operation: operations) {
			if (operation.state == State.DONE) {
				operation.operation.rollback();
			}
		}
	}
	
	
	private static enum State {
		NEW, IN_PROGRESS, DONE, ERROR
	}

	private static class OperationInfo {
		
		public Operation operation;
		
		public State state;
		
	}
}
