package ca.utoronto.msrg.padres.demo.webclient.client;

import simple.http.*;

public class WebUIHandler implements ProtocolHandler  {
    private ProtocolHandler handler;

    public WebUIHandler(ProtocolHandler handler) {
       this.handler = handler;
    }

    /**
     * Automatically stick a header on responses
     */
    public void handle(Request req, Response resp) {
       resp.set("Server", "WebUIServer/1.0 (Simple)");
       resp.setDate("Date", System.currentTimeMillis());
       resp.setDate("Last-Modified", System.currentTimeMillis());
       handler.handle(req, resp);
    }
}
