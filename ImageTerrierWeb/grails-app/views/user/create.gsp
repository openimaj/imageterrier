

<%@ page import="org.imageterrier.webapp.security.User" %>
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
		<meta name="layout" content="main" />
		<g:set var="entityName" value="${message(code: 'user.label', default: 'User')}" />
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
			<g:hasErrors bean="${userInstance}">
			<div class="errors">
				<g:renderErrors bean="${userInstance}" as="list" />
			</div>
			</g:hasErrors>
			<g:form action="save" >
				<div class="dialog">
					<table>
						<tbody>
						
							<tr class="prop">
								<td valign="top" class="name">
									<label for="username"><g:message code="user.username.label" default="Username" /></label>
								</td>
								<td valign="top" class="value ${hasErrors(bean: userInstance, field: 'username', 'errors')}">
									<g:textField name="username" value="${userInstance?.username}" />
								</td>
							</tr>
							
							<tr class="prop">
								<td valign="top" class="name">
								  <label for="new_password"><g:message code="user.new_password.label" default="New Password" /></label>
								</td>
								<td valign="top" class="value ${hasErrors(bean: userInstance, field: 'password', 'errors')}">
									<g:passwordField name="new_password" value="" />
								</td>
							</tr>
							<tr class="prop">
								<td valign="top" class="name">
								  <label for="renew_password"><g:message code="user.renew_password.label" default="Retype New Password" /></label>
								</td>
								<td valign="top" class="value ${hasErrors(bean: userInstance, field: 'password', 'errors')}">
									<g:passwordField name="renew_password" value="" />
								</td>
							</tr>
						
							<tr class="prop">
								<td valign="top" class="name">
									<label for="accountExpired"><g:message code="user.accountExpired.label" default="Account Expired" /></label>
								</td>
								<td valign="top" class="value ${hasErrors(bean: userInstance, field: 'accountExpired', 'errors')}">
									<g:checkBox name="accountExpired" value="${userInstance?.accountExpired}" />
								</td>
							</tr>
						
							<tr class="prop">
								<td valign="top" class="name">
									<label for="accountLocked"><g:message code="user.accountLocked.label" default="Account Locked" /></label>
								</td>
								<td valign="top" class="value ${hasErrors(bean: userInstance, field: 'accountLocked', 'errors')}">
									<g:checkBox name="accountLocked" value="${userInstance?.accountLocked}" />
								</td>
							</tr>
						
							<tr class="prop">
								<td valign="top" class="name">
									<label for="enabled"><g:message code="user.enabled.label" default="Enabled" /></label>
								</td>
								<td valign="top" class="value ${hasErrors(bean: userInstance, field: 'enabled', 'errors')}">
									<g:checkBox name="enabled" value="${userInstance?.enabled}" />
								</td>
							</tr>
						
							<tr class="prop">
								<td valign="top" class="name">
									<label for="passwordExpired"><g:message code="user.passwordExpired.label" default="Password Expired" /></label>
								</td>
								<td valign="top" class="value ${hasErrors(bean: userInstance, field: 'passwordExpired', 'errors')}">
									<g:checkBox name="passwordExpired" value="${userInstance?.passwordExpired}" />
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
