/*

This file contains functions for visual effects that are independent
of form elements on the default demo pages. Custom demos should be
able to use these easily.

*/

//** non-functional visual effects and layout **//
function togglePanels(closed, open) {
	toggleHidden(closed);
	toggleHidden(open);
}
function toggleHidden(elem) {
	if (elem.style.display != "none") {
		elem.style.display = "none";
		return true;
	}
	
	elem.style.display = "block";
	return false;
}
function hide(elem) {
	elem.style.display = "none";
}
function show(elem) {
	elem.style.display = "block";
}

function toggleCollapse(elem, link) {
	if (toggleHidden(elem))
		link.innerHTML="<img src=\"images/mini_expand.png\">";
	else
		link.innerHTML="<img src=\"images/mini_collapse.png\">";
	link.blur();
}

// simple code to enable resizing of elements
var resizeElem;
var panelElem;
var MAGIC_ADJUST;
function doResize(e) {
	// only resize if CTRL key is being held
	if (heldKeyId==17) {
		resizeElem.style.width = e.pageX - resizeElem.offsetLeft;
		resizeElem.style.height = e.pageY - resizeElem.offsetTop;

		// FIXME: keep the content height matched to the containing div height
		//        no way to do with normal css right now ...
		//        So using a horrid magical adjustment passed in from HTML page
		if (panelElem)
			panelElem.style.width = e.pageX - resizeElem.offsetLeft + MAGIC_ADJUST;
	}
}
var heldKeyId;
function saveKeypress(e) {
	heldKeyId=e.keyCode;
}
function clearKeypress(e) {
	heldKeyId=null;
}
function startResize(elem, outer, magicAdjust) {
	document.onmousemove = doResize;
	document.onkeydown=saveKeypress;
	document.onkeyup=clearKeypress;
	resizeElem=elem;
	document.onmouseup = stopResize;

	panelElem = outer;
	MAGIC_ADJUST = magicAdjust;
}
function stopResize() {
	document.onmousemove = null;
	resizeElem=null;
}

function showError(errString) {
	alert(errString);
}

var highlightBgColour="#ffcc00";
var highlightBorderColour="#ff6600";
var oldBgColour;
var oldBorderColour;
function highlight(elem) {
	oldBgColour=elem.style.backgroundColor;
	elem.style.backgroundColor=highlightBgColour;

	oldBorderColour=elem.style.borderColor;
	elem.style.borderColor=highlightBorderColour;
}
function unhighlight(elem) {
	elem.style.backgroundColor=oldBgColour;
	elem.style.borderColor=oldBorderColour;
}
