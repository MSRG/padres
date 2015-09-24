package ca.utoronto.msrg.padres.tools.webclient;

import java.io.File;
import java.net.ServerSocket;

import simple.http.connect.Connection;
import simple.http.connect.ConnectionFactory;
import simple.http.load.LoaderEngine;
import simple.http.load.LoadingException;
import simple.http.serve.CacheContext;
import simple.http.serve.ProtocolHandlerFactory;
import ca.utoronto.msrg.padres.tools.webclient.services.ClientService;
import ca.utoronto.msrg.padres.tools.webclient.services.FileService;
import ca.utoronto.msrg.padres.tools.webclient.services.PageService;

public class SimpleServer {

	private int httpPort;

	private WebClient webClient;

	public SimpleServer(int port, WebClient client) {
		httpPort = port;
		webClient = client;
	}

	public void startServer() {
		try {
			CacheContext webServerRoot = new CacheContext(new File(webClient.getWebDir()));
			LoaderEngine engine = new LoaderEngine(webServerRoot);
			registerServices(engine);
			WebRequestHandler handler = new WebRequestHandler(
					ProtocolHandlerFactory.getInstance(engine));
			Connection connection = ConnectionFactory.getConnection(handler);
			connection.connect(new ServerSocket(httpPort));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
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
		engine.load("page", PageService.class.getName(), webClient);
		engine.link("*.htm", "page");
		engine.link("*.html", "page");
		engine.link("/", "page");

		// client services
		engine.load("client", ClientService.class.getName(), webClient);
		engine.link("/client/*", "client");

		/**
		 * <code>
		 * engine.link("*.css", "files"); 
		 * engine.link("*.js", "files"); 
		 * engine.link("*.gif", * "files"); 
		 * engine.link("*.jpg", "files"); 
		 * engine.link("*.png", "files");
		 * engine.link("*.ico", "files");
		 * </code>
		 */
	}

}
