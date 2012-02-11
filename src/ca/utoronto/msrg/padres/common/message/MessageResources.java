package ca.utoronto.msrg.padres.common.message;

public interface MessageResources {
    
	//  return type + "(" + messageID + "):lasthop=" + lastHopID + ",nexthop=" + nextHopID + ",time='" + messageTime + "',priority=" + 
	//  priority+",TraceRouteID='"+TraceRouteID+"',PreviousBroker='"+PreviousBroker_BrokerID+"'";
  
	//field names within messages
  
	//all headers
	public static final String F_LAST_HOP = "lasthop"; 
  	public static final String F_NEXT_HOP = "nexthop";
  	public static final String F_TIME = "time";
  	public static final String F_PRIORITY = "priority";
  	public static final String F_TRACE_ROUTE_ID = "TraceRouteID"; 
  	public static final String F_PREVIOUS_BROKER_ID = "PreviousBroker";
  	public static final String F_PREVIOUS_CLIENT_ID = "PreviousClient";
  	public static final String F_EXPIRY = "expiry";
  
  	//subscription headers
  	public static final String F_START_TIME = "startTime"; 
    public static final String F_END_TIME = "endTime";
    public static final String F_TTL = "TTL";
  
}