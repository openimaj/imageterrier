
<%@ page import="org.imageterrier.webapp.QueryOptions" %>
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
		<meta name="layout" content="main" />
		<g:set var="entityName" value="${message(code: 'queryOptions.label', default: 'QueryOptions')}" />
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
							<td valign="top" class="name"><g:message code="queryOptions.id.label" default="Id" /></td>
							
							<td valign="top" class="value">${fieldValue(bean: queryOptionsInstance, field: "id")}</td>
							
						</tr>
					
						<tr class="prop">
							<td valign="top" class="name"><g:message code="queryOptions.description.label" default="Description" /></td>
							
							<td valign="top" class="value">${fieldValue(bean: queryOptionsInstance, field: "description")}</td>
							
						</tr>
					
						<tr class="prop">
							<td valign="top" class="name"><g:message code="queryOptions.name.label" default="Name" /></td>
							
							<td valign="top" class="value">${fieldValue(bean: queryOptionsInstance, field: "name")}</td>
							
						</tr>
					
						<tr class="prop">
							<td valign="top" class="name"><g:message code="queryOptions.options.label" default="Options" /></td>
							
							<td valign="top" class="value">${fieldValue(bean: queryOptionsInstance, field: "options")}</td>
							
						</tr>
					
					</tbody>
				</table>
			</div>
			<div class="buttons">
				<g:form action="edit">
					<g:hiddenField name="id" value="${queryOptionsInstance?.id}" />
					<span class="button">
						<g:submitButton class="edit" name="Edit" />
					</span>
				</g:form>
				<g:form action="delete">
					<g:hiddenField name="id" value="${queryOptionsInstance?.id}" />
					<span class="button"><g:submitButton class="delete" name="Delete" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" /></span>
				</g:form>
			</div>
		</div>
	</body>
</html>
