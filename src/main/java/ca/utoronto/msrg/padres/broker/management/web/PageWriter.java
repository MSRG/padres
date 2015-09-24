package ca.utoronto.msrg.padres.broker.management.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

/*
 * Utility class to read lines from a file and send them to an
 * output stream.
 */
public class PageWriter {

	public static void sendPage(PrintStream out, String filename, String pageDir) {
		if (out == null)
			return;

		File file = new File(pageDir + filename);
		if (!file.exists()) {
			String msg = pageDir + filename + " not found!" + "\n" + "Current dir: "
					+ System.getProperty("user.dir");
			// Don't throw the exception (it will be returned as a web page)
			(new RuntimeException(msg)).printStackTrace(System.out);
			return;
		}

		String line;
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			while ((line = in.readLine()) != null) {
				out.println(line);
			}
			in.close();
		} catch (FileNotFoundException e) {
			(new RuntimeException(e.getMessage())).printStackTrace(System.out);
		} catch (IOException e) {
			(new RuntimeException(e.getMessage())).printStackTrace(System.out);
		}
	}
}
