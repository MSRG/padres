package ca.utoronto.msrg.padres.broker.webmonitor.monitor;

import simple.http.ProtocolHandler;
import simple.http.Request;
import simple.http.Response;

public class WebMonitorUIHandler  implements ProtocolHandler {
	  private ProtocolHandler handler;

	    public WebMonitorUIHandler(ProtocolHandler handler) {
	       this.handler = handler;
	    }

	    /**
	     * Automatically stick a header on responses
	     */
	    public void handle(Request req, Response resp) {
	       resp.set("Server", "WebMonitorUIServer/1.0 (Simple)");
	       resp.setDate("Date", System.currentTimeMillis());
	       resp.setDate("Last-Modified", System.currentTimeMillis());
	       handler.handle(req, resp);
	    }

}
