/*

This file contains functions for interacting with the RMI client (Java code).
Ideally, a custom demo should be able to call these functions directly with
the appropriate string arguments and talk to the client.

Specific form elements should not be referenced in here. Specific use of
the XmlHttpRequest object should not be done in here.

*/

// These strings MUST match the corresponding strings
// used in Java code

var DEFAULT_CLASSNAME = "ca.utoronto.msrg.padres.broker.webmonitor.demo.BaseDemo";
var PROP_MONITOR_NAME="monitor_name";
var DEFAULT_WEBUI_QID = "#default_webui";

var PROP_EVENT_QID="event_qid";
var PROP_EVENT_TYPE="event_type";
var PROP_EVENT_ID="event_id";
var PROP_EVENT_CONTENT="event_content";

var TYPE_BRK = "broker";

//*** UPDATES ON CLIENT INFO ***//

function loadMonitorName() {
	runMethod(DEFAULT_CLASSNAME, "getMonitorName", null, handleMonitorNameSucc, defaultErrFunc);
}
function handleMonitorNameSucc(xmlHttp) {
	var doc = xmlToDoc(xmlHttp.responseText);
	setCookie(CK_MONITOR_NAME, getXmlTagValue(doc, PROP_MONITOR_NAME)); 
	setMonitorNameText(getCookie(CK_MONITOR_NAME));
}


//*** Event Listening ***//

var eventsEnabled=true;
function startEvents(qid) {
	//setTimeout("getNextEvent()", 0);
	enableEvents();
	getNextEvent(qid, defaultHandleEventSucc, defaultHandleEventErr);
}
function stopEvents() {
	eventsEnabled=false;
}
function enableEvents() {
	eventsEnabled=true;
}

var eventActive=false;
function getNextEvent(qid, succFunc, errFunc) {
	if (eventActive || !eventsEnabled)
		return;
	eventActive=true;

	var xmlString=createXmlTag(PROP_EVENT_QID, qid);
	runMethod(DEFAULT_CLASSNAME, "waitForNextEvent", xmlString, succFunc, errFunc);

	// The intention is to avoid more than one outstanding
	// event request at a time.
	// So this should really be set by succFunc.
	// But then a writer of custom event
	// handlers has to remember to do it ...
	// Right now, it's pretty useless
	eventActive=false;
}

// These strings MUST match the corresponding strings used in Java code
var EVENT_TYPE_NOTIFICATION = "notification";
var EVENT_TYPE_EXCEPTION = "exception";


function defaultHandleEventSucc(xmlHttp) {
	var doc = xmlToDoc(xmlHttp.responseText);
	var type = getXmlTagValue(doc, PROP_EVENT_TYPE);
	
	if (type == EVENT_TYPE_NOTIFICATION) {
		addNotification(
			getXmlTagValue(doc, PROP_EVENT_ID),
			getXmlTagValue(doc, PROP_EVENT_CONTENT));
	} else if (type == EVENT_TYPE_EXCEPTION) {
		showError("Event error: " + getXmlTagValue(doc, PROP_EVENT_CONTENT));
	}

	var qid = getXmlTagValue(doc, PROP_EVENT_QID);
	setTimeout("getNextEvent('" + qid + "', defaultHandleEventSucc, defaultHandleEventErr);", 0);
}

function defaultHandleEventErr(xmlHttp) {
	eventActive=false;
	defaultErrFunc(xmlHttp);
}

