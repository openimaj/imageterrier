
<%@ page import="org.imageterrier.webapp.ImageTerrierIndex" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="layout" content="main" />
        <g:set var="entityName" value="${message(code: 'imageTerrierIndex.label', default: 'ImageTerrierIndex')}" />
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
                        
                            <g:sortableColumn property="id" title="${message(code: 'imageTerrierIndex.id.label', default: 'Id')}" />
                        
                            <g:sortableColumn property="description" title="${message(code: 'imageTerrierIndex.description.label', default: 'Description')}" />
                        
                            <g:sortableColumn property="indexPath" title="${message(code: 'imageTerrierIndex.indexPath.label', default: 'Index Path')}" />
                        
                            <g:sortableColumn property="name" title="${message(code: 'imageTerrierIndex.name.label', default: 'Name')}" />
                        
                        </tr>
                    </thead>
                    <tbody>
                    <g:each in="${imageTerrierIndexInstanceList}" status="i" var="imageTerrierIndexInstance">
                        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        
                            <td><g:link action="show" id="${imageTerrierIndexInstance.id}">${fieldValue(bean: imageTerrierIndexInstance, field: "id")}</g:link></td>
                        
                            <td>${fieldValue(bean: imageTerrierIndexInstance, field: "description")}</td>
                        
                            <td>${fieldValue(bean: imageTerrierIndexInstance, field: "indexPath")}</td>
                        
                            <td>${fieldValue(bean: imageTerrierIndexInstance, field: "name")}</td>
                        
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
            <div class="paginateButtons">
                <g:paginate total="${imageTerrierIndexInstanceTotal}" />
            </div>
        </div>
    </body>
</html>
