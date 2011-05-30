
<%@ page import="org.imageterrier.webapp.MetadataImporter" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="layout" content="main" />
        <g:set var="entityName" value="${message(code: 'metadataImporter.label', default: 'MetadataImporter')}" />
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
                        
                            <g:sortableColumn property="id" title="${message(code: 'metadataImporter.id.label', default: 'Id')}" />
                        
                            <g:sortableColumn property="description" title="${message(code: 'metadataImporter.description.label', default: 'Description')}" />
                        
                            <g:sortableColumn property="groovyClosure" title="${message(code: 'metadataImporter.groovyClosure.label', default: 'Groovy Closure')}" />
                        
                            <g:sortableColumn property="name" title="${message(code: 'metadataImporter.name.label', default: 'Name')}" />
                        
                        </tr>
                    </thead>
                    <tbody>
                    <g:each in="${metadataImporterInstanceList}" status="i" var="metadataImporterInstance">
                        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        
                            <td><g:link action="show" id="${metadataImporterInstance.id}">${fieldValue(bean: metadataImporterInstance, field: "id")}</g:link></td>
                        
                            <td>${fieldValue(bean: metadataImporterInstance, field: "description")}</td>
                        
                            <td>${fieldValue(bean: metadataImporterInstance, field: "groovyClosure")}</td>
                        
                            <td>${fieldValue(bean: metadataImporterInstance, field: "name")}</td>
                        
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
            <div class="paginateButtons">
                <g:paginate total="${metadataImporterInstanceTotal}" />
            </div>
        </div>
    </body>
</html>
