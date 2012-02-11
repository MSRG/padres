/*

Cookies are used for maintaining state in the browser

*/

// attribute names for cookies
var CK_MONITOR_NAME="monitor_name";
var CK_DEFAULT_BROKER="default_broker";

function setCookie(nameString, valueString) {
	if (nameString.length == 0)
		return;
		
	// expire on browser close
	document.cookie = nameString+"="+valueString+"; path=/";
}

function getCookie(nameString) {
	if (nameString.length == 0)
		return;
		
	var nameEQ = nameString + "=";
	var ca = document.cookie.split(";");
	for(var ii=0;ii<ca.length;ii++) {
		var c = ca[ii];
		while (c.charAt(0)==' ') c = c.substring(1,c.length);
		if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
	}
	return null;
}

// TODO: will be easy ... just being lazy
function deleteCookie(nameString) {
}
