/*

This file contains functions for interacting with the RMI client (Java code).
Ideally, a custom demo should be able to call these functions directly with
the appropriate string arguments and talk to the client.

Specific form elements should not be referenced in here. Specific use of
the XmlHttpRequest object should not be done in here.

 */

function loadClientName() {
	xmlString = createXmlTag("command", "getid");
	runMethod(xmlString, handleClientNameSucc, defaultErrFunc);
}

function handleClientNameSucc(xmlHttp) {
	var doc = xmlToDoc(xmlHttp.responseText);
	elem = document.getElementById("subtitle");
	elem.innerHTML = getXmlTagValue(doc, "success");
	cleanup(xmlHttp);
}

function showError(alertStr) {
	alert(alertStr);
}

function parsePublication(pubMsg) {
	var publication = {};
	var pattern = /\[.+\]/g;
	var match = pubMsg.match(pattern);
	if (match != null) {
		var pubParts = match[0].split("],[");
		for (i = 0; i < pubParts.length; i++) {
			pubParts[i] = pubParts[i].replace("[", "");
			pubParts[i] = pubParts[i].replace("]", "");
			var itemParts = pubParts[i].split(",");
			publication[itemParts[0]] = itemParts[1];
		}
	}
	return publication;
}
