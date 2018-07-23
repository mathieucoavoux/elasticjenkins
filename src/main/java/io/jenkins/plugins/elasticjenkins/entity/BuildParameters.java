package io.jenkins.plugins.elasticjenkins.entity;

import org.apache.http.entity.StringEntity;

import java.util.List;

public class BuildParameters {

    public List<Parameters> parameters;

    public List<String> safeParameters;

    public List<Parameters> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameters> parameters) {
        this.parameters = parameters;
    }

    public List<String> getSafeParameters() {
        return safeParameters;
    }

    public void setSafeParameters(List<String> safeParameters) {
        this.safeParameters = safeParameters;
    }

    public List<String> getParametersDefinitionNames() {
        return parametersDefinitionNames;
    }

    public void setParametersDefinitionNames(List<String> parametersDefinitionNames) {
        this.parametersDefinitionNames = parametersDefinitionNames;
    }

    public List<String> parametersDefinitionNames;



}
