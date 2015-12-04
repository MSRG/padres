package ca.utoronto.msrg.padres.broker.webmonitor.monitor;

public class MonitorEvent {

	public static final String TYPE_NOTIFICATION= "notification";
	public static final String TYPE_EXCEPTION = "exception";
	
	private String type;
	private String id;
	private String content;
	
	public MonitorEvent(String type, String id, String content) {
		this.type = type;
		this.id = id;
		this.content = content;
	}
	public String getType() {
		return type;
	}
	public String getId() {
		return id;
	}
	public String getContent() {
		return content;
	}
	public void pubContent(String newcontent){
		this.content=newcontent;
	}
	public String toString() {
		return "Monitor Event, Type: " + type + " Id: " + id + " Content: " + content;
	}
}
