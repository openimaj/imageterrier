

<%@ page import="org.imageterrier.webapp.ImageCollection" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="layout" content="main" />
        <g:set var="entityName" value="${message(code: 'imageCollection.label', default: 'ImageCollection')}" />
        <title><g:message code="default.create.label" args="[entityName]" /></title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></span>
            <span class="menuButton"><g:link class="list" action="list"><g:message code="default.list.label" args="[entityName]" /></g:link></span>
        </div>
        <div class="body">
            <h1><g:message code="default.create.label" args="[entityName]" /></h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <g:hasErrors bean="${imageCollectionInstance}">
            <div class="errors">
                <g:renderErrors bean="${imageCollectionInstance}" as="list" />
            </div>
            </g:hasErrors>
            <g:form action="save" >
                <div class="dialog">
                    <table>
                        <tbody>
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="description"><g:message code="imageCollection.description.label" default="Description" /></label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean: imageCollectionInstance, field: 'description', 'errors')}">
                                    <g:textField name="description" value="${imageCollectionInstance?.description}" />
                                </td>
                            </tr>
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="deserializer"><g:message code="imageCollection.deserializer.label" default="Deserializer" /></label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean: imageCollectionInstance, field: 'deserializer', 'errors')}">
                                    <g:select name="deserializer.id" from="${org.imageterrier.webapp.MetadataDeserializer.list()}" optionKey="id" value="${imageCollectionInstance?.deserializer?.id}"  />
                                </td>
                            </tr>
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="name"><g:message code="imageCollection.name.label" default="Name" /></label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean: imageCollectionInstance, field: 'name', 'errors')}">
                                    <g:textField name="name" value="${imageCollectionInstance?.name}" />
                                </td>
                            </tr>
                        
                        </tbody>
                    </table>
                </div>
                <div class="buttons">
                    <span class="button"><g:submitButton name="create" class="save" value="${message(code: 'default.button.create.label', default: 'Create')}" /></span>
                </div>
            </g:form>
        </div>
    </body>
</html>
