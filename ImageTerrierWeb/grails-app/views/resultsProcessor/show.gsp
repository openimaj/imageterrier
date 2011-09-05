
<%@ page import="org.imageterrier.webapp.ResultsProcessor" %>
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
		<meta name="layout" content="main" />
		<g:set var="entityName" value="${message(code: 'resultsProcessor.label', default: 'ResultsProcessor')}" />
		<title><g:message code="default.show.label" args="[entityName]" /></title>
		<syntax:resources name="code" languages="['Java', 'Groovy']" />
		<link rel="stylesheet" href="/ImageTerrierWeb/plugins/console-1.0.1/css/grails-console.css">
		<style type="text/css" media="screen">
			.dp-j{
				height:200px;
				overflow-y: scroll;
			}
		</style>
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
							<td valign="top" class="name"><g:message code="resultsProcessor.id.label" default="Id" /></td>
							
							<td valign="top" class="value">${fieldValue(bean: resultsProcessorInstance, field: "id")}</td>
							
						</tr>
					
						<tr class="prop">
							<td valign="top" class="name"><g:message code="resultsProcessor.description.label" default="Description" /></td>
							
							<td valign="top" class="value">${fieldValue(bean: resultsProcessorInstance, field: "description")}</td>
							
						</tr>
					
						<tr class="prop">
							<td valign="top" class="name"><g:message code="resultsProcessor.groovyClosure.label" default="Groovy Closure" /></td>
							<td valign="top" class="value">
								<syntax:format name="code" language="groovy"><%=resultsProcessorInstance.groovyClosure%></syntax:format>
							</td>
						</tr>
						<%
						if(resultsProcessorInstance.compilationError!=null){
						%>
						<tr class="prop">
							<td valign="top" class="name"><g:message code="resultsProcessor.compilationError.label" default="Closure Error" /></td>
							<td valign="top" class="value stacktrace" style="width:100%;overflow:scroll"><%=resultsProcessorInstance.compilationError%></td>
						</tr>
						<%
						}
						%>
					
						<tr class="prop">
							<td valign="top" class="name"><g:message code="resultsProcessor.name.label" default="Name" /></td>
							
							<td valign="top" class="value">${fieldValue(bean: resultsProcessorInstance, field: "name")}</td>
							
						</tr>
					
						<tr class="prop">
							<td valign="top" class="name"><g:message code="resultsProcessor.shortName.label" default="Short Name" /></td>
							
							<td valign="top" class="value">${fieldValue(bean: resultsProcessorInstance, field: "shortName")}</td>
							
						</tr>
					
					</tbody>
				</table>
			</div>
			<div class="buttons">
				<g:form action="edit">
					<g:hiddenField name="id" value="${resultsProcessorInstance?.id}" />
					<span class="button">
						<g:submitButton class="edit" name="Edit" />
					</span>
				</g:form>
				<g:form action="delete">
					<g:hiddenField name="id" value="${resultsProcessorInstance?.id}" />
					<span class="button"><g:submitButton class="delete" name="Delete" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" /></span>
				</g:form>
			</div>
		</div>
	</body>
</html>
