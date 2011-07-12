
<%@ page import="org.imageterrier.webapp.ImageCollection" %>
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
		<meta name="layout" content="main" />
		<g:set var="entityName" value="${message(code: 'imageCollection.label', default: 'ImageCollection')}" />
		<title><g:message code="default.show.label" args="[entityName]" /></title>
	</head>
	<body>
		<div class="nav">
			<span class="menuButton"><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></span>
			<span class="menuButton"><g:link class="list" action="list"><g:message code="default.list.label" args="[entityName]" /></g:link></span>
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
							<td valign="top" class="name"><g:message code="imageCollection.id.label" default="Id" /></td>
							
							<td valign="top" class="value">${fieldValue(bean: imageCollectionInstance, field: "id")}</td>
							
						</tr>
					
						<tr class="prop">
							<td valign="top" class="name"><g:message code="imageCollection.description.label" default="Description" /></td>
							
							<td valign="top" class="value">${fieldValue(bean: imageCollectionInstance, field: "description")}</td>
							
						</tr>
					
						<tr class="prop">
							<td valign="top" class="name"><g:message code="imageCollection.deserializer.label" default="Deserializer" /></td>
							
							<td valign="top" class="value"><g:link controller="metadataDeserializer" action="show" id="${imageCollectionInstance?.deserializer?.id}">${imageCollectionInstance?.deserializer?.encodeAsHTML()}</g:link></td>
							
						</tr>
					
						<tr class="prop">
							<td valign="top" class="name"><g:message code="imageCollection.index.label" default="Index" /></td>
							
							<td valign="top" style="text-align: left;" class="value">
								<ul>
								<li><g:link controller="imageTerrierIndex" action="show" id="${imageCollectionInstance.index.id}">${imageCollectionInstance.index?.encodeAsHTML()}</g:link></li>
								</ul>
							</td>
							
						</tr>
					
						<tr class="prop">
							<td valign="top" class="name"><g:message code="imageCollection.metadata.label" default="Metadata" /></td>
							
							<td valign="top" style="text-align: left;" class="value">
								<ul>
								<%
								if(imageCollectionInstance?.metadata!=null){
									int i = 0;
									for (m in imageCollectionInstance?.metadata){
									%>
									<li><g:link controller="metadata" action="show" id="${m?.id}">${m?.encodeAsHTML()}</g:link></li>
									<%
									   i++;
									   if(i > 10) break;
									}
								}
								%>
								</ul>
								<span>...and ${imageCollectionInstance.metadata.size()-10} more</span>
							</td>
							
						</tr>
					
						<tr class="prop">
							<td valign="top" class="name"><g:message code="imageCollection.name.label" default="Name" /></td>
							
							<td valign="top" class="value">${fieldValue(bean: imageCollectionInstance, field: "name")}</td>
							
						</tr>
					
					</tbody>
				</table>
			</div>
		</div>
	</body>
</html>
