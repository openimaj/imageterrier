
<%@ page import="org.imageterrier.webapp.ResultsProcessor" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="layout" content="main" />
        <g:set var="entityName" value="${message(code: 'resultsProcessor.label', default: 'ResultsProcessor')}" />
        <title><g:message code="default.list.label" args="[entityName]" /></title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></span>
            <span class="menuButton"><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></span>
        </div>
        <div class="body">
            <h1><g:message code="default.list.label" args="[entityName]" /></h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <div class="list">
                <table>
                    <thead>
                        <tr>
                        
                            <g:sortableColumn property="id" title="${message(code: 'resultsProcessor.id.label', default: 'Id')}" />
                        
                            <g:sortableColumn property="description" title="${message(code: 'resultsProcessor.description.label', default: 'Description')}" />
                        
                            <g:sortableColumn property="groovyClosure" title="${message(code: 'resultsProcessor.groovyClosure.label', default: 'Groovy Closure')}" />
                        
                            <g:sortableColumn property="name" title="${message(code: 'resultsProcessor.name.label', default: 'Name')}" />
                        
                            <g:sortableColumn property="shortName" title="${message(code: 'resultsProcessor.shortName.label', default: 'Short Name')}" />
                        
                        </tr>
                    </thead>
                    <tbody>
                    <g:each in="${resultsProcessorInstanceList}" status="i" var="resultsProcessorInstance">
                        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        
                            <td><g:link action="show" id="${resultsProcessorInstance.id}">${fieldValue(bean: resultsProcessorInstance, field: "id")}</g:link></td>
                        
                            <td>${fieldValue(bean: resultsProcessorInstance, field: "description")}</td>
                        
                            <td> ... block of code ... <%fieldValue(bean: resultsProcessorInstance, field: "groovyClosure")%></td>
                        
                            <td>${fieldValue(bean: resultsProcessorInstance, field: "name")}</td>
                        
                            <td>${fieldValue(bean: resultsProcessorInstance, field: "shortName")}</td>
                        
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
            <div class="paginateButtons">
                <g:paginate total="${resultsProcessorInstanceTotal}" />
            </div>
        </div>
    </body>
</html>
