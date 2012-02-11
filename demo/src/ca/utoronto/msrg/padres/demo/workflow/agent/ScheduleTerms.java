//=============================================================================
//This file is part of The PADRES Project.
//
//For more information, see http://www.msrg.utoronto.ca
//
//Copyright (c) 2003 Middleware Systems Research Group, University of Toronto
//=============================================================================
//$Id$
//=============================================================================
package ca.utoronto.msrg.padres.demo.workflow.agent;

/**
 * @author Pengcheng Wan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface ScheduleTerms {
    
    public final static String DAILY = "Daily";
    public final static String WORK_DAYS = "Workdays";

    public final static String MONDAY = "Monday";
    public final static String TUESDAY = "Tuesday";
    public final static String WEDNESDAY = "Wednesday";
    public final static String THURSDAY = "Thursday";
    public final static String FRIDAY = "Friday";
    public final static String SATURDAY = "Saturday";
    public final static String SUNDAY = "Sunday";

    // The schedule terms will be supported possible
    public final static String EXCEPT = "Except";
    public final static String FIRST = "First";
    public final static String LAST = "Last";
    public final static String MONTHLY = "Monthly";
    

}
