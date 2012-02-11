package ca.utoronto.msrg.padres.demo.webclient.client;

import java.io.File;

import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;

public class WebClientConfig extends ClientConfig {

	// port to serve from
	private static final String PROP_HTTP_PORT = "http.port";

	// relative path to directory containing webpages, images, etc.
	private static final String PROP_WEB_DIR = "web.dir";

	// default start page
	private static final String PROP_START_PAGE = "web.startpage";

	private int httpPort = 8080;

	private String webDir = "demo/etc/web/client";

	private String webStartPage = "status.html";

	protected static final String CONFIG_FILE_PATH = String.format(
			"%s%sdemo%setc%sweb%sclient%sclient.properties", PADRES_HOME, File.separator,
			File.separator, File.separator, File.separator, File.separator);

	public WebClientConfig() throws ClientException {
		super(CONFIG_FILE_PATH);
		try {
			httpPort = Integer.parseInt(clientProps.getProperty(PROP_HTTP_PORT, "8080"));
			webDir = PADRES_HOME + File.separator
					+ clientProps.getProperty(PROP_WEB_DIR, "demo/etc/web/client");
			webStartPage = clientProps.getProperty(PROP_START_PAGE, "status.html");
		} catch (NumberFormatException e) {
			throw new ClientException("Error in parsing config file: " + e.getMessage(), e);
		}
		System.out.println(CONFIG_FILE_PATH);
		System.out.println(httpPort);
		System.out.println(webDir);
		System.out.println(webStartPage);
	}

	public int getHttpPort() {
		return httpPort;
	}

	public String getWebDir() {
		return webDir;
	}

	public String getWebStartPage() {
		return webStartPage;
	}
}
