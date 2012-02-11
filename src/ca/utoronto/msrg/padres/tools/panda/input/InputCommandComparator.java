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
 * Sorts commands by their time in ascending order
 */
import java.util.Comparator;

public class InputCommandComparator<T> implements Comparator<T> {

	public int compare(Object objA, Object objB) {
		InputCommand cmdA = (InputCommand) objA;
		InputCommand cmdB = (InputCommand) objB;
		return (cmdA.getTime() > cmdB.getTime()) ? 1 : -1;
	}

}
