package org.imageterrier.webapp
import grails.plugins.springsecurity.Secured
import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION
class ImportController {
	
	def aclUtilService
	def springSecurityService
	def sessionFactory
	
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def index = {
		
	}
	
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def importData = {
		ImageCollection collection = new ImageCollection(params.collection)
		ImageTerrierIndex index = new ImageTerrierIndex(
			indexPath:params.index.indexPath,
			name:params.index.name,
			shortName:params.index.shortName,
			description:params.index.description,
			imageCollection:collection)
		
		index.save()
				
		MetadataImporter importer = MetadataImporter.get(params.importer.id)
		File dataFile = new File(params.datapath)
		
		importer.importData(dataFile, collection, index)
		
		aclUtilService.addPermission index, springSecurityService.principal.username, ADMINISTRATION
		sessionFactory.currentSession.flush()
		
		flash.message = "${message(code: 'default.created.message', args: [message(code: 'imageTerrierIndex.label', default: 'ImageTerrierIndex'), index.id])}"
		redirect(controller: "imageTerrierIndex", action: "show", id: index.id)
	}
}