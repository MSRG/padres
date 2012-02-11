/*

This file contains functions for working with form elements in the
default demo pages. Custom demos can reuse these if they wish but
should ideally create their own form elements with their own behaviour.

*/




// monitor name text
var monitorNameElem;
function setMonitorNameElem(elem) {
	monitorNameElem = elem;
}
function setMonitorNameText(name) {
	if (monitorNameElem)
		monitorNameElem.innerHTML=name;
}
// page title
var pageTitleElem;
function setPageTitleElem(elem) {
	pageTitleElem = elem;
}

var notificationArea;
function setNotificationArea(elem) {
	notificationArea=elem;
}
function addNotification(id, content) {
	if (notificationArea) {
		notificationArea.value=
		id + ": " + content
		+ "\n"
		+ notificationArea.value;
	}
}

// textareas

function clearText(textElem) {
	textElem.value="";
}