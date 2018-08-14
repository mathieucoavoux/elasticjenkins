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

        var tdExecutedOn = document.createElement("td");
        var executedOn = document.createTextNode(array[i].executedOn);
        tdExecutedOn.appendChild(executedOn);

        tr.appendChild(tdCheck);
        tr.appendChild(tdId);
        tr.appendChild(tdName);
        tr.appendChild(tdStatus);
        tr.appendChild(tdParameters);
        tr.appendChild(tdExecutedOn);
        if(location == "after") {
            table.appendChild(tr);
        }else{
            table.insertBefore(tr,table.childNodes[0]);
        }

    }
}