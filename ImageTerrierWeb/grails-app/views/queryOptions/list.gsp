
<%@ page import="org.imageterrier.webapp.QueryOptions" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="layout" content="main" />
        <g:set var="entityName" value="${message(code: 'queryOptions.label', default: 'QueryOptions')}" />
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
                        
                            <g:sortableColumn property="id" title="${message(code: 'queryOptions.id.label', default: 'Id')}" />
                        
                            <g:sortableColumn property="description" title="${message(code: 'queryOptions.description.label', default: 'Description')}" />
                        
                            <g:sortableColumn property="name" title="${message(code: 'queryOptions.name.label', default: 'Name')}" />
                        
                            <g:sortableColumn property="options" title="${message(code: 'queryOptions.options.label', default: 'Options')}" />
                        
                        </tr>
                    </thead>
                    <tbody>
                    <g:each in="${queryOptionsInstanceList}" status="i" var="queryOptionsInstance">
                        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        
                            <td><g:link action="show" id="${queryOptionsInstance.id}">${fieldValue(bean: queryOptionsInstance, field: "id")}</g:link></td>
                        
                            <td>${fieldValue(bean: queryOptionsInstance, field: "description")}</td>
                        
                            <td>${fieldValue(bean: queryOptionsInstance, field: "name")}</td>
                        
                            <td>${fieldValue(bean: queryOptionsInstance, field: "options")}</td>
                        
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
            <div class="paginateButtons">
                <g:paginate total="${queryOptionsInstanceTotal}" />
            </div>
        </div>
    </body>
</html>
