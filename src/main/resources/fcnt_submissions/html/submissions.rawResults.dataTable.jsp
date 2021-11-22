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
<template:addResources type="css" resources="results.css"/>
<template:addResources type="css" resources="lib/font-awesome/css/font-awesome.css"/>
<template:addResources type="css" resources="languageSwitchingLinks.css"/>

<template:addResources type="javascript" resources="fcResultsUtils.js"/>
<template:addResources type="javascript" resources="collections/results.js"/>
<template:addResources type="javascript" resources="lib/backbone.paginator.min.js"/>
<template:addResources type="javascript" resources="main-results.js"/>
<template:addResources type="javascript" resources="main-results-fix.js"/>

<fmt:message key="fcnt.dataTableNoResult" var="dataTableNoResult"/>
<fmt:message key="fcnt_result.username" var="usernameHeader"/>
<fmt:message key="fcnt_result.submissionDate" var="subDateHeader"/>
<fmt:message key="fcnt_result.origin" var="originHeader"/>
<fmt:message key="fcnt_result.remoteAddress" var="remoteAddressHeader"/>
<fmt:message key="fcnt_result.submissions.total" var="submissionsTotal"/>
<fmt:message key="fcnt_result.submissions.average" var="submissionsAverage"/>
<fmt:message key="fcnt_result.submissions.last" var="submissionsLast"/>
<fmt:message key="label.days" var="labelDays"/>
<fmt:message key="label.week" var="labelWeek"/>x
<fmt:message key="label.month" var="labelMonth"/>
<fmt:message key="label.year" var="labelYear"/>

<template:addResources type="inlinejavascript">
    <script>

        var jsVarMap = {
            dataTableNoResult  : '${functions:escapeJavaScript(dataTableNoResult)}',
            usernameHeader     : '${functions:escapeJavaScript(usernameHeader)}',
            subDateHeader      : '${functions:escapeJavaScript(subDateHeader)}',
            originHeader       : '${functions:escapeJavaScript(originHeader)}',
            remoteAddressHeader: '${functions:escapeJavaScript(remoteAddressHeader)}',
            submissionsTotal   : '${functions:escapeJavaScript(submissionsTotal)}',
            submissionsAverage : '${functions:escapeJavaScript(submissionsAverage)}',
            submissionsLast    : '${functions:escapeJavaScript(submissionsLast)}',
            labelDays          : '${functions:escapeJavaScript(labelDays)}',
            labelWeek          : '${functions:escapeJavaScript(labelWeek)}',
            labelMonth         : '${functions:escapeJavaScript(labelMonth)}',
            labelYear          : '${functions:escapeJavaScript(labelYear)}'
        };
        labels = [];
        labels.push({
            label : '<input type="checkbox">',
            htmlId : 'selected'
        }, {
            label : jsVarMap.usernameHeader,
            htmlId: 'username'
        }, {
            label : jsVarMap.subDateHeader,
            htmlId: 'submissionDate'
        }, {
            label : jsVarMap.originHeader,
            htmlId: 'origin'
        }, {
            label : jsVarMap.remoteAddressHeader,
            htmlId: 'remoteAddress'
        });
        var API_URL = '${url.context}/modules/forms/results/${currentResource.locale}';
        // Initializing backbone objects
        // Initializing backbone objects
        var dataCollection = new PageableResults();
        dataCollection.site = "${renderContext.site.name}";
        dataCollection.formId = selectedResult;
        moment.locale('${renderContext.UILocale.language}');
        $.fn.dataTable.moment('LLL');

        $(document).ready(function () {
            // Initialize date pickers
            var option = {
                format       : "dd/mm/yyyy",
                todayBtn     : true,
                language     : '${renderContext.UILocale}',
                calendarWeeks: true,
                autoclose    : true,
                orientation  : "bottom auto"
            };
            $("#dateRange").find(".input-daterange").datepicker(option);
            // Creating results Backbone view now the page is loaded
            /*var dataCollectionView = new DatatableResultsView({collection: dataCollection});*/
            var dataCollectionView  = new PageableResultsView({
                collection      : dataCollection,
                grid_el         : $("#formDataTable"),
                searchLabel     : "<fmt:message key="label.search"/>",
                resultsFromLabel: "<fmt:message key="fcnt_result.resultsFrom"/>",
                resultsToLabel  : "<fmt:message key="fcnt_result.resultsTo"/>",
                noResultLabel   : "<fmt:message key="fcnt.dataTableNoResult"/>",
                exportLabel     : "<fmt:message key="label.export"/>",
                copyLabel       : "<fmt:message key="label.copy"/>",
                printLabel      : "<fmt:message key="label.print"/>",
                showHideColumnsLabel   : "<fmt:message key="ff.label.showHideColumns"/>",
                showAllColumnsLabel   : "<fmt:message key="ff.label.showAllColumns"/>",
                swfUrl          : "${url.context}" + urlCurrentModule + "/swf/copy_csv_xls_pdf.swf",
                printToolTipText: "<fmt:message key="fcnt_result.datatable.print.tooltipText"/>",
                printInfoText   : "<fmt:message key="fcnt_result.datatable.print.infoText"/>"
            });
            dataCollection.fromDate = moment(formModel.get("created")).subtract(3,'months');
            new TotalAverageView({collection: dataCollection});
            var lastScroll = 0;
            // Getting data and updating the view
            dataCollection.fetchingPage = true;
            dataCollection.getFirstPage({
                success: function () {
                    dataCollection.fetchingPage = false;
                    setTimeout(function() {
                        $("#formDataTable_wrapper").find(".dataTables_scrollBody").scroll(function (options) {
                            var scrollBody    = $(options.currentTarget);
                            var scrollTop     = scrollBody.scrollTop();
                            var scrollHeight  = scrollBody.prop('scrollHeight');
                            var scrollPercent = Math.round((scrollTop * 100) / (scrollHeight - 200));
                            if (scrollPercent >= 60 && scrollPercent > lastScroll && !dataCollection.fetchingPage) {
                                dataCollection.fetchingPage = true;
                                dataCollection.getNextPage({
                                    success: function () {
                                        dataCollection.fetchingPage = false;
                                    }
                                });
                            }
                            lastScroll = scrollPercent;
                        });
                    }, 1000);
                }
            });
            $("#columnPicker").click(function () {
                var $columnschoice = $("#columnschoice");
                $columnschoice.toggle();
                var $columnPicker = $("#columnPicker");
                if ($columnschoice.is(':visible')) {
                    $columnPicker.removeClass("down");
                    $columnPicker.addClass("up");
                } else {
                    $columnPicker.removeClass("up");
                    $columnPicker.addClass("down");
                }
            })
        });
    </script>
</template:addResources>


<style>
    a.hack-link{
        max-width: 300px;
        text-overflow: ellipsis;
        overflow: hidden;
    }
    a.hack-float {
        float: left;
        margin-right: 5px;
        margin-bottom: 5px;
    }
    a.hack-float:nth-child(4n + 4) {
        clear: left;
    }
</style>


<div class="row">
    <%@include file="totalaverage.jspf" %>
    <div class="col-sm-6">
        <form id="dateRange" class="form-inline">
            <fieldset>
                <legend>
                    <fmt:message key="fcnt_result.datePicker.legend"/>
                    <c:choose>
                        <c:when test="${renderContext.editMode}">
                            <a class="btn btn-primary pull-right"
                               href="${url.context}${fn:replace(url.baseEdit, '/edit/', '/editframe/')}${renderContext.site.path}.results.html"><i
                                    class="fa fa-home"></i></a>
                        </c:when>
                        <c:otherwise>
                            <a class="btn btn-primary pull-right"
                               href="${url.context}${url.baseLive}${renderContext.site.path}/formFactory.html"><i
                                    class="fa fa-home"></i></a>
                        </c:otherwise>
                    </c:choose>
                </legend>
                <div class="form-group">
                    <div class="input-daterange input-group">
                        <input type="text" class="form-control" name="from"/>
                        <span class="input-group-addon">to</span>
                        <input type="text" class="form-control" name="to"/>
                    </div>
                </div>
                <button type="button" class="btn btn-primary" onclick="filterResults()">
                    <fmt:message key="fcnt_result.filter"/>
                </button>
            </fieldset>
        </form>
    </div>
</div>
<table class="table table-striped table-bordered table-condensed nowrap" cellspacing="0" width="100%" id="formDataTable">
</table>
