package ca.utoronto.msrg.padres.demo.workflow.resources;

public interface JobSchedulerResources
{
	// menus
	public static final String M_MAIN = "Main";
	public static final String M_EXIT = "Exit";
	public static final String M_JOB_SCHEDULER = "Job Scheduler";
	public static final String M_TRIGGER = "Trigger";
	public static final String M_TRIGGER_DEFINITION = "Trigger Definition";
	public static final String M_TRIGGER_ISSUE = "Issue Trigger";
	public static final String M_APPLICATION = "Application";


	// dialog and window titles
	public static final String T_ERROR = "Error";
	public static final String T_TRIGGER_DEFINITION = "Job Scheduling Deployer";
	public static final String T_DEPLOYER_FRAME = "PADRES RMI JOB SCHEDULE DEPLOYER";


	// button labels
	public static final String B_OK = "OK";
	public static final String B_CANCEL = "Cancel";
	

	// text labels
	public static final String L_HOSTNAME = "Hostname:";
	public static final String L_PORT = "Port:";
	public static final String L_NOT_CONNECTED = "Not connected.";
	public static final String L_CONNECTED_TO = "Connected to: ";
	public static final String L_ARE_YOU_SURE = "Are you sure?";
	public static final String L_BROKER = "Broker: ";
	public static final String L_TRACE_MSG = "Publication: ";
	public static final String L_PUB_INTERVAL = "Publication Interval: ";
	public static final String L_INPUT_QUEUE_SIZE = "Input Queue Size";
	public static final String L_NIGHBOURS = "Neighbours";
	public static final String L_IN_PUB_RATE = "Incoming Publication Message Rate";
	public static final String L_IN_CON_RATE = "Incoming Control Message Rate";
	public static final String L_QUEUE_TIME = "Queue Time";
	public static final String L_MATCH_TIME = "Match Time";
	public static final String L_INJECT_MSG_PUB = "Publication";
	public static final String L_INJECT_MSG_SUB = "Subscription";
	public static final String L_INJECT_MSG_ADV = "Advertisement";
	public static final String L_INJECT_MSG_UNSUB = "Unsubscription";
	public static final String L_INJECT_MSG_UNADV = "Unadvertisement";
	public static final String L_INJECT_MSG_ATTRIBUTE = "Attribute";
	public static final String L_INJECT_MSG_OPERATOR = "Operator";
	public static final String L_INJECT_MSG_VALUE = "Value";

	// command IDs
	// TODO: these should probably go somewhere else
	public static final int CMD_FEDERATION_CONNECT = 1001;
	public static final int CMD_FEDERATION_DISCONNECT = 1002;
	public static final int CMD_BROKER_STOP = 1003;
	public static final int CMD_BROKER_RESUME = 1004;
	public static final int CMD_BROKER_SHUTDOWN = 1005;
	public static final int CMD_INJECT_MESSAGE = 1006;
	public static final int CMD_TRACE_MESSAGE = 1007;
	public static final int CMD_PROPERTIES = 1008;
	public static final int CMD_UNTRACE_MESSAGE = 1009;
	public static final int CMD_BROKER_STAT = 1010;

	// default values for dialog fields
	public static final String D_HOST = "localhost";
	public static final String D_PORT = "1099";
}
