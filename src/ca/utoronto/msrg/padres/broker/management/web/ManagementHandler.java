package ca.utoronto.msrg.padres.broker.management.web;

import simple.http.*;

public class ManagementHandler implements ProtocolHandler {

	private ProtocolHandler handler;

	public ManagementHandler(ProtocolHandler handler) {
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
