package ca.utoronto.msrg.padres.demo.webclient.client;

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
	// FIXME: Make the web directory a configurable parameter
	
	// where to find the web pages
//	private static final String PAGE_DIRECTORY = System.getProperty("user.dir") + System.getProperty("file.separator") + WebUIClient.getWebDir() + System.getProperty("file.separator"); 
	private static final String PAGE_DIRECTORY = WebUIClient.getWebDir() + System.getProperty("file.separator");
		
	public static void sendPage(PrintStream out, String filename) {
		if (out == null) 
			return;
		
		File file = new File(PAGE_DIRECTORY + filename);
		if (!file.exists()) {
			String msg = PAGE_DIRECTORY + filename + " not found!" + "\n" + "Current dir: " + System.getProperty("user.dir");
			// Don't throw the exception (it will be returned as a web page)
			(new RuntimeException(msg)).printStackTrace();
			return;
		}
		
		String line;
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			while ((line=in.readLine()) != null) {
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
