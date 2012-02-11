package ca.utoronto.msrg.padres.demo.webclient.client;

import java.net.ServerSocket;

import ca.utoronto.msrg.padres.demo.webclient.services.ClientService;
import ca.utoronto.msrg.padres.demo.webclient.services.FileService;
import ca.utoronto.msrg.padres.demo.webclient.services.PageService;

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
			// MapperEngine engine = new MapperEngine();
			LoaderEngine engine = new LoaderEngine();
			registerServices(engine);

			WebUIHandler handler = new WebUIHandler(ProtocolHandlerFactory.getInstance(engine));
			Connection connection = ConnectionFactory.getConnection(handler);
			connection.connect(new ServerSocket(httpPort));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/*
	 * manually register the services because that MapperEngine stuff isn't working
	 */
	private static void registerServices(LoaderEngine engine) throws LoadingException {
		// default to files
		engine.load("files", FileService.class.getName());
		engine.link("*", "files");

		// pass web pages through a different service
		// because we do a little custom page building
		// (i.e., poor man's version of templates)
		engine.load("page", PageService.class.getName());
		engine.link("*.htm", "page");
		engine.link("*.html", "page");
		engine.link("/", "page");

		// client services
		engine.load("client", ClientService.class.getName());
		engine.link("/client/*", "client");

		/*
		 * engine.link("*.css", "files"); engine.link("*.js", "files"); engine.link("*.gif",
		 * "files"); engine.link("*.jpg", "files"); engine.link("*.png", "files");
		 * engine.link("*.ico", "files");
		 */
	}
}
