/*
 * Created on May 8, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.tools.panda.input;

/**
 * @author cheung
 *
 * This is here to allow the InputCommand to instantiate itself :P
 */

import java.util.StringTokenizer;
import java.util.Map;

public class DummyCommand extends InputCommand {

	public DummyCommand(String line) {
		super(line, new StringTokenizer(line));		
	}
	
	public String getExecutableCommand(Map<Object, String> cmdMap) {
		return null;
	}
}
