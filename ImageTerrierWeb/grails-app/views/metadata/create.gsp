

<%@ page import="org.imageterrier.webapp.Metadata" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="layout" content="main" />
        <g:set var="entityName" value="${message(code: 'metadata.label', default: 'Metadata')}" />
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
            <g:hasErrors bean="${metadataInstance}">
            <div class="errors">
                <g:renderErrors bean="${metadataInstance}" as="list" />
            </div>
            </g:hasErrors>
            <g:form action="save" >
                <div class="dialog">
                    <table>
                        <tbody>
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="collection"><g:message code="metadata.collection.label" default="Collection" /></label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean: metadataInstance, field: 'collection', 'errors')}">
                                    <g:select name="collection.id" from="${org.imageterrier.webapp.ImageCollection.list()}" optionKey="id" value="${metadataInstance?.collection?.id}"  />
                                </td>
                            </tr>
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="data"><g:message code="metadata.data.label" default="Data" /></label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean: metadataInstance, field: 'data', 'errors')}">
                                    <g:textField name="data" value="${metadataInstance?.data}" />
                                </td>
                            </tr>
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="imageCollection"><g:message code="metadata.imageCollection.label" default="Image Collection" /></label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean: metadataInstance, field: 'imageCollection', 'errors')}">
                                    <g:select name="imageCollection.id" from="${org.imageterrier.webapp.ImageCollection.list()}" optionKey="id" value="${metadataInstance?.imageCollection?.id}"  />
                                </td>
                            </tr>
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="imageTerrierId"><g:message code="metadata.imageTerrierId.label" default="Image Terrier Id" /></label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean: metadataInstance, field: 'imageTerrierId', 'errors')}">
                                    <g:textField name="imageTerrierId" value="${metadataInstance?.imageTerrierId}" />
                                </td>
                            </tr>
                        
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="imageURL"><g:message code="metadata.imageURL.label" default="Image URL" /></label>
                                </td>
                                <td valign="top" class="value ${hasErrors(bean: metadataInstance, field: 'imageURL', 'errors')}">
                                    <g:textField name="imageURL" value="${metadataInstance?.imageURL}" />
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
