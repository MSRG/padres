/*
 * Created on May 6, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.tools.panda.input;

/**
 * @author Alex
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import java.util.StringTokenizer;
import java.util.Map;

import ca.utoronto.msrg.padres.common.util.TypeChecker;
import ca.utoronto.msrg.padres.common.util.datastructure.DataTypeException;
import ca.utoronto.msrg.padres.common.util.datastructure.ParseException;
import ca.utoronto.msrg.padres.tools.panda.CommandGenerator;


public class ProcessAddCommand extends InputCommand {

	private final String processScript;
	private final String processArgs;


	/*
	 * Private constructor
	 */
	private ProcessAddCommand(String line, StringTokenizer st) {
		super(line, st);
		
		try {
			processScript = st.nextToken();
			// Process script may have no arguments at all
			String tempProcessArgs = st.hasMoreTokens() ? st.nextToken() : "";
			while (st.hasMoreTokens())
				tempProcessArgs += " " + st.nextToken();
			processArgs = tempProcessArgs;
			tempProcessArgs = null;
			
		} catch (Exception e) {
			throw new ParseException(
				"ERROR: (PublisherAddCommand) Parameters for input '" + line 
				+ "' are malformed.\n");
		}
				
		verify(line, st);
	}
	
	/**
	 * Public factory
	 * 
	 * @param line
	 * @return
	 */
	public static ProcessAddCommand toProcessAddCommand(String line) {
		try {
			return new ProcessAddCommand(line, new StringTokenizer(line));
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return null;
		}
	}


	private void verify(String input, StringTokenizer st) {
		if (st.hasMoreElements()) {
			System.out.println("WARNING: (ProcessAddCommand) Extra parameters in input '"
				+ input + "' ignored.\n");
		}
		
		// address can be a name or IP address, so can't check
		if (!TypeChecker.isString(processScript)) {
			throw new DataTypeException(
				"ERROR: (ProcessAddCommand) Incorrect arguments "
				+ "for " + super.getId());
		}
	}


	public String getExecutableCommand(Map<Object, String> cmdTemplateMap) {
		String cmdTemplate = cmdTemplateMap.get(this.getClass());
		
		return cmdTemplate
			.replaceAll("<TIME>", Double.toString(super.time))
			.replaceAll("<START_PROCESS_SCRIPT>", processScript)
			.replaceAll("<RENAMED_START_PROCESS_SCRIPT>",
				CommandGenerator.PREFIX_PROCESS_NAME + super.id)
			.replaceAll("<PROGRAM_ARGS>", processArgs);		
	}
}
