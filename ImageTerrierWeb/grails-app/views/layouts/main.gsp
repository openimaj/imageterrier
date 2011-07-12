<!DOCTYPE html>
<html>
	<head>
		<title><g:layoutTitle default="Grails" /></title>
		<link rel="stylesheet" href="${resource(dir:'css',file:'main.css')}" />
		<link rel="shortcut icon" href="${resource(dir:'images',file:'favicon.ico')}" type="image/x-icon" />
		<g:layoutHead />
		<g:javascript library="application" />
	</head>
	<body>
		<div id="spinner" class="spinner" style="display:none;">
			<img src="${resource(dir:'images',file:'spinner.gif')}" alt="${message(code:'spinner.alt',default:'Loading...')}" />
		</div>
		<div id="imageterrierLogo"><g:link view="index"><img src="${resource(dir:'images',file:'imageterrier.png')}" alt="ImageTerrier" border="0" /></g:link></div>
		<div id="authbar">
			<sec:ifLoggedIn>
				Welcome, <sec:username/>. <g:link controller="logout"> Logout </g:link>
			</sec:ifLoggedIn>
			<sec:ifNotLoggedIn>
				You are not logged in. <g:link controller="login"> Login here </g:link>
			</sec:ifNotLoggedIn>
		</div>
		<g:layoutBody />
	</body>
</html>