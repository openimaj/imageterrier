

<%@ page import="org.imageterrier.webapp.ImageCollection" %>
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
		<meta name="layout" content="main" />
		<g:set var="entityName" value="${message(code: 'imageCollection.label', default: 'ImageCollection')}" />
		<title><g:message code="default.edit.label" args="[entityName]" /></title>
	</head>
	<body>
		<div class="nav">
			<span class="menuButton"><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></span>
			<span class="menuButton"><g:link class="list" action="list"><g:message code="default.list.label" args="[entityName]" /></g:link></span>
			<span class="menuButton"><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></span>
		</div>
		<div class="body">
			<h1><g:message code="default.edit.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
			<div class="message">${flash.message}</div>
			</g:if>
			<g:hasErrors bean="${imageCollectionInstance}">
			<div class="errors">
				<g:renderErrors bean="${imageCollectionInstance}" as="list" />
			</div>
			</g:hasErrors>
			<g:form method="post" >
				<g:hiddenField name="id" value="${imageCollectionInstance?.id}" />
				<g:hiddenField name="version" value="${imageCollectionInstance?.version}" />
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
								  <label for="metadata"><g:message code="imageCollection.metadata.label" default="Metadata" /></label>
								</td>
								<td valign="top" class="value ${hasErrors(bean: imageCollectionInstance, field: 'metadata', 'errors')}">
									
<ul>
<g:each in="${imageCollectionInstance?.metadata?}" var="m">
	<li><g:link controller="metadata" action="show" id="${m.id}">${m?.encodeAsHTML()}</g:link></li>
</g:each>
</ul>
<g:link controller="metadata" action="create" params="['imageCollection.id': imageCollectionInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'metadata.label', default: 'Metadata')])}</g:link>

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
					<span class="button"><g:actionSubmit class="save" action="update" value="${message(code: 'default.button.update.label', default: 'Update')}" /></span>
					<span class="button"><g:actionSubmit class="delete" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" /></span>
				</div>
			</g:form>
		</div>
	</body>
</html>
