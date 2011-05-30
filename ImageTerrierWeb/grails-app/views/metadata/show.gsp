
<%@ page import="org.imageterrier.webapp.Metadata" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="layout" content="main" />
        <g:set var="entityName" value="${message(code: 'metadata.label', default: 'Metadata')}" />
        <title><g:message code="default.show.label" args="[entityName]" /></title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></span>
            <span class="menuButton"><g:link class="list" action="list"><g:message code="default.list.label" args="[entityName]" /></g:link></span>
            <span class="menuButton"><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></span>
        </div>
        <div class="body">
            <h1><g:message code="default.show.label" args="[entityName]" /></h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <div class="dialog">
                <table>
                    <tbody>
                    
                        <tr class="prop">
                            <td valign="top" class="name"><g:message code="metadata.id.label" default="Id" /></td>
                            
                            <td valign="top" class="value">${fieldValue(bean: metadataInstance, field: "id")}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name"><g:message code="metadata.collection.label" default="Collection" /></td>
                            
                            <td valign="top" class="value"><g:link controller="imageCollection" action="show" id="${metadataInstance?.collection?.id}">${metadataInstance?.collection?.encodeAsHTML()}</g:link></td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name"><g:message code="metadata.data.label" default="Data" /></td>
                            
                            <td valign="top" class="value">${fieldValue(bean: metadataInstance, field: "data")}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name"><g:message code="metadata.imageCollection.label" default="Image Collection" /></td>
                            
                            <td valign="top" class="value"><g:link controller="imageCollection" action="show" id="${metadataInstance?.imageCollection?.id}">${metadataInstance?.imageCollection?.encodeAsHTML()}</g:link></td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name"><g:message code="metadata.imageTerrierId.label" default="Image Terrier Id" /></td>
                            
                            <td valign="top" class="value">${fieldValue(bean: metadataInstance, field: "imageTerrierId")}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name"><g:message code="metadata.imageURL.label" default="Image URL" /></td>
                            
                            <td valign="top" class="value">${fieldValue(bean: metadataInstance, field: "imageURL")}</td>
                            
                        </tr>
                    
                    </tbody>
                </table>
            </div>
            <div class="buttons">
                <g:form>
                    <g:hiddenField name="id" value="${metadataInstance?.id}" />
                    <span class="button"><g:actionSubmit class="edit" action="edit" value="${message(code: 'default.button.edit.label', default: 'Edit')}" /></span>
                    <span class="button"><g:actionSubmit class="delete" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" /></span>
                </g:form>
            </div>
        </div>
    </body>
</html>
