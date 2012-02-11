/*
 * Created on Nov 28, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.broker.router.matching.jess.userfunction;

import jess.Context;
import jess.JessException;
import jess.RU;
import jess.Userfunction;
import jess.Value;
import jess.ValueVector;

/**
 * @author strangelove
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class GreaterThanOrEqualTo implements Userfunction {

	/* (non-Javadoc)
	 * @see jess.Userfunction#getName()
	 */
	public String getName() {
		return ">=";
	}

	/* (non-Javadoc)
	 * @see jess.Userfunction#call(jess.ValueVector, jess.Context)
	 */
	public Value call(ValueVector functionArguments, Context context) throws JessException {
		Value argument1 = functionArguments.get(1).resolveValue(context);
		Value argument2 = functionArguments.get(2).resolveValue(context);
		
		int arg1Type = argument1.type();
		int arg2Type = argument2.type();
		if((isANumber(arg1Type) && !isANumber(arg2Type)) || (!isANumber(arg1Type) && isANumber(arg2Type))) {
			return new Value(false);
		} else {
			double arg1 = argument1.numericValue(context);
			double arg2 = argument2.numericValue(context);
			return new Value(arg1 >= arg2);
		}

	}
	
	private boolean isANumber(int type) {
		return (type == RU.INTEGER || type == RU.FLOAT || type == RU.LONG);
	}
}
