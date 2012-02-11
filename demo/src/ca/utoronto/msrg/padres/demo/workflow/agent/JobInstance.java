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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ca.utoronto.msrg.padres.common.comm.rmi.RMIServerInterface;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

/**
 * @author Pengcheng Wan
 *
 * HashMap dependency hold all the dependent information: jobstatus and trigger
 * For example, for JobD it has these information
 * { {JobB, Success:Norun}} {JobC, Success:Norun} {schedule, daily} }
 * HashMap matchMap hold all the above the condition satisfied alternatives.
 * For example, for JobD it has these information
 * { {JobB, false}} {JobC, false} {schedule, false} }
 */

public class JobInstance {
    
    //===============================================================
    //Define all required informaiton about one job instance
    private String agent_name;
    private String applName;
    private String jobName;

    private HashMap dependency;		
    private HashMap matchMap;
    private String schedule;		// schedule time default is DAILY
    private String status;
    private String generationID;   
    private String detail;

    private boolean triggered;      // schedule time has been matched
    private boolean executed;
    private AgentFrame frame = null;
    //===============================================================

    /**
     * 
     */
    public JobInstance() {
        triggered = false;
        executed = false;
        //schedule = "";
        dependency = new HashMap();
        matchMap = new HashMap();
    }
    
    public JobInstance(Publication pub){
        this();
        Map pairMap = pub.getPairMap();

        System.out.println(pairMap);

        for (Iterator i = (pairMap.keySet()).iterator(); i.hasNext();) {
			String attribute = (String) i.next();
			if (attribute == null) break;
			if (attribute.equalsIgnoreCase("class"))
				continue;
			
			Object value = pairMap.get(attribute);
			if (attribute.equalsIgnoreCase(JobFields.APPLICATION_NAME)){
			    this.applName = (String)value;
			}else if (attribute.equalsIgnoreCase(JobFields.JOB_NAME)){
			    this.jobName = (String)value;
			}else if (attribute.equalsIgnoreCase(JobFields.DEPENDENCY)){
			    dependency.put(jobName, value);
			}else if (attribute.equalsIgnoreCase(JobFields.STATUS)){
			    this.status = (String)value;
			}else if (attribute.equalsIgnoreCase(JobFields.GENERATION_ID)){
			    this.generationID = value.toString();
			}else if (attribute.equalsIgnoreCase(JobFields.DETAIL)){
			    this.detail = (String)value;
			}
		}
    }
    
    //=======================================================================
    // All set methods about fields
    public void setAgentName(String agentName){
        this.agent_name = agentName;
    }
    
    public void setApplName(String applName){
        this.applName = applName;
    }
    
    public void setJobName(String jobname){
        this.jobName = jobname;
    }
    
    public void setSchedule(String scheDate){
        this.schedule = scheDate;
    }
    
    public void setStatus(String status){
        this.status = status;
    }

    public void setGenerationID(String gid){
        this.generationID = gid;
    }
    
    public void setDetail(String detail){
    	this.detail = detail;
    }
    
    public void setTriggered(boolean ifMatched){
        this.triggered = ifMatched;
    }
    
    public void setTriggered(String key, String value){
        matchMap.put(key, value);
    }
    
    public void setExecuted(boolean executed){
        this.executed = executed;
    }

    public void setDependency(String key, String value){
        if (dependency.containsKey(key)) {
            String former = dependency.get(key).toString();
            String newValue = former + ":" + value;
            dependency.put(key, newValue);
        }else{
            dependency.put(key, value);
            matchMap.put(key, JobFields.FALSE);
        }
    }
    
    public void setMatchDependency(String key, String status){
        if (matchMap.containsKey(key)){
            String value = dependency.get(key).toString();
            if (value.indexOf(status) != -1){
                matchMap.put(key, JobFields.TRUE);
            }else{
                matchMap.put(key, JobFields.FALSE);
            }
        }else{
            // Not contain the key
            //System.err.println("Error:" + key);
        }
    }

    /** Special for clear all match state after it receive the APPL_END
     *  job status for PADRES
     */
    public void clearMatchDependency(){
        Iterator it = matchMap.keySet().iterator();
        while(it.hasNext()){
            String key = it.next().toString();
            matchMap.put(key, JobFields.FALSE);
        }
        
    }
    //=======================================================================

    //=======================================================================
    // All get methods about fields
    public String getAgentName(){
        return this.agent_name;
    }
    
    public String getApplName(){
        return this.applName;
    }
    
    public String getJobName(){
        return this.jobName;
    }
    
    public HashMap getDependencyMap(){
        return this.dependency;
    }
    
    public String getDependency(){
        String depenes = "";
        Iterator it = dependency.keySet().iterator();
        while(it.hasNext()){
            String key = it.next().toString();
            String value = dependency.get(key).toString();
            depenes += key + "=" + value + ";";
        }
        return depenes;
    }

    public String getSchedule(){
        if (schedule != null){
            return this.schedule;
        }
        return JobFields.SCHEDULE_APPL_END;
    }
    
    public String getStatus(){
        return this.status;
    }
    
    public String getGenerationID(){
        return this.generationID;
    }
    
    public String getDetail(){
    	return this.detail;
    }
    
    public boolean getTriggered(){
        return triggered;
    }
    
    public boolean getExecuted(){
        return executed;
    }
    //=======================================================================
    
    public boolean checkMatchMap(){
        // handle APPL_END case
        if (jobName.equalsIgnoreCase(JobFields.APPL_END)){
            boolean flagSchedule = false;
            boolean flagOneDependency = false;
            Iterator itend = matchMap.keySet().iterator();
            while(itend.hasNext()){
                String eachkey = itend.next().toString();
                String eachValue = matchMap.get(eachkey).toString();
                if (eachValue.equalsIgnoreCase(JobFields.TRUE)){
                    if (eachkey.equalsIgnoreCase(JobFields.SCHEDULE)){
                        flagSchedule = true;
                    }else{
                        flagOneDependency = true;
                    }
                }
            }
            if (flagSchedule && flagOneDependency){
                return true;
            }
            return false;
        }
        
        // Handle regular job
        Iterator it = matchMap.keySet().iterator();
        String output = "";
        while(it.hasNext()){
            String key = it.next().toString();
            String value = matchMap.get(key).toString();
            output += key + ":" + value + ";";
            if (value.equalsIgnoreCase(JobFields.FALSE)){
                System.out.println("**********" + output);
                return false;
            }
        }
        System.out.println("**********" + output);
        return true;
    }
    
    /**
     * When the status of the job is "SUCCESS" or "NORUN" and job schedule is
     * matched, then it can be executed
     * @param cmdargs
     */
    public boolean execute(String userID, String cmd, String args, boolean isScript){
        AgentJobPanel ajp = new AgentJobPanel(agent_name);
        System.out.println("Begin to run......");
        ajp.setStatus("Now the job of " + agent_name + " is running.......");
        ajp.setButton("Running...");
        try{
            Thread.sleep(10*1000);
            ajp.setStatus("The job of " + agent_name + " is finished!");
            ajp.setButton("Success");
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
    
    public void execute(JobInfo jobInfo, RMIServerInterface rmiConnection, MessageDestination clientDest){
        //if (frame == null){
            frame = new AgentFrame(agent_name, jobInfo, this, rmiConnection, clientDest);
        //}else{
            //frame.setVisible(true);
            //frame.reset(applName, generationID);
        //}
    }
    
    public String toString(){
        String stringRep = JobFields.APPLICATION_NAME + "=" + this.applName + ";" +
        				   JobFields.GENERATION_ID + "=" + this.generationID + ";" +
        				   JobFields.JOB_NAME + "=" + this.jobName + ";" + 
        				   JobFields.DEPENDENCY + "=" + this.dependency + ";" +
        				   "MatchDependency" + "=" + this.matchMap + ";" +
        				   JobFields.STATUS + "=" + this.status + ";" +
        				   JobFields.DETAIL + "=" + this.detail + ";" +
        				   "IFExecuted" + "=" + this.executed + ";" +
        				   JobFields.TRIGGER + "=" + this.triggered;
        				   
        return stringRep;
    }
    
    //========================Testing, Removed in the future==========================
    public static void main(String[] args) throws ParseException {
        Publication pub = MessageFactory.createPublicationFromString("[class,JOBSTATUS],[applname,PAYROLL],[jobname,APPL_START],[GID,g001],[status,SUCCESS],[detail,'AAAA']");
        JobInstance job = new JobInstance(pub);
        System.out.println(job);
    }
}