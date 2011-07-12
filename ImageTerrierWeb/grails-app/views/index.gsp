<html>
	<head>
		<title>Welcome to Grails</title>
		<meta name="layout" content="main" />
		<style type="text/css" media="screen">

		#nav {
			margin-top:20px;
			margin-left:30px;
			width:228px;
			float:left;

		}
		.homePagePanel * {
			margin:0px;
		}
		.homePagePanel .panelBody ul {
			list-style-type:none;
			margin-bottom:10px;
		}
		.homePagePanel .panelBody h1 {
			text-transform:uppercase;
			font-size:1.1em;
			margin-bottom:10px;
		}
		.homePagePanel .panelBody {
			background: url(images/leftnav_midstretch.png) repeat-y top;
			margin:0px;
			padding:15px;
		}
		.homePagePanel .panelBtm {
			background: url(images/leftnav_btm.png) no-repeat top;
			height:20px;
			margin:0px;
		}

		.homePagePanel .panelTop {
			background: url(images/leftnav_top.png) no-repeat top;
			height:11px;
			margin:0px;
		}
		h2 {
			margin-top:15px;
			margin-bottom:15px;
			font-size:1.2em;
		}
		#pageBody {
			margin-left:280px;
			margin-right:20px;
		}
		</style>
	</head>
	<body>
		<div id="pageBody">
		<sec:ifLoggedIn>
			<div id="controllerList" class="dialog">
				<sec:ifAllGranted roles="ROLE_INDEXER">
				<h2>As an Indexer you can access:</h2>
				<ul>
					<li class="controller">
						<h3>Metadata</h3>
						<ul>
							<li class="action"><g:link controller="metadataImporter" action="create">Add Importer</g:link></li>
							<li class="action"><g:link controller="metadataImporter" action="list">List Importer</g:link></li>
							<li class="action"><g:link controller="metadataDeserializer" action="create">Add Deserializer</g:link></li>
							<li class="action"><g:link controller="metadataDeserializer" action="list">List Deserializer</g:link></li>
						</ul>
					</li>
					<li class="controller">
						<h3>Indexes</h3>
						<ul>
							<li class="action"><g:link controller="import">Add</g:link></li>
							<li class="action"><g:link controller="imageTerrierIndex" action="list">List All</g:link></li>
						</ul>
					</li>
					
					<li class="controller">
						<h3>Query Options</h3>
						<ul>
							<li class="action"><g:link controller="queryOptions" action="create">Add</g:link></li>
							<li class="action"><g:link controller="queryOptions" action="list">List All</g:link></li>
						</ul>
					</li>
					<li class="controller">
						<h3>Results Processors</h3>
						<ul>
							<li class="action"><g:link controller="resultsProcessor" action="create">Add</g:link></li>
							<li class="action"><g:link controller="resultsProcessor" action="list">List All</g:link></li>
						</ul>
					</li>
					<li class="controller">
						<h3>User Preferences</h3>
						<ul>
							<li class="action"><g:link controller="user" action="edit" id="${sec.loggedInUserInfo(field:'id')}">Edit</g:link></li>
						</ul>
					</li>
				</ul>
				</sec:ifAllGranted>
				<sec:ifAllGranted roles="ROLE_ADMIN">
				<h2>As an Admin you can access:</h2>
				<ul>
					<li class="controller">
						<h3>User Preferences</h3>
						<ul>
							<li class="action"><g:link controller="user" action="create" >Add</g:link></li>
							<li class="action"><g:link controller="user" action="list" >List</g:link></li>
						</ul>
					</li>
					<li class="controller">
						<h3>Roles</h3>
						<ul>
							<li class="action"><g:link controller="role" action="list" >List</g:link></li>
							<li class="action"><g:link controller="role" action="create" >Add</g:link></li>
						</ul>
					</li>
					<li class="controller">
						<h3>User->Roles</h3>
						<ul>
							<li class="action"><g:link controller="userRole" action="list" >List</g:link></li>
							<li class="action"><g:link controller="userRole" action="create" >Add</g:link></li>
						</ul>
					</li>
				</ul>
				</sec:ifAllGranted>
			</div>
		</sec:ifLoggedIn>
		</div>
	</body>
</html>
