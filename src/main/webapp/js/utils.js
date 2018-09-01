function addContent(json_history,location) {

    var array_history = JSON.parse(json_history);
    var table = document.getElementById("result_tbody");
    for(i=0;i<array_history.length;i++) {

        var tr = document.createElement("tr");

        var tdCheck = document.createElement("td");
        var check = "<input name=\"chk-id\" id=\"chk-"+array_history[i].id+"\" type=\"checkbox\" value=\""+array_history[i].jenkinsMasterName+","+array_history[i].name+","+array_history[i].id+"\"/>";
        tdCheck.innerHTML = check;

        var tdId = document.createElement("td");
        var id = document.createTextNode(array_history[i].id);
        tdId.appendChild(id);

        var tdName = document.createElement("td");
        var name = document.createTextNode(array_history[i].name);
        tdName.appendChild(name);

        var tdStatus = document.createElement("td");
        var status = document.createTextNode(array_history[i].status);
        tdStatus.appendChild(status);

        //Loop for all parameters
        var myParameters = "";
        if(typeof array_history[i].parameters != 'undefined') {
            for(indParam=0;indParam<array_history[i].parameters.length;indParam++) {
                myParameters = "<pre>"+myParameters + array_history[i].parameters[indParam].name+" : "+myParameters + array_history[i].parameters[indParam].value+"<pre>"
            }
            var tdParameters = document.createElement("td");
            tdParameters.innerHTML = myParameters;
        }
        var tdMaster = document.createElement("td");
        var master = document.createTextNode(array_history[i].jenkinsMasterName);
        tdMaster.appendChild(master);

        var tdExecutedOn = document.createElement("td");
        var executedOn = document.createTextNode(array_history[i].executedOn);
        tdExecutedOn.appendChild(executedOn);

        tr.appendChild(tdCheck);
        tr.appendChild(tdId);
        tr.appendChild(tdName);
        tr.appendChild(tdStatus);
        if(typeof array_history[i].parameters != 'undefined') {
            tr.appendChild(tdParameters);
        }
        tr.appendChild(tdMaster);
        tr.appendChild(tdExecutedOn);
        if(location == "after") {
            table.appendChild(tr);
        }else{
            table.insertBefore(tr,table.childNodes[0]);
        }

    }
}


function addContentPanel(json,name_type) {
    var array = JSON.parse(json);
     var panel_name = "tbody_"+name_type+"_id";
    var divNoResult = document.getElementById("div_"+name_type+"_no_result");
    var divContent = document.getElementById(panel_name);

    if(array.length == 0) {
        divNoResult.removeAttribute("style");
        divContent.style.display = "none";
        return;
    }
    divNoResult.style.display = "none";
    divContent.style.display = "block";

    var new_tbody = document.createElement("tbody");
        new_tbody.setAttribute("name",panel_name);
        new_tbody.setAttribute("id",panel_name);

    var trHeader = document.createElement("tr");
    var thId = document.createElement("th");
    var hId = '<th class="pane" colspan="1">N.</th>'
    thId.innerHTML = hId;
    var thName = document.createElement("th");
    var hName = '<th class="pane" colspan="1">Name</th>'
    thName.innerHTML = hName;
    var thMaster = document.createElement("th");
    var hMaster = '<th class="pane" colspan="1">Master</th>'
    thMaster.innerHTML = hMaster;
    trHeader.appendChild(thId);
    trHeader.appendChild(thName);
    trHeader.appendChild(thMaster);
    new_tbody.appendChild(trHeader);
    for(i=0;i<array.length;i++) {
        var tr = document.createElement("tr");

        var tdId = document.createElement("td");
        var id = document.createTextNode(array[i].id);
        tdId.appendChild(id);

        var tdName = document.createElement("td");
        var name = document.createTextNode(array[i].name);
        tdName.appendChild(name);

        var tdMaster = document.createElement("td");
        var master = document.createTextNode(array[i].jenkinsMasterName);
        tdMaster.appendChild(master);

        tr.appendChild(tdId);
        tr.appendChild(tdName);
        tr.appendChild(tdMaster);
        new_tbody.appendChild(tr);
    }

    var old_tbody = document.getElementById(panel_name);
    old_tbody.parentNode.replaceChild(new_tbody, old_tbody);

}