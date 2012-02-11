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
 * This class includes all fields related to the job information or job 
 * instance and other pub/sub fields related to the job
 */
public interface JobFields {
    
    public final static String AGENT_CTL = "AGENT_CTL";
    public final static String JOB_STATUS = "JOBSTATUS";
    public final static String TRIGGER = "TRIGGER";
    
    public final static String COMMAND_JOBINFO = "JOBINFO";
    public final static String COMMAND_SUBSCRIBE = "SUBSCRIBE";
    public final static String COMMAND_COMSUBSCRIBE = "COMPOSITESUBSCRIBE";
    public final static String COMMAND_ADVERTISE = "ADVERTISE";
    
    public final static String APPL_START = "APPL_START";
    public final static String APPL_END = "APPL_END";
    public final static String CLEANUP = "CLEANUP";
    
    public final static String AGENT_NAME = "agentname";
    public final static String COMMAND = "command";
    public final static String CONTENT = "content";
    public final static String DETAIL = "detail";
    public final static String DEPENDENCY = "dependency";
    
    //These fields holds the detail job information
    public final static String APPLICATION_NAME = "applname";
    public final static String APPLICATION = "applname";
    public final static String JOB_NAME = "jobname";
    public final static String GENERATION_ID = "GID";
    public final static String STATUS = "status";
    public final static String SCHEDULE = "schedule";
    public final static String SUBMISSION = "submission";
    public final static String USER_ID = "userID";
    public final static String ARGS = "args";
    public final static String IS_SCRIPT = "isScript";
    
    public final static String STATUS_SUCCESS = "SUCCESS";
    public final static String STATUS_FAILURE = "FAILURE";
    public final static String STATUS_NORUN = "NORUN";
    public final static String STATUS_UNKNOWN = "UNKNOWN";
    
    public final static String TRUE = "true";
    public final static String FALSE = "false";
    public final static String SCHEDULE_APPL_START = "SCHEDULE_APPL_START";
    public final static String SCHEDULE_APPL_END = "SCHEDULE_APPL_END";
}
