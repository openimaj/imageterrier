<%@ page import="org.imageterrier.webapp.ImageCollection" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="layout" content="main" />
        
        <title>Import An Index and Collection</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></span>
        </div>

        <div class="body">
            <h1>Import a new collection and index</h1>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
	
						<g:hasErrors bean="${imageCollectionInstance}">
            <div class="errors">
                <g:renderErrors bean="${imageCollectionInstance}" as="list" />
            </div>
            </g:hasErrors>


            <g:form action="importData" >
                <div class="dialog">
									<h2>Describe the image collection:</h2>
                  <table>
                      <tbody>
                      		
													<tr class="prop">
                              <td valign="top" class="name">
                                  <label for="collection.name"><g:message code="imageCollection.name.label" default="Name" /></label>
                              </td>
                              <td valign="top" class="value ${hasErrors(bean: imageCollectionInstance, field: 'name', 'errors')}">
                                  <g:textField name="collection.name" value="${imageCollectionInstance?.name}" />
                              </td>
                          </tr>

                          <tr class="prop">
                              <td valign="top" class="name">
                                  <label for="collection.description"><g:message code="imageCollection.description.label" default="Description" /></label>
                              </td>
                              <td valign="top" class="value ${hasErrors(bean: imageCollectionInstance, field: 'description', 'errors')}">
                                  <g:textField name="collection.description" value="${imageCollectionInstance?.description}" />
                              </td>
                          </tr>
                      
                          <tr class="prop">
                              <td valign="top" class="name">
                                  <label for="collection.deserializer"><g:message code="imageCollection.deserializer.label" default="Deserializer" /></label>
                              </td>
                              <td valign="top" class="value ${hasErrors(bean: imageCollectionInstance, field: 'deserializer', 'errors')}">
                                  <g:select name="collection.deserializer.id" from="${org.imageterrier.webapp.MetadataDeserializer.list()}" optionKey="id" value="${imageCollectionInstance?.deserializer?.id}"  />
                              </td>
                          </tr>
                      
                      </tbody>
                  </table>

									<h2>Describe the ImageTerrier index:</h2>
									<table>
                      <tbody>
                      
	                        <tr class="prop">
	                            <td valign="top" class="name">
	                                <label for="index.name"><g:message code="imageTerrierIndex.name.label" default="Name" /></label>
	                            </td>
	                            <td valign="top" class="value ${hasErrors(bean: imageTerrierIndexInstance, field: 'name', 'errors')}">
	                                <g:textField name="index.name" value="${imageTerrierIndexInstance?.name}" />
	                            </td>
	                        </tr>

                          <tr class="prop">
                              <td valign="top" class="name">
                                  <label for="index.description"><g:message code="imageTerrierIndex.description.label" default="Description" /></label>
                              </td>
                              <td valign="top" class="value ${hasErrors(bean: imageTerrierIndexInstance, field: 'description', 'errors')}">
                                  <g:textField name="index.description" value="${imageTerrierIndexInstance?.description}" />
                              </td>
                          </tr>
                      
                          <tr class="prop">
                              <td valign="top" class="name">
                                  <label for="index.indexPath"><g:message code="imageTerrierIndex.indexPath.label" default="Index Path" /></label>
                              </td>
                              <td valign="top" class="value ${hasErrors(bean: imageTerrierIndexInstance, field: 'indexPath', 'errors')}">
                                  <g:textField name="index.indexPath" value="${imageTerrierIndexInstance?.indexPath}" />
                              </td>
                          </tr>
                      
                      </tbody>
                  </table>

									<h2>Select the importer and specify the data file path:</h2>
									<table>
                      <tbody>
                      		
                          <tr class="prop">
                              <td valign="top" class="name">
                                  <label for="importer"><g:message code="MetadataImporter.label" default="Importer" /></label>
                              </td>
                              <td valign="top" class="value">
                                  <g:select name="importer.id" from="${org.imageterrier.webapp.MetadataImporter.list()}" optionKey="id" value="${metadataImporterInstance?.id}" />
                              </td>
                          </tr>

                          <tr class="prop">
                              <td valign="top" class="name">
                                  <label for="datapath">Data File Path</label>
                              </td>
                              <td valign="top" class="value">
                                  <g:textField name="datapath" value="${datapath}" />
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
