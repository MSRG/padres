/*

This file contains functions for working with form elements in the
default demo pages. Custom demos can reuse these if they wish but
should ideally create their own form elements with their own behaviour.

*/

//** custom lists for displaying filters

// initialize form elements
//
// NOTE: Setting form elements like this in javascript is apparently
//       bad because IE will not free the elements properly - causing
//       memory leaks. FIXME: store ID string instead
//
var subListElem;
var advListElem;
function setAdvListElem(elem) {
	advListElem=elem;
}
function setSubListElem(elem) {
	subListElem=elem;
}
function getFilterListElem(msgtypeString) {
	var filterListElem=advListElem;
	if (msgtypeString == MSG_TYPE_SUB)
		filterListElem=subListElem;
	return filterListElem;
}
function addFilterListItem(msgidString, msgString, msgtypeString) {
	if (!msgidString || !msgString || ! msgtypeString)
		return;
	if (msgidString == PROP_MSG_TYPE)
		return;
		
	// avoid duplicates
	var divIdName = msgidString;
	if (document.getElementById(divIdName))
		return;

	var filterListElem=getFilterListElem(msgtypeString);
	// comment out this check for easier debugging
	if (!filterListElem)
		return;


	var newdiv = document.createElement("div");
	newdiv.setAttribute("id",divIdName);
	newdiv.setAttribute("class","message_list_item");
	newdiv.innerHTML = "<input type=\"button\" onclick=\""

//	newdiv.innerHTML += "removeFilterListItem("
//				+ "'" + divIdName + "'"
//				+ ","
//				+ "'" + msgtypeString + "'"
//				+ ");";

				+ "unfilter("
				+ "'" + divIdName + "'"
				+ ","
				+ "'" + msgtypeString + "'"
				+ ");"

				+ "\" value=\"Remove\" />"
				+ "&nbsp;&nbsp;(" + divIdName + ") " + msgString;
	filterListElem.appendChild(newdiv);
}

function removeFilterListItem(divId, msgtypeString) {
	var filterListElem = getFilterListElem(msgtypeString);
	var olddiv = document.getElementById(divId);
	if (olddiv)
		filterListElem.removeChild(olddiv);
}
function loadActiveFilters(xmlString, msgtypeString) {
	var doc=xmlToDoc(xmlString);
	var nodes = doc.childNodes[0].childNodes;
	
	clearActiveFilters(msgtypeString);
	for (ii=0; ii<nodes.length; ii++) {
		addFilterListItem(nodes[ii].nodeName, nodes[ii].firstChild.nodeValue, msgtypeString);
	}
}

function clearActiveFilters(msgtypeString) {
	var filterListElem=getFilterListElem(msgtypeString);
	while (filterListElem.firstChild) {
		filterListElem.removeChild(filterListElem.firstChild);
	}
}

//** dropdowns

var brokerOptionElem;
var pubOptionElem;
var subOptionElem;
var advOptionElem;
function setBrokerOptionElem(elem) {
	brokerOptionElem=elem;
}
function setPubOptionElem(elem) {
	pubOptionElem=elem;
}
function setSubOptionElem(elem) {
	subOptionElem=elem;
}
function setAdvOptionElem(elem) {
	advOptionElem=elem;
}

function loadOptions(xmlString, defString) {
	var doc=xmlToDoc(xmlString);
	var nodes = doc.childNodes[0].childNodes;

	var optionType = getXmlTagValue(doc, PROP_REQ_TYPE);
	if (!optionType)
		optionType = getXmlTagValue(doc, PROP_MSG_TYPE);
	
	getOptionElem(optionType).length=0;
	for (ii=0; ii<nodes.length; ii++) {
		if (nodes[ii].nodeName != PROP_REQ_TYPE)
			addOption(nodes[ii].nodeName, nodes[ii].firstChild.nodeValue, defString, optionType);
	}
}

function addOption(idString, valueString, defaultString, optionType) {
	var optionElem=getOptionElem(optionType);

	// comment out for easier debugging
	if (!optionElem)
		return;

	// TODO: check for duplicates before adding?

	optionElem.options[optionElem.length] = new Option(idString + ": " + valueString, valueString);
	if (defaultString == valueString)
		optionElem.selectedIndex=optionElem.length-1;
}

function removeOption(valueString, optionType) {
	var optionElem=getOptionElem(optionType);
	
	//search for first occurrence by value
	var options = optionElem.options;
	for (ii=0; ii<options.length; ii++) {
		if (options[ii].value == valueString) {
			optionElem.options[options[ii].index] = null;
			return;
		}
	}
}

// FIXME: use better method of choosing option element
function getOptionElem(optionType) {
	var optionElem=brokerOptionElem;
	if (optionType==MSG_TYPE_PUB)
		optionElem=pubOptionElem;
	else if (optionType==MSG_TYPE_SUB)
		optionElem=subOptionElem;
	else if (optionType==MSG_TYPE_ADV)
		optionElem=advOptionElem;
	return optionElem;
}

// textareas

function addTuple(publicationTextElem, attributeString, valueString) {
	if (!attributeString || attributeString.length == 0) {
		alert("Missing attribute in tuple");
		return;
	}
	if (!valueString || valueString.length == 0) {
		alert("Missing value in tuple");
		return;
	}

	addToPublication(publicationTextElem,		
	"("
	+ attributeString
	+" "
	+ valueString
	+")");
}

function addToPublication(publicationTextElem, tupleString) {
	publicationTextElem.value=
	publicationTextElem.value
	+ tupleString;
}

function addContentToText(textElem, attributeString, operatorString, valueString) {
	if (!attributeString || attributeString.length == 0) {
		alert("Missing attribute");
		return;
	}
	if (!valueString || valueString.length == 0) {
		alert("Missing value");
		return;
	}
	// publications may have no operators
	
	addToText(textElem,
	"("
	+attributeString
	+" "
	+operatorString
	+" "
	+valueString
	+")"
	);
}

function addQuery(queryString){
	addToText(document.getElementById("query_text")
		,queryString);
}
function addToText(textElem, textString) {
	textElem.value=
	textElem.value
	+ textString;
}

function clearText(textElem) {
	textElem.value="";
}

var notificationArea;
var notificationCount;
var notificationCountLabel;
/*
 * Note: The retail demo scenario pages still call setNotificationArea
 *       with only a single argument and uses the old notification
 *       counting mechanism.
 *      
 *       I will let the maintainer of the retail demo scenario decide
 *       whether or not to use the new count mechanism since I haven't
 *       looked into whether there are odd interactions with counting
 *       in that demo.
 */
function setNotificationArea(notiArea, countLabel) {
	notificationArea=notiArea;
	notificationCountLabel=countLabel;
	resetNotificationCount();
}
function resetNotificationCount() {
	notificationCount = 0;
	if (notificationCountLabel) {
		notificationCountLabel.value = "(" + notificationCount + ")";
	}
}

var notificationSeparator = "\n\n";
// old count mechanism
function setNumberOfNotificationArea(elem){
	numberOfNotificationArea = elem;
}
function addNotification(id, content) {
	if (notificationArea) {
		notificationArea.value=
		id
		+ ": " 
		+ content
		+ notificationSeparator
		+ notificationArea.value;
	}
	// new count mechanism
	if (notificationCountLabel) {
		notificationCount++;
		notificationCountLabel.value = "(" + notificationCount + ")";
	}
}

// client name text
var clientNameElem;
function setClientNameElem(elem) {
	clientNameElem = elem;
}
function setClientNameText(name) {
	if (clientNameElem)
		clientNameElem.innerHTML=name;
}

// page title
var pageTitleElem;
function setPageTitleElem(elem) {
	pageTitleElem = elem;
}
function setPageTitleText(name) {
	if (pageTitleElem)
		pageTitleElem.innerHTML="&nbsp;(" + name + ")";
}
