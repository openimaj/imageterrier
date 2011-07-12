package org.imageterrier.webapp
import grails.converters.*
import org.springframework.security.access.prepost.PostFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.model.Permission
import org.springframework.transaction.annotation.Transactional
import grails.plugins.springsecurity.Secured
import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION
class QueryOptionsController {

	static allowedMethods = [save: "POST", update: "POST", delete: "POST"]
	def queryOptionsService
	def sessionFactory
	def aclPermissionFactory
	def aclService
	def aclUtilService
	def springSecurityService
	def index = {
		redirect(action: "list", params: params)
	}

	def list = {
		params.max = Math.min(params.max ? params.int('max') : 10, 100)
		def ret = [queryOptionsInstanceList: QueryOptions.list(params), queryOptionsInstanceTotal: QueryOptions.count()]
		withFormat {
			html { return ret }
			xml { render ret as XML }
			json { render ret as JSON }
		}
	}
	
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def create = {
		def queryOptionsInstance = new QueryOptions()
		queryOptionsInstance.properties = params
		return [queryOptionsInstance: queryOptionsInstance]
	}
	
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def save = {
		def queryOptionsInstance = new QueryOptions(params)
		if (queryOptionsInstance.save(flush: true)) {
			aclUtilService.addPermission queryOptionsInstance, springSecurityService.principal.username, ADMINISTRATION
			sessionFactory.currentSession.flush()
			
			flash.message = "${message(code: 'default.created.message', args: [message(code: 'queryOptions.label', default: 'QueryOptions'), queryOptionsInstance.id])}"
			redirect(action: "show", id: queryOptionsInstance.id)
		}
		else {
			render(view: "create", model: [queryOptionsInstance: queryOptionsInstance])
		}
	}
	
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def show = {
		def queryOptionsInstance = QueryOptions.get(params.id)
		if (!queryOptionsInstance) {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'queryOptions.label', default: 'QueryOptions'), params.id])}"
			redirect(action: "list")
		}
		else {
			[queryOptionsInstance: queryOptionsInstance]
		}
	}
	
	private getById(id){
		return queryOptionsService.get(id)
	}
	
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def edit = {
		def queryOptionsInstance = getById(params.long("id"))
		if (!queryOptionsInstance) {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'queryOptions.label', default: 'QueryOptions'), params.id])}"
			redirect(action: "list")
		}
		else {
			return [queryOptionsInstance: queryOptionsInstance]
		}
	}
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def update = {
		def queryOptionsInstance = getById(params.long("id"))
		if (queryOptionsInstance) {
			if (params.version) {
				def version = params.version.toLong()
				if (queryOptionsInstance.version > version) {
					
					queryOptionsInstance.errors.rejectValue("version", "default.optimistic.locking.failure", [message(code: 'queryOptions.label', default: 'QueryOptions')] as Object[], "Another user has updated this QueryOptions while you were editing")
					render(view: "edit", model: [queryOptionsInstance: queryOptionsInstance])
					return
				}
			}
			queryOptionsInstance.properties = params
			if (!queryOptionsInstance.hasErrors() && queryOptionsInstance.save(flush: true)) {
				flash.message = "${message(code: 'default.updated.message', args: [message(code: 'queryOptions.label', default: 'QueryOptions'), queryOptionsInstance.id])}"
				redirect(action: "show", id: queryOptionsInstance.id)
			}
			else {
				render(view: "edit", model: [queryOptionsInstance: queryOptionsInstance])
			}
		}
		else {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'queryOptions.label', default: 'QueryOptions'), params.id])}"
			redirect(action: "list")
		}
	}
	@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
	def delete = {
		def queryOptionsInstance = getById(params.long("id"))
		if (queryOptionsInstance) {
			try {
				queryOptionsInstance.delete(flush: true)
				flash.message = "${message(code: 'default.deleted.message', args: [message(code: 'queryOptions.label', default: 'QueryOptions'), params.id])}"
				redirect(action: "list")
			}
			catch (org.springframework.dao.DataIntegrityViolationException e) {
				flash.message = "${message(code: 'default.not.deleted.message', args: [message(code: 'queryOptions.label', default: 'QueryOptions'), params.id])}"
				redirect(action: "show", id: params.id)
			}
		}
		else {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'queryOptions.label', default: 'QueryOptions'), params.id])}"
			redirect(action: "list")
		}
	}
}
