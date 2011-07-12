package org.imageterrier.webapp
import grails.converters.*
import grails.plugins.springsecurity.Secured
import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION


class ResultsProcessorController {

	static allowedMethods = [save: "POST", update: "POST", delete: "POST", edit:"POST"]
	
	def resultsProcessorService
	def aclUtilService
	def springSecurityService
	def sessionFactory
	def index = {
		redirect(action: "list", params: params)
	}

	def list = {
		params.max = Math.min(params.max ? params.int('max') : 10, 100)
		def ret = [resultsProcessorInstanceList: ResultsProcessor.list(params), resultsProcessorInstanceTotal: ResultsProcessor.count()]
		withFormat {
			html { return ret }
			xml { render ret as XML }
			json { render ret as JSON }
		}
	}
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def create = {
		def resultsProcessorInstance = new ResultsProcessor()
		resultsProcessorInstance.properties = params
		return [resultsProcessorInstance: resultsProcessorInstance]
	}
	
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def save = {
		def resultsProcessorInstance = new ResultsProcessor(params)
		if (resultsProcessorInstance.save(flush: true)) {
			
			aclUtilService.addPermission resultsProcessorInstance, springSecurityService.principal.username, ADMINISTRATION
			sessionFactory.currentSession.flush()
			
			flash.message = "${message(code: 'default.created.message', args: [message(code: 'resultsProcessor.label', default: 'ResultsProcessor'), resultsProcessorInstance.id])}"
			redirect(action: "show", id: resultsProcessorInstance.id)
		}
		else {
			render(view: "create", model: [resultsProcessorInstance: resultsProcessorInstance])
		}
	}
	
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def show = {
		def resultsProcessorInstance = ResultsProcessor.get(params.id)
		if (!resultsProcessorInstance) {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'resultsProcessor.label', default: 'ResultsProcessor'), params.id])}"
			redirect(action: "list")
		}
		else {
			[resultsProcessorInstance: resultsProcessorInstance]
		}
	}
	
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def edit = {
		def resultsProcessorInstance = resultsProcessorService.get(params.long("id"))
		if (!resultsProcessorInstance) {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'resultsProcessor.label', default: 'ResultsProcessor'), params.id])}"
			redirect(action: "list")
		}
		else {
			return [resultsProcessorInstance: resultsProcessorInstance]
		}
	}
	
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def update = {
		def resultsProcessorInstance = resultsProcessorService.get(params.long("id"))
		if (resultsProcessorInstance) {
			if (params.version) {
				def version = params.version.toLong()
				if (resultsProcessorInstance.version > version) {
					
					resultsProcessorInstance.errors.rejectValue("version", "default.optimistic.locking.failure", [message(code: 'resultsProcessor.label', default: 'ResultsProcessor')] as Object[], "Another user has updated this ResultsProcessor while you were editing")
					render(view: "edit", model: [resultsProcessorInstance: resultsProcessorInstance])
					return
				}
			}
			resultsProcessorInstance.properties = params
			if (!resultsProcessorInstance.hasErrors() && resultsProcessorInstance.save(flush: true)) {
				flash.message = "${message(code: 'default.updated.message', args: [message(code: 'resultsProcessor.label', default: 'ResultsProcessor'), resultsProcessorInstance.id])}"
				redirect(action: "show", id: resultsProcessorInstance.id)
			}
			else {
				render(view: "edit", model: [resultsProcessorInstance: resultsProcessorInstance])
			}
		}
		else {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'resultsProcessor.label', default: 'ResultsProcessor'), params.id])}"
			redirect(action: "list")
		}
	}
	
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def delete = {
		def resultsProcessorInstance = resultsProcessorService.get(params.long("id"))
		if (resultsProcessorInstance) {
			try {
				resultsProcessorInstance.delete(flush: true)
				flash.message = "${message(code: 'default.deleted.message', args: [message(code: 'resultsProcessor.label', default: 'ResultsProcessor'), params.id])}"
				redirect(action: "list")
			}
			catch (org.springframework.dao.DataIntegrityViolationException e) {
				flash.message = "${message(code: 'default.not.deleted.message', args: [message(code: 'resultsProcessor.label', default: 'ResultsProcessor'), params.id])}"
				redirect(action: "show", id: params.id)
			}
		}
		else {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'resultsProcessor.label', default: 'ResultsProcessor'), params.id])}"
			redirect(action: "list")
		}
	}
}
