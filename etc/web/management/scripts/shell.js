var consoleElem;

function setConsole(elem){
    consoleElem = elem;
}

function printToConsole(string){
    if (consoleElem) {
        consoleElem.value += string + "\n";
        // keep focus on new text that's added
        consoleElem.scrollTop = consoleElem.scrollHeight;
    }
}

function clearConsole(){
    consoleElem.value = "";
}

var COMMAND_CLS = "cls";

//TODO: Command history
var MAX_HISTORY_LEN = 128;
var commandHistory = new Array();
var seekIdx = 0;
var currIdx = 0;
function processKeyDown(e, cmd_line){
    // Enter key
    if (e.keyCode == 13) {
        // TODO: More robust command line parsing
        var words = cmd_line.value.replace("\\s+", " ").split(" ");
        var command = words[0];
        printToConsole("\n>>> " + cmd_line.value);
        
        // rebuild args ... yeah, this is a bit silly
        var args = "";
        var ii;
        for (ii = 1; ii < words.length; ii++) {
            if (args != "") 
                args += " ";
            args += words[ii];
        }
        
        if (command == COMMAND_CLS) {
            clearConsole();
        } else {
            runCommand(command, args, processResults);
        }
        
        updateCommandHistory();
        currIdx = (currIdx + 1) % MAX_HISTORY_LEN;
        seekIdx = currIdx;
        commandHistory[currIdx] = "";
        
        cmd_line.value = "";
        updateCommandHistory();
    } else if (e.keyCode == 38) {
        // Up arrow
        var idx = seekIdx - 1;
        if (idx < 0) {
            idx = MAX_HISTORY_LEN - 1;
        }
        if (commandHistory[idx] != null) {
            seekIdx = idx;
            cmd_line.value = commandHistory[seekIdx];
        }
    } else if (e.keyCode == 40) {
        // Down arrow
        var idx = (seekIdx + 1) % MAX_HISTORY_LEN;
        if (commandHistory[idx] != null) {
            seekIdx = idx;
            cmd_line.value = commandHistory[seekIdx];
        }
    }
}

function processKeyUp(e, cmd_line){
    if (e.keyCode != 13 && e.keyCode != 38 && e.keyCode != 40) {
        updateCommandHistory();
    }
}

function updateCommandHistory(){
    commandHistory[currIdx] = cmd_line.value;
    // Always keep the next element undefined so we know where the history begins while seeking
    var idx = (currIdx + 1) % MAX_HISTORY_LEN;
    commandHistory[idx] = null;
}

function processResults(xmlHttp){
    var doc = xmlToDoc(xmlHttp.responseText);
    var nodes = doc.firstChild.childNodes;
    var ii;
    for (ii = 0; ii < nodes.length; ii++) {
        var node = nodes[ii];
        printToConsole(node.nodeName + ": " + node.firstChild.nodeValue);
    }
    cleanup(xmlHttp);
}
