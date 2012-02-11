package ca.utoronto.msrg.padres.tools.webclient;

import java.util.ArrayList;
import java.util.List;

import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.util.CommandLine;

public class WebClientConfig extends ClientConfig {

	public static final String WEB_CONFIG_FILE_PATH = String.format(
			"%s/etc/web/client/client.properties", PADRES_HOME);

	public static final String DEFAULT_WEB_PORT = "8080";

	public static final String DEFAULT_WEB_DIR = "etc/web/";

	public static final String DEFAULT_START_PAGE = "index.html";

	public static final String CLI_WEB_PORT = "wport";

	public static final String CLI_WEB_DIR = "webdir";

	public static final String CLI_START_PAGE = "ipage";

	public int webPort = Integer.parseInt(DEFAULT_WEB_PORT);

	public String webDir = DEFAULT_WEB_DIR;

	public String startPage = DEFAULT_START_PAGE;

	public WebClientConfig() throws ClientException {
		this(WEB_CONFIG_FILE_PATH);
	}

	public WebClientConfig(String configFile) throws ClientException {
		super(configFile);
		webPort = Integer.parseInt(clientProps.getProperty("http.port", DEFAULT_WEB_PORT));
		webDir = clientProps.getProperty("web.dir", DEFAULT_WEB_DIR);
		startPage = clientProps.getProperty("web.startpage", DEFAULT_START_PAGE);
	}

	public static String[] getCommandLineKeys() {
		List<String> cliKeys = new ArrayList<String>();
		for (String key : ClientConfig.getCommandLineKeys()) {
			cliKeys.add(key);
		}
		cliKeys.add(CLI_WEB_PORT + ":");
		cliKeys.add(CLI_WEB_DIR + ":");
		cliKeys.add(CLI_START_PAGE + ":");
		return cliKeys.toArray(new String[0]);
	}

	public void overwriteWithCmdLineArgs(CommandLine cmdLine) {
		super.overwriteWithCmdLineArgs(cmdLine);
		if (cmdLine.getOptionValue(CLI_WEB_PORT) != null)
			webPort = Integer.parseInt(cmdLine.getOptionValue(WebClientConfig.CLI_WEB_PORT));
		if (cmdLine.getOptionValue(CLI_WEB_DIR) != null)
			webDir = cmdLine.getOptionValue(WebClientConfig.CLI_WEB_DIR);
		if (cmdLine.getOptionValue(CLI_START_PAGE) != null)
			startPage = cmdLine.getOptionValue(CLI_START_PAGE);
	}

	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append(super.toString());
		strBuilder.append("Web Port: ");
		strBuilder.append(webPort);
		strBuilder.append("\n");
		strBuilder.append("Web Dir: ");
		strBuilder.append(webDir);
		strBuilder.append("\n");
		strBuilder.append("Start Page: ");
		strBuilder.append(startPage);
		strBuilder.append("\n");
		return strBuilder.toString();
	}

}
