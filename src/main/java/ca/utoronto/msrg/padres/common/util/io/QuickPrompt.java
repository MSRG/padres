/*
 * Created on May 17, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.common.util.io;

/**
 * @author cheung
 *
 * Useful if you want to prompt the user for something and expect a response.
 * Not case sensitive
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;

public class QuickPrompt {
	private HashSet<String> validResponseSet;

	private String prompt;

	private String input = "";

	/**
	 * Constructor. 'prompt' is the message to be displayed
	 * 
	 * @param prompt
	 */
	public QuickPrompt(String msg) {
		validResponseSet = new HashSet<String>();
		prompt = msg;
	}

	public void addResponse(String str) {
		validResponseSet.add(str);
	}

	public void removeResponse(String str) {
		validResponseSet.remove(str);
	}

	public void changePrompt(String newPrompt) {
		prompt = newPrompt;
	}

	/**
	 * Returns the response typed in by the user, accepts either upper or lower
	 * case input
	 * 
	 * @return
	 */
	public String promptAndGetResponse() {
		// Console setup
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

			System.out.println(prompt);
			while (true) {
				showRetryPrompt();
				input = in.readLine().trim();

				for (String response : validResponseSet) {
					if (response.toString().equalsIgnoreCase(input)) {
						in = null; // don't close it!
						return input;
					}
				}
			}
		} catch (Exception e) {
			System.out.println("ERROR: (QuickPrompt) Exception occurred when getting "
					+ "input from the console.");
			System.err.println(e.getMessage());
		}

		// Should never reach here
		return null;
	}

	private void showRetryPrompt() {
		System.out.print("Enter one of "
				+ validResponseSet.toString().replaceAll("\\[", "(").replaceAll("\\]", ")").replaceAll(
						",", " |") + ": ");
	}

	/**
	 * Returns the last input
	 * 
	 * @return
	 */
	public String getResponse() {
		return input;
	}

	/**
	 * Call this to clean up this class after using it
	 * 
	 */
	public void clean() {
		validResponseSet.clear();
		validResponseSet = null;
		input = null;
		prompt = null;
	}
}
