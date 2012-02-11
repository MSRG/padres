package ca.utoronto.msrg.padres.broker.webmonitor.monitor;

public class MonitorException extends Exception{

	private static final long serialVersionUID = -922965906289249743L;

	public MonitorException(Exception e) {
		super(e);
	}
	
	public MonitorException(String string) {
		super(string);
	}

}
