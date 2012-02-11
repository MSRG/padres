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

import java.io.Serializable;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIAddress;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIServerInterface;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIMessageListenerInterfce;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.CompositeSubscription;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;

/**
 * @author Pengcheng Wan
 * 
 *         jobInfoMap hold job information : {key=applicaiton+job; value=JobInfo class}
 *         jobInstanceMap hold job instance: {key=application+job; value=JobInstance class}
 *         triggerMap hold generation id : {key=application+gid, value=gid}
 */
public class AgentRMIClient extends UnicastRemoteObject implements RMIMessageListenerInterfce {

	private static final long serialVersionUID = -5391773116170956238L;

	static Logger exceptionLogger = Logger.getLogger("Exception");
	
	private RMIServerInterface rmiConnection;

	private MessageDestination clientDest;

	private boolean isError = false;

	private String name = null;

	// Data structures for recording job information and its instance
	private HashMap<String, JobInfo> jobInfoMap = null;

	private HashMap<String, JobInstance> jobInstanceMap = null;

	private HashMap<String, String> triggerMap = null;

	/**
	 * @throws java.rmi.RemoteException
	 */
	public AgentRMIClient() throws RemoteException {
		jobInfoMap = new HashMap<String, JobInfo>();
		jobInstanceMap = new HashMap<String, JobInstance>();
		triggerMap = new HashMap<String, String>();
	}

	/**
	 * @throws java.rmi.RemoteException
	 */
	public AgentRMIClient(String name) throws RemoteException {
		this();
		this.name = name;
	}

	/**
	 * This is called when we receive a publication message.
	 */
	public void onMessage(Message message) {
	}

	@Override
	public String getID() throws RemoteException {
		return clientDest.getDestinationID();
	}

	/**
	 * Implement the abstract method of MessageListener which will handle the publication:
	 * AGENT_CONTROL, JOB_STATUS, TRIGGER
	 */
	public String receiveMessage(Message msg) {
		Publication pub = ((PublicationMessage) msg).getPublication();
		System.out.println("AGENT {" + name + "} Got Publication: " + pub + "\n");

		String classname = (pub.getPairMap().get("class")).toString();

		if (classname.equalsIgnoreCase(JobFields.AGENT_CTL)) {
			handleControl(pub);
		} else if (classname.equalsIgnoreCase(JobFields.JOB_STATUS)) {
			handleJobStatus(pub);
		} else if (classname.equalsIgnoreCase(JobFields.TRIGGER)) {
			handleTrigger(pub);
		}
		return msg.getMessageID();
	}

	/**
	 * Handle Agent Control publication which has this format:
	 * [class,AGENT_CTL],[agentname,'agentA'],[command,'SUBSCRIBE'],[content,'*']
	 * 
	 * @param pub
	 */
	protected void handleControl(Publication pub) {
		Map<String, Serializable> pairMap = pub.getPairMap();
		// System.out.println(pairMap);

		String agentName = null;
		String command = null;
		String content = null;
		Iterator<String> iter = pairMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next().toString();
			String value = pairMap.get(key).toString();
			// System.out.println(key + " = " + value);
			if (key.equalsIgnoreCase(JobFields.COMMAND)) {
				command = value;
			} else if (key.equalsIgnoreCase(JobFields.CONTENT)) {
				content = value;
			} else if (key.equalsIgnoreCase(JobFields.AGENT_NAME)) {
				agentName = value;
			}
		}

		try {
			if (name.equals(agentName) && command.equalsIgnoreCase(JobFields.COMMAND_JOBINFO)) {
				// store the detail information of an assigned job
				JobInfo job = new JobInfo(content);
				job.setAgentName(agentName);

				// put it in the HashMap
				String key = job.getApplName() + job.getJobName();
				jobInfoMap.put(key, job);

				System.out.println(name + " receive a job information :  " + job);
				System.out.println("=============================================");
			} else if (name.equals(agentName)
					&& command.equalsIgnoreCase(JobFields.COMMAND_SUBSCRIBE)) {
				// Agent will subscribe two classes: TRIGGER and CLEANUP
				// Get the real content of subscription
				String header = "";
				String subContent = "";
				int index;
				if ((index = content.indexOf(JobFields.APPL_START)) != -1) {
					index += JobFields.APPL_START.length();
					subContent = content.substring(index);
					header = JobFields.APPL_START;
				} else if (content.indexOf(JobFields.CLEANUP) != -1) {
					index += JobFields.CLEANUP.length() + 1;
					subContent = content.substring(index);
				}
				Subscription sub = MessageFactory.createSubscriptionFromString(subContent);
				SubscriptionMessage subMsg = new SubscriptionMessage(sub, "-1", clientDest);
				rmiConnection.receiveMessage(subMsg, HostType.CLIENT);

				// Handle APPL_START virtual job
				if (header.equalsIgnoreCase(JobFields.APPL_START)) {
					JobInstance instance = this.getJobInstance(JobFields.APPL_START, sub);
					instance.setAgentName(agentName);
					String instanceKey = instance.getApplName() + instance.getJobName();
					jobInstanceMap.put(instanceKey, instance);
				}

				System.out.println(JobFields.COMMAND_SUBSCRIBE + ": " + sub);
				System.out.println("=============================================");
			} else if (name.equals(agentName)
					&& command.equalsIgnoreCase(JobFields.COMMAND_COMSUBSCRIBE)) {
				// Agent subscribe composite job dependencies and store them

				CompositeSubscription cs = new CompositeSubscription(content.substring(content
						.indexOf("{")));
				JobInstance instance = this.getJobInstance(content, cs);
				instance.setAgentName(agentName);

				String instanceKey = instance.getApplName() + instance.getJobName();
				jobInstanceMap.put(instanceKey, instance);
				CompositeSubscriptionMessage csMsg = new CompositeSubscriptionMessage(cs, "-1",
						clientDest);
				rmiConnection.receiveMessage(csMsg, HostType.CLIENT);

				// System.out.println(JobFields.COMMAND_COMSUBSCRIBE + ": " + content);
				System.out.println("=============================================");
			} else if (name.equals(agentName)
					&& command.equalsIgnoreCase(JobFields.COMMAND_ADVERTISE)) {
				// Agent will advertise JOBSTATUS
				Advertisement ad = MessageFactory.createAdvertisementFromString(content);
				AdvertisementMessage advMsg = new AdvertisementMessage(ad, "-1", clientDest);
				rmiConnection.receiveMessage(advMsg, HostType.CLIENT);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper Method. Generate the job instance for APPL_START job which only subscribe Trigger.
	 */
	private JobInstance getJobInstance(String jobName, Subscription sub) {
		JobInstance instance = new JobInstance();
		instance.setJobName(jobName);
		// Get application name and all the dependency information from composite subscription
		Map<String, Predicate> subMap = sub.getPredicateMap();
		String className = ((Predicate) subMap.get("class")).getValue().toString();
		if (className.equalsIgnoreCase(JobFields.TRIGGER)) {
			// Get the application name
			String applName = ((Predicate) subMap.get(JobFields.APPLICATION_NAME)).getValue()
					.toString();
			instance.setApplName(applName);
			// Set the dependency and the schedule time
			String depSchedule = ((Predicate) subMap.get(JobFields.SCHEDULE)).getValue().toString();
			instance.setDependency(JobFields.TRIGGER, depSchedule);
			instance.setSchedule(JobFields.SCHEDULE_APPL_START);
		}
		return instance;
	}

	/**
	 * Helper Method. Generate the job instance according to the composite subscription the agent
	 * received
	 * 
	 * @param content
	 *            composite subscription content
	 * @param cs
	 *            conposite subscription
	 * @return job instance
	 */
	private JobInstance getJobInstance(String content, CompositeSubscription cs) {
		JobInstance instance = new JobInstance();

		// Get the subscriber application name and job name
		String applName = "";
		String jobName = "";
		int index;
		if ((index = content.indexOf('{')) != -1) {
			jobName = content.substring(0, index);
		}
		instance.setJobName(jobName);

		// Get application name and all the dependency information from composite subscription
		Map<String, Subscription> csMap = cs.getSubscriptionMap();
		Iterator<String> iterator = csMap.keySet().iterator();
		while (iterator.hasNext()) {
			String keyCS = iterator.next().toString();
			// String value = csMap.get(keyCS).toString();
			Subscription value = csMap.get(keyCS);

			Map<String, Predicate> subMap = value.getPredicateMap();
			String className = subMap.get("class").getValue().toString();
			if (className.equalsIgnoreCase(JobFields.JOB_STATUS)) {
				// Get the application name
				applName = subMap.get(JobFields.APPLICATION_NAME).getValue().toString();
				instance.setApplName(applName);
				// Get the dependecy job and its possible status
				String depJobName = subMap.get(JobFields.JOB_NAME).getValue().toString();
				String depStatus = subMap.get(JobFields.STATUS).getValue().toString();
				instance.setDependency(depJobName, depStatus);
				// System.out.println(depJobName+":" + depStatus);
			} else if (className.equalsIgnoreCase(JobFields.TRIGGER)) {
				String depSchedule = subMap.get(JobFields.SCHEDULE).getValue().toString();
				instance.setDependency(JobFields.SCHEDULE, depSchedule);
				// System.out.println(JobFields.SCHEDULE+":"+depSchedule);
			} else {
				// Do nothing for other fields!
			}
		}

		// Set the schedule for instance by getting it from job information
		String keyInfo = instance.getApplName() + instance.getJobName();
		JobInfo jobinfo = (JobInfo) jobInfoMap.get(keyInfo);
		if (jobinfo != null) {
			instance.setSchedule(jobinfo.getSchedule());
		} else { // APPL_END job has no schedule time field
			instance.setSchedule(JobFields.SCHEDULE_APPL_END);
		}

		return instance;
	}

	/**
	 * Start the application after receive this trigger
	 * [class,Trigger],[applname,PAYROLL],[GID,g001],[schedule,Monday]
	 * 
	 * @param pub
	 *            notificaiton of firing the application
	 */
	protected void handleTrigger(Publication pub) {
		System.out.println("==================================");
		System.out.println("Handle Trigger....");
		Map<String, Serializable> pairMap = pub.getPairMap();

		String applname = null;
		String generationID = null;
		String triggerTime = null;

		Iterator<String> iter = pairMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next().toString();
			String value = pairMap.get(key).toString();
			// System.out.println(key + " = " + value);
			if (key.equalsIgnoreCase(JobFields.APPLICATION_NAME)) {
				applname = value;
			} else if (key.equalsIgnoreCase(JobFields.GENERATION_ID)) {
				generationID = value;
			} else if (key.equalsIgnoreCase(JobFields.SCHEDULE)) {
				triggerTime = value;
			}
		}

		// Handle the repetional trigger firstly
		String triggerKey = applname + generationID;
		if (triggerMap.containsKey(triggerKey)) {
			// Not trigger anymore
			return;
		} else {
			triggerMap.put(triggerKey, generationID);
		}

		System.out.println("jobInfoMap         : " + jobInfoMap);
		System.out.println("jobInstanceMap     : " + jobInstanceMap);
		System.out.println("triggerMap         : " + triggerMap);
		System.out.println("=============================================");

		try {
			Iterator<String> it = jobInstanceMap.keySet().iterator();
			while (it.hasNext()) {
				String inKey = it.next().toString();
				JobInstance job = (JobInstance) jobInstanceMap.get(inKey);

				// Check the application name if matched
				if (applname.equalsIgnoreCase(job.getApplName())) {
					job.setGenerationID(generationID);

					// Handle APPL_START firstly
					String jobName = job.getJobName();
					if (jobName.equalsIgnoreCase(JobFields.APPL_START)) {
						job.setStatus(JobFields.STATUS_SUCCESS);
						this.publishStartOREND(JobFields.APPL_START, job);
					}

					String schedule = job.getSchedule();
					boolean ifMatched = false;
					if (schedule.equalsIgnoreCase(JobFields.SCHEDULE_APPL_END)) {
						ifMatched = true;
					} else if (schedule.equalsIgnoreCase(JobFields.SCHEDULE_APPL_START)) {
						ifMatched = false;
					} else { // for real jobs, verify the schedule firstly
						SimpleDate date = new SimpleDate(triggerTime);
						ifMatched = date.match(schedule);
					}

					if (ifMatched) {
						job.setTriggered(true);
						job.setTriggered(JobFields.SCHEDULE, JobFields.TRUE);

						System.out.println("The job has been triggered:   " + job);

						this.executeJob(applname, job);
					} else {
						if (jobName.equalsIgnoreCase(JobFields.APPL_START)) {
							// not need publish APPL_START job status again
						} else {
							// Schedule time not satisfied, publish NORUN job status
							job.setStatus(JobFields.STATUS_NORUN);
							this.publishStartOREND(job.getJobName(), job);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper Method. Publish APPL_START or APPL_END JOBSTATUS.
	 * 
	 * @param jobName
	 *            APPL_START or APPL_END
	 * @param job
	 *            job instance
	 */
	private void publishStartOREND(String jobName, JobInstance job) {
		try {
			String detail = "ANYSTRING";
			String advContent = "[class,eq," + JobFields.JOB_STATUS + "]," + "["
					+ JobFields.APPLICATION_NAME + ",eq," + job.getApplName() + "]," + "["
					+ JobFields.JOB_NAME + ",eq," + jobName + "]," + "[" + JobFields.GENERATION_ID
					+ ",isPresent," + job.getGenerationID() + "]," + "[" + JobFields.STATUS
					+ ",isPresent," + job.getStatus() + "]," + "[" + JobFields.DETAIL
					+ ",isPresent," + detail + "]";
			System.out.println(jobName + " adv:  " + advContent);
			Advertisement astart = MessageFactory.createAdvertisementFromString(advContent);
			AdvertisementMessage advMsg = new AdvertisementMessage(astart, "-1", clientDest);
			rmiConnection.receiveMessage(advMsg, HostType.CLIENT);

			String pubContent = "[class," + JobFields.JOB_STATUS + "]," + "["
					+ JobFields.APPLICATION_NAME + "," + job.getApplName() + "]," + "["
					+ JobFields.GENERATION_ID + "," + job.getGenerationID() + "]," + "["
					+ JobFields.STATUS + "," + job.getStatus() + "]," + "[" + JobFields.DETAIL
					+ "," + detail + "]," + "[" + JobFields.JOB_NAME + "," + jobName + "]";
			System.out.println(jobName + " pub:  " + pubContent);
			Publication pstart = MessageFactory.createPublicationFromString(pubContent);
			PublicationMessage pubMsg = new PublicationMessage(pstart, "-1", clientDest);
			rmiConnection.receiveMessage(pubMsg, HostType.CLIENT);
		} catch (Exception e) {
			exceptionLogger.error(e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Handle the JOBSTATUS publication which has this format:
	 * [class,JOBSTATUS],[applname,PAYROLL],[jobname,APPL_START],
	 * [GID,003],[status,SUCCESS],[detail,'some special notice']
	 * 
	 * @param pub
	 */
	protected void handleJobStatus(Publication pub) {
		System.err.println("==================================");
		System.out.println("Handle Job Status....");
		System.out.println("jobInfoMap         : " + jobInfoMap);
		System.out.println("jobInstanceMap     : " + jobInstanceMap);
		System.out.println("triggerMap         : " + triggerMap);
		System.out.println("=============================================");

		Map<String, Serializable> pairMap = pub.getPairMap();

		String applname = pairMap.get(JobFields.APPLICATION_NAME).toString();
		String dependency = pairMap.get(JobFields.JOB_NAME).toString();
		String generationID = pairMap.get(JobFields.GENERATION_ID).toString();

		String status = pairMap.get(JobFields.STATUS).toString();
		// String detail = pairMap.get(JobFields.DETAIL).toString();

		// Clean up when receive APPL_END job status
		String triggerKey = applname + generationID;
		if (triggerMap.containsKey(triggerKey) && dependency.equalsIgnoreCase(JobFields.APPL_END)) {
			clearDependency(generationID);
			triggerMap.remove(triggerKey);
			System.out.println("triggerMap         : " + triggerMap);
			return;
		}

		Iterator<String> it = jobInstanceMap.keySet().iterator();
		while (it.hasNext()) {
			String inKey = it.next().toString();
			JobInstance job = (JobInstance) jobInstanceMap.get(inKey);
			if (job.getExecuted()) { // Already executed
				// Do nothing!!
			} else if (job.getJobName().equalsIgnoreCase(JobFields.APPL_START)) {
				// Do nothing!!
			} else {
				if (applname.equalsIgnoreCase(job.getApplName())) {
					job.setGenerationID(generationID);
					job.setMatchDependency(dependency, status);

					System.out.println("Try to run :" + job + "\n");
					this.executeJob(applname, job);
				}
			}
		}
	}

	/**
	 * Special for receiving APPL_END job status and reset the jobinstance
	 * 
	 * @param generationID
	 *            the generation id for the job
	 */
	private void clearDependency(String generationID) {
		Iterator<String> it = jobInstanceMap.keySet().iterator();
		while (it.hasNext()) {
			String inKey = it.next().toString();
			JobInstance job = (JobInstance) jobInstanceMap.get(inKey);
			if (generationID.equalsIgnoreCase(job.getGenerationID())) {
				job.clearMatchDependency();
				job.setExecuted(false);
				jobInstanceMap.put(inKey, job);
			}
		}
	}

	/**
	 * Helper Method. Execute the job if all the dependent conditions has been satisfied.
	 * 
	 * @param applname
	 * @param job
	 */
	private void executeJob(String applname, JobInstance job) {
		if (job.checkMatchMap()) {
			if ((job.getJobName()).equalsIgnoreCase(JobFields.APPL_END)) {
				// Handle special virtual job APPL_END
				job.setStatus(JobFields.STATUS_SUCCESS);
				job.setExecuted(true);
				this.publishStartOREND(JobFields.APPL_END, job);

				System.out.println(JobFields.APPL_END + " should be executed !!!!");
			} else {
				String keyInfo = applname + job.getJobName();
				JobInfo jobinfo = null;
				if (jobInfoMap.containsKey(keyInfo)) {
					jobinfo = (JobInfo) jobInfoMap.get(keyInfo);
				}
				job.execute(jobinfo, rmiConnection, clientDest);
				job.setExecuted(true);
				String instanceKey = job.getApplName() + job.getJobName();
				jobInstanceMap.put(instanceKey, job);
			}
		} else {
			String instanceKey = job.getApplName() + job.getJobName();
			jobInstanceMap.put(instanceKey, job);
		}
	}

	/**
	 * Make a connection to Padres infrastructure, the format should be rmi://[host of
	 * object]:[registry port]/[object name]
	 * 
	 * @param args
	 *            Host name or host name with designated port number
	 */
	public void run(String[] args) {
		try {
			RMIAddress rmiAddress = new RMIAddress(args[0]);
			clientDest = MessageDestination.formatClientDestination(name, rmiAddress.getNodeID());
			System.out.println("Making RMI connection to " + args[0] + " ...\n");
			rmiConnection = (RMIServerInterface) Naming.lookup(args[0]);
		} catch (Exception e) {
			System.out.println("ERROR: RMI connection failed: " + e + "\n");
			isError = true;
		}

		if (rmiConnection == null) {
			System.out.println("ERROR: RMI connection failed: rmiConnection is null" + "\n");
			isError = true;
		}

		if (!isError) {
			System.out.println("RMI connection successful..." + "\n");
			try {
				rmiConnection.registerMessageListener(clientDest, this);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		initialize(args[2]);
	}

	/**
	 * Subscribe all AGENT_CTL messages
	 */
	private void initialize(String agentName) {
		try {
			Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq," + JobFields.AGENT_CTL + "]," + "["
					+ JobFields.AGENT_NAME + ",eq," + agentName + "]");
			System.out.println(agentName + " subscribe all AGENT_CTL message: " + sub + "\n");
			System.err.println("======================================");

			// send out the subscription with AGENT NAME
			SubscriptionMessage subMsg = new SubscriptionMessage(sub, "-1", clientDest);
			rmiConnection.receiveMessage(subMsg, HostType.CLIENT);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Agent Entry which has following parameters format: AgentRMIClient <broker IP> <broker port>
	 * <agent name>
	 * 
	 * @param args
	 * @throws RemoteException
	 */
	public static void main(String[] args) throws RemoteException {
		if (args.length < 2) {
			System.out.println("Usage: AgentRMIClient <broker URI> <agent name>");
			System.exit(1);
		}

		// Set the RMI security manager to load remote classes
		System.setSecurityManager(new RMISecurityManager());

		// AgentRMIClient agent = new AgentRMIClient();
		AgentRMIClient agent = new AgentRMIClient(args[1]);

		agent.run(args);

		// =====================For Testing Only==========================
		// Publication pub = MessageFactory.createPublicationFromString("" +
		// "[class,AGENT_CTL],[agentname,'agentA'],[command,'JOBINFO'],[content,'appl:PAYROLL;jobname:JobA;schedule:Daily;submission:9:00AM;userID:gli;command:ls;args:-l;isScript:N']"
		// );

		// agent.notify(pub);
		// ================================================================

	}
}
