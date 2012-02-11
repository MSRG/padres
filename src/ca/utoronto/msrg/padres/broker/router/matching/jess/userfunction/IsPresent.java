// =============================================================================
// This file is part of The PADRES Project.
//
// For more information, see http://www.msrg.utoronto.ca
//
// Copyright (c) 2003 Middleware Systems Research Group, University of Toronto
// =============================================================================
// $Id$
// =============================================================================
/*
 * Created on 12-Aug-2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
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
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class IsPresent implements Userfunction {

	/* (non-Javadoc)
	 * @see jess.Userfunction#getName()
	 */
	public String getName() {
		return "isPresent";
	}

	/* (non-Javadoc)
	 * @see jess.Userfunction#call(jess.ValueVector, jess.Context)
	 */
	public Value call(ValueVector functionArguments, Context context) throws JessException {
		Value argument1 = functionArguments.get(1).resolveValue(context);
		Value argument2 = functionArguments.get(2).resolveValue(context);
		
		int arg1Type = argument1.type();
		int arg2Type = argument2.type();
				
		if(arg1Type == arg2Type) {
			// There needs to be a check here because Jess doesn't have a Boolean type
			if(arg1Type == RU.ATOM) {
				// If one's a boolean and the other isn't, return false.  This shouldn't really ever happen, as we only
				// allow booleans as atoms
				if(isAJessBoolean(argument1) ^ isAJessBoolean(argument2)) {
					return new Value(false);
				} else {
					return new Value(true);
				}
			} else {
				return new Value(true);
			}
		} else if(isAJessNumber(arg1Type) && isAJessNumber(arg2Type)) {
			return new Value(true);
		} else {
			return new Value(false);
		}
	}
	
	private boolean isAJessBoolean(Value argument) {
		if((argument.toString()).equals("TRUE") || (argument.toString()).equals("FALSE")) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isAJessNumber(int type) {
		if(type == RU.INTEGER || type == RU.FLOAT || type == RU.LONG) {
			return true;
		} else {
			return false;
		}
	}
}
