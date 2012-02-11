/*

This file contains functions for interacting with the RMI client (Java code).
Ideally, a custom demo should be able to call these functions directly with
the appropriate string arguments and talk to the client.

Specific form elements should not be referenced in here. Specific use of
the XmlHttpRequest object should not be done in here.

*/

// These strings MUST match the corresponding strings
// used in Java code
var DEFAULT_WEBUI_QID = "#default_webui";
var DEFAULT_CLASSNAME = "ca.utoronto.msrg.padres.demo.webclient.demo.BaseDemo";
var PROP_MSG_CONTENT = "msg_content";
var PROP_MSG_TYPE = "msg_type";
var PROP_MSG_ID = "msg_id";
var PROP_NUM_OF_MSG = "num_of_msg";
var PROP_BROKER_ID="broker_id";
var PROP_BROKER_ADDRESS="broker_address";
var PROP_FILE_PATH="file_path";
var PROP_FILE_CONTENTS="file_contents";
var PROP_REQ_TYPE="req_type";
var PROP_CLIENT_NAME="client_name";

var PROP_EVENT_QID="event_qid";
var PROP_EVENT_TYPE="event_type";
var PROP_EVENT_ID="event_id";
var PROP_EVENT_CONTENT="event_content";

var MSG_TYPE_PUB = "publication";
var MSG_TYPE_SUB = "subscription";
var MSG_TYPE_ADV = "advertisement";
var TYPE_BRK = "broker";

//*** Publish ***//
function publish(publicationString, brokerString, doAutoadvertise) {
	var xmlString = createXmlTag(PROP_BROKER_ADDRESS, brokerString);
	xmlString += createXmlTag(PROP_MSG_CONTENT, publicationString);
	var methodName = "publish";
	if (doAutoadvertise == true)
		methodName = "advertisePublish";
	runMethod(DEFAULT_CLASSNAME, methodName, xmlString, handlePublishSucc, defaultErrFunc)
}

// actually, this is advertisement success since
// a succesful publication doesn't really need any handling
function handlePublishSucc(xmlHttp) {
	var doc = xmlToDoc(xmlHttp.responseText);
	if (getXmlTagValue(doc, PROP_MSG_ID))
		addFilterListItem(getXmlTagValue(doc, PROP_MSG_ID), getXmlTagValue(doc, PROP_MSG_CONTENT), MSG_TYPE_ADV);
	cleanup(xmlHttp);
}

//*** Subscribe and Advertise ***//
function subscribe(subscriptionString, brokerString) {
	sendFilter(subscriptionString, brokerString, MSG_TYPE_SUB);
}

function advertise(advertisementString, brokerString) {
	sendFilter(advertisementString, brokerString, MSG_TYPE_ADV);
}

function sendFilter(filterString, brokerString, msgtypeString) {
	var xmlString = createXmlTag(PROP_BROKER_ADDRESS, brokerString);
	xmlString += createXmlTag(PROP_MSG_CONTENT, filterString);
	xmlString += createXmlTag(PROP_MSG_TYPE, msgtypeString);
	runMethod(DEFAULT_CLASSNAME, "filter", xmlString, handleFilterSucc, defaultErrFunc);
}
function handleFilterSucc(xmlHttp) {
	var doc = xmlToDoc(xmlHttp.responseText);
	addFilterListItem(
		getXmlTagValue(doc, PROP_MSG_ID), 
		getXmlTagValue(doc, PROP_MSG_CONTENT),
		getXmlTagValue(doc, PROP_MSG_TYPE));
	cleanup(xmlHttp);
}

function unfilter(filteridString, filtertypeString) {
	var xmlString = createXmlTag(PROP_MSG_TYPE, filtertypeString);
	xmlString += createXmlTag(PROP_MSG_ID, filteridString);
	runMethod(DEFAULT_CLASSNAME, "unfilter", xmlString, handleUnfilterSucc, defaultErrFunc);
}
function handleUnfilterSucc(xmlHttp) {
	var doc = xmlToDoc(xmlHttp.responseText);
	removeFilterListItem(getXmlTagValue(doc, PROP_MSG_ID), getXmlTagValue(doc, PROP_MSG_TYPE));
	cleanup(xmlHttp);
}

//*** BROKER CONNECTIONS ***//

function connect(addressString) {
	var xmlString = createXmlTag(PROP_BROKER_ADDRESS, addressString);
	runMethod(DEFAULT_CLASSNAME, "connect", xmlString, handleConnectSucc, defaultErrFunc);
}
function handleConnectSucc(xmlHttp) {
	var doc = xmlToDoc(xmlHttp.responseText);
	addOption(getXmlTagValue(doc, PROP_BROKER_ID), getXmlTagValue(doc, PROP_BROKER_ADDRESS), getCookie(CK_DEFAULT_BROKER), TYPE_BRK);
	cleanup(xmlHttp);
}

function disconnect(addrString) {
	var xmlString = createXmlTag(PROP_BROKER_ADDRESS, addrString);
	runMethod(DEFAULT_CLASSNAME, "disconnect", xmlString, handleDisconnectSucc, defaultErrFunc);
}
function handleDisconnectSucc(xmlHttp) {
	var doc = xmlToDoc(xmlHttp.responseText);
	removeOption(getXmlTagValue(doc, PROP_BROKER_ADDRESS), TYPE_BRK);
	cleanup(xmlHttp);
}

//*** UPDATES ON CLIENT INFO ***//

function setClientName(name) {
	var xmlString = createXmlTag(PROP_CLIENT_NAME, name);
	runMethod(DEFAULT_CLASSNAME, "setClientName", xmlString, handleClientNameSucc, defaultErrFunc);
}
function loadClientName() {
	runMethod(DEFAULT_CLASSNAME, "getClientName", null, handleClientNameSucc, defaultErrFunc);
}
function handleClientNameSucc(xmlHttp) {
	var doc = xmlToDoc(xmlHttp.responseText);
	setCookie(CK_CLIENT_NAME, getXmlTagValue(doc, PROP_CLIENT_NAME)); 
	setClientNameText(getCookie(CK_CLIENT_NAME));
	cleanup(xmlHttp);
}

// *** CONNECTIONS ***//

function loadBrokers() {
	var xmlString = createXmlTag(PROP_REQ_TYPE, TYPE_BRK);
	runMethod(DEFAULT_CLASSNAME, "getBrokers", xmlString, handleLoadBrokersSucc, defaultErrFunc);
}
function handleLoadBrokersSucc(xmlHttp) {
	loadOptions(xmlHttp.responseText, getCookie(CK_DEFAULT_BROKER));
	cleanup(xmlHttp);
}

//*** ACTIVE FILTER LISTS ***//
function refreshActiveSubscriptions() {
	refreshActiveFilters(MSG_TYPE_SUB);
}
function refreshActiveAdvertisements() {
	refreshActiveFilters(MSG_TYPE_ADV);
}
function refreshActiveFilters(msgtypeString) {
	var xmlString = createXmlTag(PROP_MSG_TYPE, msgtypeString);
	runMethod(DEFAULT_CLASSNAME, "getfilters", xmlString, handleRefreshFiltersSucc, defaultErrFunc);
}
function handleRefreshFiltersSucc(xmlHttp) {
	var doc = xmlToDoc(xmlHttp.responseText);
	loadActiveFilters(xmlHttp.responseText, getXmlTagValue(doc, PROP_MSG_TYPE));
	cleanup(xmlHttp);
}

//*** PRE-POPULATED PREDICATES DROPDOWN ***//
// TODO: It would be nice to make "load file" functionality a little more generic and portable
function loadFile(filepathString, msgtypeString) {
	var xmlString = createXmlTag(PROP_FILE_PATH,  filepathString);
	xmlString += createXmlTag(PROP_REQ_TYPE,  msgtypeString);
	runMethod(DEFAULT_CLASSNAME, "loadfile", xmlString, handleLoadfileSucc, defaultErrFunc);
}
function handleLoadfileSucc(xmlHttp) {
	loadOptions(xmlHttp.responseText, "");
	cleanup(xmlHttp);
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
var EVENT_TYPE_PUBLICATION = "publication";
var EVENT_TYPE_PUBLISH = "publish";
var EVENT_TYPE_SUBSCRIBE = "subscribe";
var EVENT_TYPE_ADVERTISE = "advertise";
var EVENT_TYPE_CONNECT = "connect";
var EVENT_TYPE_EXCEPTION = "exception";

var EVENT_TYPE_UNSUBSCRIBE = "unsubscribe";
var EVENT_TYPE_UNADVERTISE = "unadvertise";
var EVENT_TYPE_DISCONNECT = "disconnect";

function defaultHandleEventSucc(xmlHttp) {
	var doc = xmlToDoc(xmlHttp.responseText);
	var type = getXmlTagValue(doc, PROP_EVENT_TYPE);
	var qid = getXmlTagValue(doc, PROP_EVENT_QID);
	
	if (type == EVENT_TYPE_NOTIFICATION) {
		addNotification(
			getXmlTagValue(doc, PROP_EVENT_ID),
			getXmlTagValue(doc, PROP_EVENT_CONTENT));
	} else if (type == EVENT_TYPE_PUBLICATION) {
		addPublication(
			getXmlTagValue(doc, PROP_EVENT_CONTENT));
	} else if (type == EVENT_TYPE_SUBSCRIBE) {
		addFilterListItem(
			getXmlTagValue(doc, PROP_EVENT_ID), 
			getXmlTagValue(doc, PROP_EVENT_CONTENT),
			MSG_TYPE_SUB);
	} else if (type == EVENT_TYPE_ADVERTISE) {
		addFilterListItem(
			getXmlTagValue(doc, PROP_EVENT_ID), 
			getXmlTagValue(doc, PROP_EVENT_CONTENT),
			MSG_TYPE_ADV);
	} else if (type == EVENT_TYPE_UNSUBSCRIBE) {
		removeFilterListItem(
			getXmlTagValue(doc, PROP_EVENT_ID),
			MSG_TYPE_SUB);
	} else if (type == EVENT_TYPE_UNADVERTISE) {
		removeFilterListItem(
			getXmlTagValue(doc, PROP_EVENT_ID),
			MSG_TYPE_ADV);
	} else if (type == EVENT_TYPE_CONNECT) {
		addOption(
			getXmlTagValue(doc, PROP_EVENT_ID),
			getXmlTagValue(doc, PROP_EVENT_CONTENT),
			getCookie(CK_DEFAULT_BROKER),
			TYPE_BRK);
	} else if (type == EVENT_TYPE_DISCONNECT) {
		removeOption(
			getXmlTagValue(doc, PROP_EVENT_CONTENT),
			TYPE_BRK);
	} else if (type == EVENT_TYPE_EXCEPTION) {
		showError("Event error: " + getXmlTagValue(doc, PROP_EVENT_CONTENT));
	}

	var qid = getXmlTagValue(doc, PROP_EVENT_QID);
	setTimeout("getNextEvent('" + qid + "', defaultHandleEventSucc, defaultHandleEventErr);", 0);
	cleanup(xmlHttp);
}

function defaultHandleEventErr(xmlHttp) {
	eventActive=false;
	defaultErrFunc(xmlHttp);
	cleanup(xmlHttp);
}

