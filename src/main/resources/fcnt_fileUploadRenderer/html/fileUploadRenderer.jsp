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
<c:set var="title" value="RV is back again"/>
<utility:logger level="info" value="FORMS ****** ${currentNode.identifier}"/>



<@ if (!Array.isArray(data.image)) { @>
    <@ if(data.image) { @>
        <a href="<@=data.url.replace('/repository/','/files/')@>" target="_blank">
            <img style="object-fit: contain;width: 80px;height: 80px;" src="<@=data.url.replace('/repository/','/files/')@>?t=thumbnail2"/>
        </a>
    <@} else {@>
        <a href="<@=data.url@>" target="_blank"><@=data.name@></a>
    <@}@>
<@ } else { @>
    <@ data.url.forEach(function(element, index){ @>
        <@ if(data.image[index]) { @>
         <a style="display: block"
            href="<@=element.replace('/repository/','/files/')@>" target="_blank">
             <img style="object-fit: contain;width: 80px;height: 80px;"src="<@=element.replace('/repository/','/files/')@>?t=thumbnail2"/>
         </a>
        <@} else { @>
           <a style="display: block" href="<@=element@>" target="_blank" title="${title}"><@=data.name[index]@></a>
        <@}@>
    <@ }) @>
<@ } @>
