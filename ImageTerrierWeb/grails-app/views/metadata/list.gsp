
<%@ page import="org.imageterrier.webapp.Metadata" %>
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
		<meta name="layout" content="main" />
		<g:set var="entityName" value="${message(code: 'metadata.label', default: 'Metadata')}" />
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
						
							<g:sortableColumn property="id" title="${message(code: 'metadata.id.label', default: 'Id')}" />
						
							<th><g:message code="metadata.imageCollection.label" default="Collection" /></th>
						
							<g:sortableColumn property="data" title="${message(code: 'metadata.data.label', default: 'Data')}" />
						
							<th><g:message code="metadata.imageCollection.label" default="Image Collection" /></th>
						
							<g:sortableColumn property="imageTerrierId" title="${message(code: 'metadata.imageTerrierId.label', default: 'Image Terrier Id')}" />
						
							<g:sortableColumn property="imageURL" title="${message(code: 'metadata.imageURL.label', default: 'Image URL')}" />
						
						</tr>
					</thead>
					<tbody>
					<g:each in="${metadataInstanceList}" status="i" var="metadataInstance">
						<tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
						
							<td><g:link action="show" id="${metadataInstance.id}">${fieldValue(bean: metadataInstance, field: "id")}</g:link></td>
						
							<td>${fieldValue(bean: metadataInstance, field: "imageCollection")}</td>
						
							<td>${fieldValue(bean: metadataInstance, field: "data")}</td>
						
							<td>${fieldValue(bean: metadataInstance, field: "imageTerrierId")}</td>
						
							<td>${fieldValue(bean: metadataInstance, field: "imageURL")}</td>
						
						</tr>
					</g:each>
					</tbody>
				</table>
			</div>
			<div class="paginateButtons">
				<g:paginate total="${metadataInstanceTotal}" />
			</div>
		</div>
	</body>
</html>
