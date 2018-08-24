function addContent(json,location) {
    var array = JSON.parse(json);
    var table = document.getElementById("result_tbody");
    for(i=0;i<array.length;i++) {

        var tr = document.createElement("tr");

        var tdCheck = document.createElement("td");
        var check = "<input name=\"chk-id\" id=\"chk-"+array[i].id+"\" type=\"checkbox\" value=\""+array[i].jenkinsMasterName+","+array[i].name+","+array[i].id+"\"/>";
        tdCheck.innerHTML = check;

        var tdId = document.createElement("td");
        var id = document.createTextNode(array[i].id);
        tdId.appendChild(id);

        var tdName = document.createElement("td");
        var name = document.createTextNode(array[i].name);
        tdName.appendChild(name);

        var tdStatus = document.createElement("td");
        var status = document.createTextNode(array[i].status);
        tdStatus.appendChild(status);

        //Loop for all parameters
        var myParameters = "";
        for(indParam=0;indParam<array[i].parameters.length;indParam++) {
            myParameters = "<pre>"+myParameters + array[i].parameters[indParam].name+" : "+myParameters + array[i].parameters[indParam].value+"<pre>"
        }
        var tdParameters = document.createElement("td");
        tdParameters.innerHTML = myParameters;

        var tdMaster = document.createElement("td");
        var master = document.createTextNode(array[i].jenkinsMasterName);
        tdMaster.appendChild(master);

        var tdExecutedOn = document.createElement("td");
        var executedOn = document.createTextNode(array[i].executedOn);
        tdExecutedOn.appendChild(executedOn);

        tr.appendChild(tdCheck);
        tr.appendChild(tdId);
        tr.appendChild(tdName);
        tr.appendChild(tdStatus);
        tr.appendChild(tdParameters);
        tr.appendChild(tdMaster);
        tr.appendChild(tdExecutedOn);
        if(location == "after") {
            table.appendChild(tr);
        }else{
            table.insertBefore(tr,table.childNodes[0]);
        }

    }
}

function addContentSpan(json) {
}

function addContentPanel(json,panel_name) {

    var array = JSON.parse(json);

    var new_tbody = document.createElement("tbody");
    new_tbody.setAttribute("name",panel_name);
    new_tbody.setAttribute("id",panel_name);
    for(i=0;i<array.length;i++) {
        var tr = document.createElement("tr");

        var tdIcon = document.createElement("td");
        if(panel_name == "tbody_build_id") {
            var icon_name = "blue_anime.gif";
        }else{
            var icon_name = "grey.png";
        }
        var icon = "<img src=\"images/24x24/"+icon_name+"\"></img>";
        tdIcon.innerHTML = icon;

        var tdName = document.createElement("td");
        var name = document.createTextNode(array[i].name);
        tdName.appendChild(name);

        var tdMaster = document.createElement("td");
        var master = document.createTextNode(array[i].jenkinsMasterName);
        tdMaster.appendChild(master);

        tr.appendChild(tdIcon);
        tr.appendChild(tdName);
        tr.appendChild(tdMaster);
        new_tbody.appendChild(tr);
    }

    var old_tbody = document.getElementById(panel_name);
    old_tbody.parentNode.replaceChild(new_tbody, old_tbody);

}