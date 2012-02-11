package ca.utoronto.msrg.padres.broker.management.web;

import java.io.File;
import java.net.ServerSocket;

import simple.http.connect.Connection;
import simple.http.connect.ConnectionFactory;
import simple.http.load.LoaderEngine;
import simple.http.load.LoadingException;
import simple.http.serve.CacheContext;
import simple.http.serve.ProtocolHandlerFactory;
import ca.utoronto.msrg.padres.broker.management.web.services.BrokerService;
import ca.utoronto.msrg.padres.broker.management.web.services.FileService;
import ca.utoronto.msrg.padres.broker.management.web.services.PageService;

public class SimpleServer {

	private int httpPort;

	private ManagementServer manageServer;

	public SimpleServer(int port, ManagementServer ms) {
		httpPort = port;
		manageServer = ms;
	}

	public void startServer() {
		try {
			CacheContext webServerRoot = new CacheContext(new File(manageServer.getWebDir()));
			LoaderEngine engine = new LoaderEngine(webServerRoot);
			registerServices(engine);

			ManagementHandler handler = new ManagementHandler(
					ProtocolHandlerFactory.getInstance(engine));
			Connection connection = ConnectionFactory.getConnection(handler);
			connection.connect(new ServerSocket(httpPort));
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/*
	 * manually register the services because that MapperEngine stuff isn't working
	 */
	private void registerServices(LoaderEngine engine) throws LoadingException {
		// default to files
		engine.load("files", FileService.class.getName());
		engine.link("*", "files");

		// pass web pages through a different service because we do a little custom page building
		// (i.e., poor man's version of templates)
		engine.load("page", PageService.class.getName(), manageServer);
		engine.link("*.htm", "page");
		engine.link("*.html", "page");
		engine.link("/", "page");

		// broker services
		engine.load("broker", BrokerService.class.getName(), manageServer);
		engine.link("/broker/*", "broker");

		/*
		 * engine.link("*.css", "files"); engine.link("*.js", "files"); engine.link("*.gif",
		 * "files"); engine.link("*.jpg", "files"); engine.link("*.png", "files");
		 * engine.link("*.ico", "files");
		 */
	}
}
