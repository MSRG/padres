/*
 * Created on May 6, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.tools.panda.input;

/**
 * @author cheung
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import java.util.StringTokenizer;
import java.util.Map;

import ca.utoronto.msrg.padres.tools.panda.CommandGenerator;

public class ProcessRemoveCommand extends InputCommand {

	private ProcessRemoveCommand(String line, StringTokenizer st) {
		super(line, st);
	}
		
	public static ProcessRemoveCommand toProcessRemoveCommand(String line) {
		try {
			return new ProcessRemoveCommand(line, new StringTokenizer(line));
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return null;
		}
	}

	
	public String getExecutableCommand(Map<Object, String> cmdTemplateMap) {
		String cmdTemplate = cmdTemplateMap.get(this.getClass());
		
		return cmdTemplate
			.replaceAll("<TIME>", Double.toString(super.time))
			.replaceAll("<PROCESS_NAME_TO_KILL>",
				CommandGenerator.PREFIX_PROCESS_NAME + super.id);
	}
}
