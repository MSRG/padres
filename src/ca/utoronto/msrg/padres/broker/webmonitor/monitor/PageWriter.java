package ca.utoronto.msrg.padres.broker.webmonitor.monitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;

/*
 * Utility class to read lines from a file and send them to an
 * output stream.
 */
public class PageWriter {
	// TODO: Make the web directory a configurable parameter

	public static final String WEB_DIR = BrokerConfig.PADRES_HOME + "etc/web/webmonitor";

	// where to find the web pages
	private static final String PAGE_DIRECTORY = WEB_DIR + '/';

	public static void sendPage(PrintStream out, String filename) {
		if (out == null)
			return;

		File file = new File(PAGE_DIRECTORY + filename);
		if (!file.exists()) {
			// TODO: throw proper exception
			System.err.println(PAGE_DIRECTORY + filename + " not found!");
			System.err.println("Current dir: " + System.getProperty("user.dir"));
			return;
		}

		String line;
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			while ((line = in.readLine()) != null) {
				out.println(line);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
