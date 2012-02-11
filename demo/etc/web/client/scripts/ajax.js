/*

All functions related to XML parsing and HTTP communication with
the server go into this script. Browser compatibility testing/support
is still required. Only Firefox and Opera have been used so far.

*/

function getXmlHttpObject()
{
  var xmlHttpObj=null;
  try {
    // Firefox, Opera 8.0+, Safari
    xmlHttpObj=new XMLHttpRequest();
  } catch (e) {
    // Internet Explorer
    try {
      xmlHttpObj=new ActiveXObject("Msxml2.XMLHTTP");
    } catch (e) {
      xmlHttpObj=new ActiveXObject("Microsoft.XMLHTTP");
    }
  }
  return xmlHttpObj;
}

/*

	The XmlHttpRequest object communication function. Ideally, this should be the
  only function that gets the object, opens the connection, and sends requests.

*/

var runAsync=true;
// The hostUrl parameter is optional when this function is called
function runMethod(classnameString, methodnameString, xmlString, succFunc, errFunc, hostUrl) {
	if (!classnameString) {
		showError("Missing Class Name");
		return;
	}
	if (!methodnameString) {
		showError("Missing Method Name");
		return;
	}

	// avoid null arguments
	if (!xmlString)
		xmlString = createXmlTag("no_arg", "");
	var xmlReq = createXmlReq(classnameString, methodnameString, xmlString);

	var xmlHttp=getXmlHttpObject();
	xmlHttp.onreadystatechange=function() {
		if (xmlHttp.readyState==1) {
			// Opera is okay with this. 
			// Firefox is not.
			// xmlHttp.send(xmlReq);
		} else if (xmlHttp.readyState==4) {
			if (xmlHttp.status==200) {
				if (xmlHttp.responseText.length > 0) {
					if (succFunc)
						succFunc(xmlHttp);
				} else {
					if (errFunc)
						errFunc(xmlHttp);
					else
						statusErr(classnameString, methodnameString, xmlHttp);
				}
			} else {
				statusErr(classnameString, methodnameString, xmlHttp);
			}
		}
	}
    
	// by default, use client services relative to current URL
	var url="client/";
	if (hostUrl != null)
		url = hostUrl+url;
	xmlHttp.open("POST", url, runAsync);
	xmlHttp.setRequestHeader("content-type", "text/xml");
	xmlHttp.send(xmlReq);
}

/*
 * Called when the callback handlers have completed
 * Doesn't seem to be helping with memory leak
 */
function cleanup(xmlHttp) {
	//xmlHttp.onreadystatechange=new function() {};
	xmlHttp.onreadystatechange = undefined;
	xmlHttp = undefined;
}

function defaultErrFunc(xmlHttp) {
	showError(xmlHttp.statusText);
}
function statusErr(classnameString, methodnameString, xmlHttp) {
	showError(classnameString + "." + methodnameString + " Error: " + xmlHttp.statusText);
}

function createXmlReq(classnameString, methodnameString, xmlArg) {
	var xml = xmlArg;
	return "<?xml version=\"1.0\"?>"
			+ "<request>"
			+ "<class_name>" + classnameString + "</class_name>"
			+ "<method_name>" + methodnameString + "</method_name>"
			+ xml
			+ "</request>";
}

function createXmlTag(keyString, valString) {
	valString = valString.replace(/&/g, "&amp;");
	valString = valString.replace(/</g, "&lt;");
	valString = valString.replace(/>/g, "&gt;");
	return "<" + keyString + ">"
			+ valString
			+ "</" + keyString + ">";
}

function xmlToDoc(xmlString) {
	return (new DOMParser()).parseFromString(xmlString, "text/xml")
}

function getXmlTagValue(doc, tagname) {
	var nodes=doc.getElementsByTagName(tagname);
	
	// grab the node's text node child and return it's value
	if (nodes.length > 0)
		return nodes[0].firstChild.nodeValue;
}
