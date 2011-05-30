
<%@ page import="org.imageterrier.webapp.MetadataDeserializer" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="layout" content="main" />
        <g:set var="entityName" value="${message(code: 'metadataDeserializer.label', default: 'MetadataDeserializer')}" />
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
                        
                            <g:sortableColumn property="id" title="${message(code: 'metadataDeserializer.id.label', default: 'Id')}" />
                        
                            <g:sortableColumn property="description" title="${message(code: 'metadataDeserializer.description.label', default: 'Description')}" />
                        
                            <g:sortableColumn property="groovyClosure" title="${message(code: 'metadataDeserializer.groovyClosure.label', default: 'Groovy Closure')}" />
                        
                            <g:sortableColumn property="name" title="${message(code: 'metadataDeserializer.name.label', default: 'Name')}" />
                        
                        </tr>
                    </thead>
                    <tbody>
                    <g:each in="${metadataDeserializerInstanceList}" status="i" var="metadataDeserializerInstance">
                        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        
                            <td><g:link action="show" id="${metadataDeserializerInstance.id}">${fieldValue(bean: metadataDeserializerInstance, field: "id")}</g:link></td>
                        
                            <td>${fieldValue(bean: metadataDeserializerInstance, field: "description")}</td>
                        
                            <td>${fieldValue(bean: metadataDeserializerInstance, field: "groovyClosure")}</td>
                        
                            <td>${fieldValue(bean: metadataDeserializerInstance, field: "name")}</td>
                        
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
            <div class="paginateButtons">
                <g:paginate total="${metadataDeserializerInstanceTotal}" />
            </div>
        </div>
    </body>
</html>
