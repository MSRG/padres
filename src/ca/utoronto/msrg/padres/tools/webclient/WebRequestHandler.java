package ca.utoronto.msrg.padres.tools.webclient;

import simple.http.*;

public class WebRequestHandler implements ProtocolHandler  {
    private ProtocolHandler handler;

    public WebRequestHandler(ProtocolHandler handler) {
       this.handler = handler;
    }

    /**
     * Automatically stick a header on responses
     */
    public void handle(Request req, Response resp) {
       resp.set("Server", "WebClientServer/1.0 (Simple)");
       resp.setDate("Date", System.currentTimeMillis());
       resp.setDate("Last-Modified", System.currentTimeMillis());
       handler.handle(req, resp);
    }
}
