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
 * This class hold the detail information of an assigned job for agent
 */
public class JobInfo {

    //===============================================================
    //The fields are about the job information
    private String agent_name;
    private String appl_name;
    private String job_name;
    private String schedule_time;
    private String submission_time;
    private String user_id;
    private String cmd_name;
    private String arguments;
    private boolean is_script;
    private String shell;
    //===============================================================

    /**
     * Default constructor
     */
    public JobInfo() {
        agent_name  = "";
    }
    
	/**
	 * Constructor for building JobInfo from stringified version.
	 * @param stringRep The string representation of the Job Infomation
	*/
	public JobInfo(String stringRep) {
		this();
		int index = 0;				//index of each field
		int indexOfSemiComma = -1;	//next index of delimeter ';'
		
		String compareS = stringRep.toUpperCase();
		
		if ((index = compareS.indexOf(JobFields.APPLICATION.toUpperCase())) != -1){
		    indexOfSemiComma = stringRep.indexOf(';', indexOfSemiComma);
		    index += JobFields.APPLICATION.length();
		    appl_name = stringRep.substring(index+1, indexOfSemiComma);
		}
		
		if ((index = compareS.indexOf(JobFields.JOB_NAME.toUpperCase(), index)) != -1){
		    indexOfSemiComma = stringRep.indexOf(';', indexOfSemiComma+1);
		    index += JobFields.JOB_NAME.length();
		    job_name = stringRep.substring(index+1, indexOfSemiComma);
		}
		
		if ((index = compareS.indexOf(JobFields.SCHEDULE.toUpperCase(), index)) != -1){
		    indexOfSemiComma = stringRep.indexOf(';', indexOfSemiComma+1);
		    index += JobFields.SCHEDULE.length();
		    schedule_time = stringRep.substring(index+1, indexOfSemiComma);
		}
		
		if ((index = compareS.indexOf(JobFields.SUBMISSION.toUpperCase(), index)) != -1){
		    indexOfSemiComma = stringRep.indexOf(';', indexOfSemiComma+1);
		    index += JobFields.SUBMISSION.length();
		    submission_time = stringRep.substring(index+1, indexOfSemiComma);
		}
		
		if ((index = compareS.indexOf(JobFields.USER_ID.toUpperCase(), index)) != -1){
		    indexOfSemiComma = stringRep.indexOf(';', indexOfSemiComma+1);
		    index += JobFields.USER_ID.length();
		    user_id = stringRep.substring(index+1, indexOfSemiComma);
		}
		
		if ((index = compareS.indexOf(JobFields.COMMAND.toUpperCase(), index)) != -1){
		    indexOfSemiComma = stringRep.indexOf(';', indexOfSemiComma+1);
		    index += JobFields.COMMAND.length();
		    cmd_name = stringRep.substring(index+1, indexOfSemiComma);
		}
		
		if ((index = compareS.indexOf(JobFields.ARGS.toUpperCase(), index)) != -1){
		    indexOfSemiComma = stringRep.indexOf(';', indexOfSemiComma+1);
		    index += JobFields.ARGS.length();
		    arguments = stringRep.substring(index+1, indexOfSemiComma);
		}
		
		// Suppose IS_SCRIPT is the last field
		if ((index = compareS.indexOf(JobFields.IS_SCRIPT.toUpperCase(), index)) != -1){
		    indexOfSemiComma = stringRep.indexOf(';', indexOfSemiComma+1);
		    index += JobFields.IS_SCRIPT.length();
		    //String temp =  stringRep.substring(index+1, indexOfSemiComma);
		    String temp =  stringRep.substring(index+1);
		    //is_script = (temp.equalsIgnoreCase("N") ? false : true);
		    if (temp.equalsIgnoreCase("N") || temp.equalsIgnoreCase("NO")){
		        is_script = false;
		    }else{
		        is_script = true;
		    }
		}
	}
	
	//================================================================
	// All the get methods
	public String getApplName(){
	    return this.appl_name;
	}
	
	public String getJobName(){
	    return this.job_name;
	}
	
	public String getSchedule(){
	    return this.schedule_time;
	}
	
	public String getSubmission(){
	    return this.submission_time;
	}
	
	public String getUserID(){
	    return this.user_id;
	}
	
	public String getCommand(){
	    return this.cmd_name;
	}
	
	public String getArgs(){
	    return this.arguments;
	}
	
	public boolean isScript(){
	    return this.is_script;
	}
	//================================================================
	
	/*
	 * Returns a string representation of the detail job information with form:
	 * "[attr1,value1][attr2,value2]..."
	 *
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
	    String stringRep = this.agent_name + ";" +
	    				   this.appl_name + ";" +
	    				   this.job_name + ";" +
	    				   this.schedule_time + ";" +
	    				   this.submission_time + ";" +
	    				   this.user_id + ";" +
	    				   this.cmd_name + ";" +
	    				   this.arguments + ";" +
	    				   this.is_script;
		return stringRep;
	}
	
	//================================================================
	// All the set methods
    public void setAgentName(String agentName){
        this.agent_name = agentName;
    }
    
    public void setAppName(String appName){
        this.appl_name = appName;
    }
    
    public void setJobName(String jobName){
        this.job_name = jobName;
    }
    
    public void setScheduleTime(String scheduleTime){
        this.schedule_time = scheduleTime;
    }
    
    public void setSubmissionTime(String submissionTime){
        this.submission_time = submissionTime;
    }
    
    public void setUserId(String userID){
        this.user_id = userID;
    }
    
    public void setCommand(String cmd){
        this.cmd_name = cmd;
    }
    
    public void setArguments(String arg){
        this.arguments = arg;
    }
    
    public void setShell(String shell){
        this.shell = shell;
    }
    
    public void setIfScript(boolean isScript){
        this.is_script = isScript;
    }
    //===============================================================
    
    
    public static void main(String[] args) {
        JobInfo job = new JobInfo(
        "appl:PAYROLL;jobname:JobD;schedule:Daily;submission:9:00AM;userID:gli;command:ls;args:-l;isScript:N");
        
        System.out.println(job);
    }
}
