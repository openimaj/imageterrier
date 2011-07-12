package org.imageterrier.webapp
import grails.converters.*
import grails.plugins.springsecurity.Secured
import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION

class MetadataDeserializerController {

	static allowedMethods = [save: "POST", update: "POST", delete: "POST"]
	def metadataDeserializerService
	def aclUtilService
	def springSecurityService
	def sessionFactory
	def index = {
		redirect(action: "list", params: params)
	}

	def list = {
		params.max = Math.min(params.max ? params.int('max') : 10, 100)
		[metadataDeserializerInstanceList: MetadataDeserializer.list(params), metadataDeserializerInstanceTotal: MetadataDeserializer.count()]
	}
	
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def create = {
		def metadataDeserializerInstance = new MetadataDeserializer()
		metadataDeserializerInstance.properties = params
		return [metadataDeserializerInstance: metadataDeserializerInstance]
	}
	
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def save = {
		def metadataDeserializerInstance = new MetadataDeserializer(params)
		if (metadataDeserializerInstance.save(flush: true)) {
			aclUtilService.addPermission metadataDeserializerInstance, springSecurityService.principal.username, ADMINISTRATION
			sessionFactory.currentSession.flush()
			
			flash.message = "${message(code: 'default.created.message', args: [message(code: 'metadataDeserializer.label', default: 'MetadataDeserializer'), metadataDeserializerInstance.id])}"
			redirect(action: "show", id: metadataDeserializerInstance.id)
		}
		else {
			render(view: "create", model: [metadataDeserializerInstance: metadataDeserializerInstance])
		}
	}
	
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def show = {
		def metadataDeserializerInstance = MetadataDeserializer.get(params.id)
		if (!metadataDeserializerInstance) {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'metadataDeserializer.label', default: 'MetadataDeserializer'), params.id])}"
			redirect(action: "list")
		}
		else {
			[metadataDeserializerInstance: metadataDeserializerInstance]
		}
	}
	
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def edit = {
		def metadataDeserializerInstance = metadataDeserializerService.get(params.long("id"))
		if (!metadataDeserializerInstance) {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'metadataDeserializer.label', default: 'MetadataDeserializer'), params.id])}"
			redirect(action: "list")
		}
		else {
			return [metadataDeserializerInstance: metadataDeserializerInstance]
		}
	}
	
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def update = {
		def metadataDeserializerInstance = metadataDeserializerService.get(params.long("id"))
		if (metadataDeserializerInstance) {
			if (params.version) {
				def version = params.version.toLong()
				if (metadataDeserializerInstance.version > version) {
					
					metadataDeserializerInstance.errors.rejectValue("version", "default.optimistic.locking.failure", [message(code: 'metadataDeserializer.label', default: 'MetadataDeserializer')] as Object[], "Another user has updated this MetadataDeserializer while you were editing")
					render(view: "edit", model: [metadataDeserializerInstance: metadataDeserializerInstance])
					return
				}
			}
			metadataDeserializerInstance.properties = params
			if (!metadataDeserializerInstance.hasErrors() && metadataDeserializerInstance.save(flush: true)) {
				flash.message = "${message(code: 'default.updated.message', args: [message(code: 'metadataDeserializer.label', default: 'MetadataDeserializer'), metadataDeserializerInstance.id])}"
				redirect(action: "show", id: metadataDeserializerInstance.id)
			}
			else {
				render(view: "edit", model: [metadataDeserializerInstance: metadataDeserializerInstance])
			}
		}
		else {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'metadataDeserializer.label', default: 'MetadataDeserializer'), params.id])}"
			redirect(action: "list")
		}
	}
	
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def delete = {
		def metadataDeserializerInstance = metadataDeserializerService.get(params.long("id"))
		if (metadataDeserializerInstance) {
			try {
				metadataDeserializerInstance.delete(flush: true)
				flash.message = "${message(code: 'default.deleted.message', args: [message(code: 'metadataDeserializer.label', default: 'MetadataDeserializer'), params.id])}"
				redirect(action: "list")
			}
			catch (org.springframework.dao.DataIntegrityViolationException e) {
				flash.message = "${message(code: 'default.not.deleted.message', args: [message(code: 'metadataDeserializer.label', default: 'MetadataDeserializer'), params.id])}"
				redirect(action: "show", id: params.id)
			}
		}
		else {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'metadataDeserializer.label', default: 'MetadataDeserializer'), params.id])}"
			redirect(action: "list")
		}
	}
}
