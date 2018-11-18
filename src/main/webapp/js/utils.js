function addContent(jsonHistory,location) {
    var arrayHistory = JSON.parse(jsonHistory);
    var table = document.getElementById("result_tbody");
    for(i=0;i<arrayHistory.length;i++) {

        var tr = document.createElement("tr");

        var tdCheck = document.createElement("td");
        var check = "<input name=\"chk-id\" id=\"chk-"+arrayHistory[i].id+"\" type=\"checkbox\" value=\""+arrayHistory[i].jenkinsMasterName+","+arrayHistory[i].name+","+arrayHistory[i].id+"\"/>";
        tdCheck.innerHTML = check;

        var tdId = document.createElement("td");
        var id = document.createTextNode(arrayHistory[i].id);
        tdId.appendChild(id);

        var tdName = document.createElement("td");
        var name = document.createTextNode(arrayHistory[i].name);
        tdName.appendChild(name);

        var tdStatus = document.createElement("td");
        var status = document.createTextNode(arrayHistory[i].status);
        tdStatus.appendChild(status);

        //Loop for all parameters
        var tdParameters = document.createElement("td");
        var myParameters = "";
        if(typeof arrayHistory[i].parameters != 'undefined' && arrayHistory[i].parameters.parameters != 'undefined') {
            for(indParam=0;indParam<arrayHistory[i].parameters.length;indParam++) {
                for(indParam2=0;indParam2<arrayHistory[i].parameters[indParam].parameters.length;indParam2++) {
                    myParameters = "<pre>"+myParameters + arrayHistory[i].parameters[indParam].parameters[indParam2].name+" : "+ arrayHistory[i].parameters[indParam].parameters[indParam2].value+"<pre>"
                }
            }
            tdParameters.innerHTML = myParameters;
        }
        var tdMaster = document.createElement("td");
        var master = document.createTextNode(arrayHistory[i].jenkinsMasterName);
        tdMaster.appendChild(master);

        var tdExecutedOn = document.createElement("td");
        if(typeof arrayHistory[i].executedOn != 'undefined' && arrayHistory[i].executedOn != 'undefined') {
            var executedOn = document.createTextNode(arrayHistory[i].executedOn);
            tdExecutedOn.appendChild(executedOn);
        }

        var tdLog = document.createElement("td");
        var logLink = "<td><a href=\"getLog?id="+arrayHistory[i].id+"_"+ arrayHistory[i].projectId+"_"+ arrayHistory[i].jenkinsMasterId+"\"><img src=\""+rootUrl+"/plugin/elasticjenkins/36x36/log_icon.png\" ></img></a></td>"
        tdLog.innerHTML = logLink;

        tr.appendChild(tdCheck);
        tr.appendChild(tdId);
        tr.appendChild(tdName);
        tr.appendChild(tdStatus);
        tr.appendChild(tdParameters);
        tr.appendChild(tdMaster);
        tr.appendChild(tdExecutedOn);
        tr.appendChild(tdLog);
        if(location == "after") {
            table.appendChild(tr);
        }else{
            table.insertBefore(tr,table.childNodes[0]);
        }

    }
}


function addContentPanel(json,nameType) {
    var array = JSON.parse(json);
     var panel_name = "tbody_"+nameType+"_id";
    var divNoResult = document.getElementById("div_"+nameType+"_no_result");
    var divContent = document.getElementById(panel_name);

    if(array.length == 0) {
        divNoResult.removeAttribute("style");
        divContent.style.display = "none";
        return;
    }
    divNoResult.style.display = "none";
    divContent.style.display = "block";

    var newTbody = document.createElement("tbody");
        newTbody.setAttribute("name",panel_name);
        newTbody.setAttribute("id",panel_name);

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
    newTbody.appendChild(trHeader);
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
        newTbody.appendChild(tr);
    }

    var oldTbody = document.getElementById(panel_name);
    oldTbody.parentNode.replaceChild(newTbody, oldTbody);

}