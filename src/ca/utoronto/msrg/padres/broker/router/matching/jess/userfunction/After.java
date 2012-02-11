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
 * Created on 23-Sep-2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ca.utoronto.msrg.padres.broker.router.matching.jess.userfunction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.router.matching.rete.ReteMatcher;

import jess.Context;
import jess.JessException;
import jess.Userfunction;
import jess.Value;
import jess.ValueVector;

/**
 * @author strangelove
 * 
 *         To change the template for this generated type comment go to
 *         Window>Preferences>Java>Code Generation>Code and Comments
 */
public class After implements Userfunction {

	/*
	 * (non-Javadoc)
	 * 
	 * @see jess.Userfunction#getName()
	 */
	static Logger reteMatcherLogger = Logger.getLogger(ReteMatcher.class);

	static Logger exceptionLogger = Logger.getLogger("Exception");

	public String getName() {
		return "after";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jess.Userfunction#call(jess.ValueVector, jess.Context)
	 */
	public Value call(ValueVector functionArguments, Context context) throws JessException {
		Value argument1 = functionArguments.get(1).resolveValue(context);
		Value argument2 = functionArguments.get(2).resolveValue(context);

		// int arg1Type = argument1.type();
		// int arg2Type = argument2.type();

		SimpleDateFormat timeFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
		try {
			Date time1 = timeFormat.parse(argument1.toString().replaceAll("\"", ""));
			Date time2 = timeFormat.parse(argument2.toString().replaceAll("\"", ""));
			return new Value(time1.after(time2));
		} catch (ParseException pe) {
			reteMatcherLogger.error("Failed to parse time: " + pe);
			exceptionLogger.error("Failed to parse time: " + pe);
			return new Value(false);
		}
	}
}
