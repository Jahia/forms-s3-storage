<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>

<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="flowRequestContext" type="org.springframework.webflow.execution.RequestContext"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<template:addResources type="inlinejavascript">
    <script>
        var apiName = "rawResults";

        //Predefined URL parts and URL Base
        var API_URL_START = 'modules/forms/results';
        var context = "${url.context}";
        var locale = "${currentResource.locale}";

        var apiURLBase = context + "/" + API_URL_START + "/" + locale + "/";

        //Listing API entries in an object
        var apiMethods = {
          "results":function(parameters){
              if(parameters.fromDate!=null && parameters.toDate!=null){
                  return apiURLBase + parameters.formId + "/from/" + parameters.fromDate + "/to/" + parameters.toDate + "/results";
              }
              else{
                  return apiURLBase + parameters.formId + "/results";
              }
          },
          "results_submissiontime":function(parameters){
              return apiURLBase + parameters.formId + "/results/submissiontime";
          },
          "results_submissionpage":function(parameters){
              return apiURLBase + parameters.formId + "/results/submissionpage";
          },
          "results_submissionempty":function(parameters){
              return apiURLBase + parameters.formId + "/results/submissionempty";
          },
          "results_removeselectedsubmissions":function(parameters){
              return apiURLBase + parameters.formId + "/removeselectedsubmissions";
          },
            "results_getpermissions":function(parameters){
                return apiURLBase + parameters.siteId + "/getpermissions";
            },
          "results_choicelabel":function(parameters){
              return apiURLBase + parameters.formId + "/results/choicelabel";
          },
          "results_label":function(parameters){
              return apiURLBase + parameters.formId+"/results/labels";
          },
          "results_choice":function(parameters){
              return apiURLBase + parameters.formId+"/results/choice/"+parameters.choiceId;
          },
          "total":function(parameters){
              return "${url.context}/modules/forms/results/total/"+parameters.formId;
          },
          "lastDays":function(parameters){
              return "${url.context}/modules/forms/results/${renderContext.UILocale.language}/totallastdays/"+parameters.formId;
          },
          "groupTotal":function(parameters){
            return "${url.context}/modules/forms/results/${renderContext.UILocale.language}/groupTotal/"+parameters.formId;
            },
            "form_name": function(parameters) {return "${url.context}/modules/forms/results/${renderContext.UILocale.language}/formName/"+parameters.formId;}
        };

        //Generate an URL from parameters
        function getFetchURL(urlParameters) {
            return apiMethods[urlParameters.apiMethodName](urlParameters);
        }
    </script>
</template:addResources>
