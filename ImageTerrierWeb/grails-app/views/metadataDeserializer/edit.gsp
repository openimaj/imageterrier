

<%@ page import="org.imageterrier.webapp.MetadataDeserializer" %>
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
		<meta name="layout" content="main" />
		<g:set var="entityName" value="${message(code: 'metadataDeserializer.label', default: 'MetadataDeserializer')}" />
		<title><g:message code="default.edit.label" args="[entityName]" /></title>
		<script type="text/javascript" src="/ImageTerrierWeb/plugins/console-1.0.1/js/jquery-1.4.4.min.js"></script>
		<script type="text/javascript" src="/ImageTerrierWeb/plugins/console-1.0.1/js/jquery.hotkeys.js"></script>
		<script type="text/javascript" src="/ImageTerrierWeb/plugins/console-1.0.1/js/jquery.scrollTo-min.js"></script>
		<script type="text/javascript" src="/ImageTerrierWeb/plugins/console-1.0.1/js/splitter.js"></script>
		<script type="text/javascript" src="/ImageTerrierWeb/plugins/console-1.0.1/js/codemirror/js/codemirror.js"></script>
		<script type="text/javascript" charset="utf-8">
			$(document).ready(function () {
				var pluginContext = "/ImageTerrierWeb/plugins/console-1.0.1/"
				var editor = CodeMirror.fromTextArea('code', {
					height: '250px', // 100% for splitter
					content: $('#code').val(),
					parserfile: ['../contrib/groovy/parsegroovy.js', '../contrib/groovy/tokenizegroovy.js'],
					stylesheet: pluginContext + '/js/codemirror/contrib/groovy/groovycolors.css',
					path: pluginContext + '/js/codemirror/js/',
					autoMatchParens: true,
					tabMode: 'shift',
					textWrapping: false,
					indentUnit: 3
				});
				
			})
		</script>
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
			<g:hasErrors bean="${metadataDeserializerInstance}">
			<div class="errors">
				<g:renderErrors bean="${metadataDeserializerInstance}" as="list" />
			</div>
			</g:hasErrors>
			<g:form method="post" >
				<g:hiddenField name="id" value="${metadataDeserializerInstance?.id}" />
				<g:hiddenField name="version" value="${metadataDeserializerInstance?.version}" />
				<div class="dialog">
					<table>
						<tbody>
						
							<tr class="prop">
								<td valign="top" class="name">
								  <label for="description"><g:message code="metadataDeserializer.description.label" default="Description" /></label>
								</td>
								<td valign="top" class="value ${hasErrors(bean: metadataDeserializerInstance, field: 'description', 'errors')}">
									<g:textField name="description" value="${metadataDeserializerInstance?.description}" />
								</td>
							</tr>
						
							<tr class="prop">
								<td valign="top" class="name">
								  <label for="groovyClosure"><g:message code="metadataDeserializer.groovyClosure.label" default="Groovy Closure" /></label>
								</td>
								<td valign="top" class="value ${hasErrors(bean: metadataDeserializerInstance, field: 'groovyClosure', 'errors')}">
									<textarea id="code" name="groovyClosure"  width="525" height="200">${metadataDeserializerInstance?.groovyClosure}</textarea>
								</td>
							</tr>
						
							<tr class="prop">
								<td valign="top" class="name">
								  <label for="name"><g:message code="metadataDeserializer.name.label" default="Name" /></label>
								</td>
								<td valign="top" class="value ${hasErrors(bean: metadataDeserializerInstance, field: 'name', 'errors')}">
									<g:textField name="name" value="${metadataDeserializerInstance?.name}" />
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
