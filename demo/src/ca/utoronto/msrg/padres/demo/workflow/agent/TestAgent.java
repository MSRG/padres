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

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;
import java.util.Iterator;

/**
 * @author Pengcheng Wan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestAgent {

    public TestAgent(String fileName) {
        //JobInstance instance = this.getJobInstance(content);
        //System.out.println(instance);
		try {
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String temp = "";
			while ((temp = br.readLine()) != null){
			    if (temp.length() != 0){
			        System.out.println(temp);
			        
			    }
			}
			br.close();
		}catch (Exception e) {
			e.printStackTrace();
		}

    }
    
	private JobInstance getJobInstance(String content){
	    JobInstance instance = new JobInstance();
	    
	    // Get the subscriber job name
	    String jobName = "";
	    int index;
	    if ((index = content.indexOf('{')) != -1) {
	        jobName = content.substring(0, index);
	    }
	    instance.setJobName(jobName);
	    
	    // Get the subscriber application name
	    String applName = "";
	    if ((index = content.indexOf(JobFields.APPLICATION_NAME)) != -1){
	        // The following 4 is the length of string ",eq,"
	        int start = index + JobFields.APPLICATION_NAME.length() + 4;
	        int end = content.indexOf(']', start);
	        if (end != -1){
	            applName = content.substring(start, end);
	        }
	    }
	    instance.setApplName(applName);
	    
	    // Get the job dependency
	    String dependency = "";
	    index = 0; 		// start from the first character
	    while(true){
		    if ((index = content.indexOf(JobFields.JOB_NAME, index)) != -1) {
		        // The following 4 is the length of string ",eq,"
		        int start = index + JobFields.JOB_NAME.length() + 4;
		        int end = content.indexOf(']', start);
		        if (end != -1){
		            String temp = content.substring(start, end);
		            if (dependency.indexOf(temp) == -1){
		                dependency += temp;
		            }
		        }
		        index = end;
		    }else{
		        break;
		    }
	    }
	    //instance.setDependency(dependency);
	    
	    return instance;
	}

	public void testVector(){
	    // Test Vector 
	    Vector v = new Vector();
	    v.add("WPC");
	    v.add("CHenhai");
	    v.add("TIanQin");
	    System.out.println(v);

	    Iterator itf = v.iterator();
	    int ind = -1;
	    while(itf.hasNext()){
	        ind++;
	        String con = (String)itf.next();
	        if (con.equalsIgnoreCase("wpc")){
	            v.setElementAt("New Staff", ind);
	        }
	    }
	    System.out.println(v);
	}
	
	public void testJobInfo(){
	    
	}

    public static void main(String[] args) {
        TestAgent ta = new TestAgent("TestPub1.TXT");
        
        JobInstance job = ta.getJobInstance("" +
        		//"[class,eq,JOBSTATUS],[applname,eq,PAYROLL],[jobname,eq,APPL_START],[GID,eq,$S$X],[status,eq,SUCCESS],[detail,isPresent,ANYSTRING]" +
               "JobA{{[class,eq,JOBSTATUS],[applname,eq,PAYROLL],[jobname,eq,APPL_START],[GID,eq,$S$X],[status,eq,SUCCESS],[detail,isPresent,ANYSTRING]}&{[class,eq,Trigger],[applname,eq,PAYROLL],[GID,eq,$S$X],[schedule,isPresent,DAILY]}}" 		
               // "JobD{{{{[class,eq,JOBSTATUS],[applname,eq,PAYROLL],[jobname,eq,JobB],[GID,eq,$S$X],[status,eq,SUCCESS]}||{[class,eq,JOBSTATUS],[applname,eq,PAYROLL],[jobname,eq,JobB],[GID,eq,$S$X],[status,eq,'NORUN']}}&{{[class,eq,JOBSTATUS],[applname,eq,PAYROLL],[jobname,eq,JobC],[GID,eq,$S$X],[status,eq,'SUCCESSS']}||{[class,eq,JOBSTATUS],[applname,eq,PAYROLL],[jobname,eq,JobC],[GID,eq,$S$X],[status,eq,'NORUN']}}}&{[class,eq,Trigger],[applname,eq,PAYROLL],[GID,eq,$S$X],[schedule,isPresent,DAILY]}}"
        );
        System.out.println(job);
    }
}
