package pl.robotix.cinx.trade;

public interface Operation {
	
	public boolean canRollback();
	
	public void rollback();

}
