package ca.utoronto.msrg.padres.tools.padresmonitor.resources;

import java.awt.Font;

public interface MonitorResources
{
    // menus
    public static final String M_MAIN = "Main";
    public static final String M_EXIT = "Exit";
    public static final String M_FAILURE_DETECTION = "Failure Detection...";
    public static final String M_GLOBAL_FAILURE_DETECTION = "Global Failure Detection...";
    public static final String M_FEDERATION = "Federation";
    public static final String M_CONNECT_FEDERATION = "Connect to Federation...";
    public static final String M_DISCONNECT_FEDERATION = "Disconnect Federation...";
    public static final String M_BROKER = "Broker";
    public static final String M_STOP_BROKER = "Stop Broker...";
    public static final String M_RESUME_BROKER = "Resume Broker...";
    public static final String M_SHUTDOWN_BROKER = "Shutdown Broker...";
    public static final String M_INJECT_MESSAGE = "Inject Message...";
    public static final String M_UNINJECT_MESSAGE = "Uninject Message...";
    public static final String M_TRACE_PUB_MESSAGE = "Trace PUB Message...";
    public static final String M_TRACE_SUB_MESSAGE = "Trace SUB Message...";
	public static final String M_UNTRACE_MESSAGE = "Untrace Message...";
    public static final String M_PROPERTIES = "Properties...";
	public static final String M_STAT = "Statistic...";
	public static final String M_SET_ADV = "Set of Adv...";
	public static final String M_SET_SUB = "Set of Sub...";
	public static final String M_FIRST_BROKER="First Broker";
	public static final String M_BATCH_MSG="Batch Message...";
	public static final String M_BATCH_MSG_INCR="Batch Message Incremental...";
	public static final String M_TRACE_BY_TRACEID="Trace by ID";
	public static final String M_REFRESH="Refresh Federation";
	public static final String M_APPLY_LAYOUT = "Apply Layout";
	public static final String M_SHOW_ALL_EDGE_MESSAGE_COUNTER = "Show Edge Message Counter";
	public static final String M_HIDE_ALL_EDGE_MESSAGE_COUNTER = "Hide Edge Message Counter";
	public static final String M_EDGE_THROUGHPUT_INDICATOR = "Edge Throughput Indicator";
    public static final String M_SHOW_FULL_LABELS = "Show Full Node Labels";
	
	// layout submenu items
	public static final String M_LAYOUT_CIRCLE = "Circle Layout";
	public static final String M_LAYOUT_FRUCHTERMAN_REINGOLD = "FR Layout";
	public static final String M_LAYOUT_ISOM = "ISOM Layout";
	public static final String M_LAYOUT_KAMADA_KAWAI = "KK Layout";
	public static final String M_LAYOUT_KAMADA_KAWAI_INTEGER = "KK Integer Layout";
	public static final String M_LAYOUT_SPRING = "Spring Layout";
	public static final String M_LAYOUT_STATIC = "Static Layout";
//	public static final String M_LAYOUT_TREE = "Tree Layout";
	
	// edge throughput indicator submenu items
	public static final String M_EDGE_THROUGHPUT_INDICATOR_ON = "On";
	public static final String M_EDGE_THROUGHPUT_INDICATOR_OFF = "Off";
	public static final String M_EDGE_THROUGHPUT_INDICATOR_RESET = "Reset";
	

	
    // dialog and window titles
    public static final String T_ERROR = "Error";
    public static final String T_MONITOR_FRAME = "PADRES System Monitor";
    public static final String T_CONNECT_FEDERATION = "Connect to Federation";
    public static final String T_DISCONNECT_FEDERATION = "Disconnect Federation";
    public static final String T_STOP_BROKER = "Stop Broker";
    public static final String T_RESUME_BROKER = "Resume Broker";
    public static final String T_SHUTDOWN_BROKER = "Shutdown Broker";
    public static final String T_TRACE_PUB_MSG = "Trace PUB Message";
    public static final String T_TRACE_SUB_MSG = "Trace SUB Message";
    public static final String T_BROKER_PROP = "Broker Properties";
    public static final String T_BROKER_STAT = "Broker Statistic";
    public static final String T_INJECT_MESSAGE = "Inject Message";
    public static final String T_UNINJECT_MESSAGE = "Uninject Message";
    public static final String T_SET_ADV = "Set of advitisement";
    public static final String T_SET_SUB = "Set of subscription";
    public static final String T_FAILURE_DETECTION = "Failure Detection Parameters";
    public static final String T_GLOBAL_FAILURE_DETECTION = "Global Failure Detection";
    public static final String T_BATCH_MSG="Batch Message";
    public static final String T_TRACE_BY_ID="Trace By ID";
    
    // button labels
    public static final String B_OK = "OK";
    public static final String B_CANCEL = "Cancel";
    public static final String B_ADD = "ADD";
    public static final String B_PROCESS_REMAINING = "Process Remaining";
    public static final String B_PROCESS_NEXT = "Process Next";
    public static final String B_SKIP_NEXT = "Skip Next";
    public static final String B_ENABLED = "Enabled";
    public static final String B_DISABLED = "Disabled";

    // text labels
    public static final String L_HOSTNAME = "Hostname:";
    public static final String L_PORT = "Port:";
    public static final String L_BROKER_URL = "Broker URL:";
    public static final String L_NOT_CONNECTED = "Not connected.";
    public static final String L_CONNECTED_TO = "Connected to: ";
    public static final String L_ARE_YOU_SURE = "Are you sure?";
    public static final String L_BROKER = "Broker: ";
    public static final String L_TRACE_MSG = "Publication: ";
    public static final String L_TRACE_SUB_MSG = "Subscription: ";
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
	public static final String L_OS_INFO = "OS Info";
	public static final String L_INJECTED_MESSAGE = "Uninject Message";
	public static final String L_JVM_VERSION = "JVM Version";
	public static final String L_JVM_VENDOR = "JVM Vendor";
	public static final String L_UNTRACE_MESSAGE = "Untrace Message";
    public static final String L_HEARTBEAT_ENABLED = "Enable Heartbeats";
    public static final String L_HEARTBEAT_INTERVAL = "Heartbeat Interval (millis):";
    public static final String L_HEARTBEAT_TIMEOUT = "Heartbeat Timeout (millis):";
    public static final String L_HEARTBEAT_FAILURE_THRESHOLD = "Heartbeat Failure Threashold:";
	public static final String L_TRACE_ID="Trace ID";
	public static final String L_SET_GLOBAL_FD = "Set Global Failure Detection";
	public static final String L_REMAINING = "Remaining:";
	public static final String L_NEXT = "Next:";
	public static final String L_COMPLETED = "Completed:";
	
    // command IDs
    // TODO: these should probably go somewhere else
    public static final int CMD_FEDERATION_CONNECT = 1001;
    public static final int CMD_FEDERATION_DISCONNECT = 1002;
    public static final int CMD_BROKER_STOP = 1003;
    public static final int CMD_BROKER_RESUME = 1004;
    public static final int CMD_BROKER_SHUTDOWN = 1005;
    public static final int CMD_INJECT_MESSAGE = 1006;
    public static final int CMD_TRACE_PUB_MESSAGE = 1007;
    public static final int CMD_TRACE_SUB_MESSAGE = 1008;
    public static final int CMD_PROPERTIES = 1009;
	public static final int CMD_UNTRACE_MESSAGE = 1010;
	public static final int CMD_BROKER_STAT = 1011;
	public static final int CMD_UNINJECT_MESSAGE = 1012;
	public static final int CMD_SET_ADV = 1013;
	public static final int CMD_SET_SUB = 1014;
	public static final int CMD_FAILURE_DETECTION = 1015;
	public static final int CMD_BATCH_MSG=1016;
	public static final int CMD_BATCH_MSG_INCRE=1017;
	public static final int CMD_TRACE_BY_TRACEID=1018;
	public static final int CMD_GLOBAL_FAILURE_DETECTION = 1019;
	
    // default values for dialog fields
    public static final String D_BROKER_URL = "rmi://localhost:1099/BrokerA";
    public static final String D_HOST = "localhost";
    public static final String D_PORT = "1099";
    
    // layout IDs
	public static final int LAYOUT_CIRCLE = 0;
	public static final int LAYOUT_FRUCHTERMAN_REINGOLD = 1;
	public static final int LAYOUT_ISOM = 2;
	public static final int LAYOUT_KAMADA_KAWAI = 3;
	public static final int LAYOUT_KAMADA_KAWAI_INTEGER = 4;
	public static final int LAYOUT_SPRING = 5;
	public static final int LAYOUT_STATIC = 6;
    
	// edge throughput indicator state IDs
	public static final boolean EDGE_THROUGHPUT_ON = true;
	public static final boolean EDGE_THROUGHPUT_OFF = false;
	public static final int EDGE_THROUGHPUT_RESET = 2001;
	
	public static final float EDGE_DEFAULT_STROKE_WIDTH = 1;
	
	public static final String EDGE_LABEL_FONT_NAME = "Arial";
	public static final int EDGE_LABEL_FONT_STYLE = Font.BOLD;
	public static final int EDGE_LABEL_FONT_SIZE = 16;
	//must be valid HTML colour
	public static final String EDGE_LABEL_FONT_COLOUR = "red";
	
	public static final String VERTEX_LABEL_FONT_NAME = "Arial";
	public static final int VERTEX_LABEL_FONT_STYLE = Font.PLAIN;
	public static final int VERTEX_LABEL_FONT_SIZE = 14;
	//must be valid HTML colour
	public static final String VERTEX_LABEL_FONT_COLOUR = "blue";
}
