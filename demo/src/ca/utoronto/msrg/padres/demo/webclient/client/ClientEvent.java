package ca.utoronto.msrg.padres.demo.webclient.client;

/*
 * The fields of this object are packed into an XML string and sent to the browser
 * as an event notification
 */
public class ClientEvent {
	public static final String TYPE_NOTIFICATION = "notification";
	public static final String TYPE_PUBLICATION = "publication";
	public static final String TYPE_PUBLISH = "publish";
	public static final String TYPE_SUBSCRIBE = "subscribe";
	public static final String TYPE_ADVERTISE = "advertise";
	public static final String TYPE_CONNECT= "connect";
	public static final String TYPE_EXCEPTION = "exception";
	
	public static final String TYPE_UNSUBSCRIBE = "unsubscribe";
	public static final String TYPE_UNADVERTISE = "unadvertise";
	public static final String TYPE_DISCONNECT= "disconnect";
	
	private String type;
	private String id;
	private String content;
	public ClientEvent(String type, String id, String content) {
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
		return "Client Event, Type: " + type + " Id: " + id + " Content: " + content;
	}
}
