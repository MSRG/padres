package ca.utoronto.msrg.padres.common.util.io;
/**
 * 
 * @author cheung
 * 
 * URL related functions
 *
 */

import java.net.URL;
import java.net.HttpURLConnection;

public class URLOperation {
	
	public static boolean exists(String strUrl) {
		try {
			URL url = new URL(strUrl);
			HttpURLConnection httpConnection = (HttpURLConnection)url.openConnection();
			return (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK);
		} catch (Exception e) {
			return false;
		}
	}
	
	// Uncomment below code for testing
	/*
	public static void main(String[] args) {
		String url = "http://www.msrg.utoronto.ca/planetlab/setup/setup";
		//String url = "http://www.msrg.utoronto.ca/planetlab/boot.sh";
		System.out.println("The URL " + url + " returns " + URLOperation.exists(url));
	}
	*/
}
