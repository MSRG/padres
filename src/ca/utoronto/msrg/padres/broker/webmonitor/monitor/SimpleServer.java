package ca.utoronto.msrg.padres.broker.webmonitor.monitor;

import java.net.ServerSocket;

import ca.utoronto.msrg.padres.broker.webmonitor.services.FileService;

import simple.http.connect.Connection;
import simple.http.connect.ConnectionFactory;
import simple.http.load.LoaderEngine;
import simple.http.load.LoadingException;
import simple.http.serve.ProtocolHandlerFactory;

public class SimpleServer {

	private int httpPort;

	public SimpleServer(int port) {
		httpPort = port;
	}

	public void startServer() {
		try {
			LoaderEngine engine = new LoaderEngine();
			registerServices(engine);

			WebMonitorUIHandler handler = new WebMonitorUIHandler(
					ProtocolHandlerFactory.getInstance(engine));
			Connection connection = ConnectionFactory.getConnection(handler);
			connection.connect(new ServerSocket(httpPort));
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/*
	 * manually register the services because that MapperEngine stuff isn't working
	 */
	private static void registerServices(LoaderEngine engine) throws LoadingException,
			ClassNotFoundException {
		// default to files
		// engine.load("files", "ca.utoronto.msrg.padres.broker.webmonitor.services.FileService");
		engine.load("files", FileService.class.getName());
		engine.link("*", "files");

		// monitor services
		engine.load("monitor", "ca.utoronto.msrg.padres.broker.webmonitor.services.MonitorService");
		engine.link("/client/webmonitor/monitor/*", "monitor");

		engine.load("page", "ca.utoronto.msrg.padres.broker.webmonitor.services.PageService");
		engine.link("*.htm", "page");
		engine.link("*.html", "page");
		engine.link("/", "page");

	}
}
