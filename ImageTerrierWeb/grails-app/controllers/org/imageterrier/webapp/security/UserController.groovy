package org.imageterrier.webapp.security
import grails.plugins.springsecurity.Secured
import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION


@Secured(['ROLE_ADMIN','ROLE_INDEXER'])
class UserController {
	
	static allowedMethods = [save: "POST", update: "POST", delete: "POST"]
	
	def userService
	def springSecurityService
	def aclService
	def aclUtilService
	
	@Secured(['ROLE_ADMIN'])
	def index = {
		redirect(action: "list", params: params)
	}
	
	@Secured(['ROLE_ADMIN'])
	def list = {
		params.max = Math.min(params.max ? params.int('max') : 10, 100)
		[userInstanceList: User.list(params), userInstanceTotal: User.count()]
	}
	
	@Secured(['ROLE_ADMIN'])
	def create = {
		def userInstance = new User()
		userInstance.properties = params
		return [userInstance: userInstance]
	}
	
	@Secured(['ROLE_ADMIN'])
	def save = {
		def userInstance = new User(params)
		if(assignPasswords(userInstance,params)){
			if (userInstance.save(flush: true)) {
				flash.message = "${message(code: 'default.created.message', args: [message(code: 'user.label', default: 'User'), userInstance.id])}"
				def indexerRole = Role.findByAuthority("ROLE_INDEXER");
				
				UserRole.create userInstance, indexerRole
				
				aclUtilService.addPermission userInstance, springSecurityService.principal.username, ADMINISTRATION
				aclUtilService.addPermission userInstance, userInstance.username, ADMINISTRATION
				
				
				redirect(action: "show", id: userInstance.id)
			}
			else {
				render(view: "create", model: [userInstance: userInstance])
			}
		}
		else {
			render(view: "create", model: [userInstance: userInstance])
		}
	}
	
	
	def show = {
		def userInstance = userService.get(params.long("id"))
		if (!userInstance) {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'User'), params.id])}"
			redirect(action: "list")
		}
		else {
			[userInstance: userInstance]
		}
	}

	def edit = {
		def userInstance = userService.get(params.long("id"))
		if (!userInstance) {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'User'), params.id])}"
			redirect(action: "list")
		}
		else {
			userInstance.password = ""
			return [userInstance: userInstance]
		}
	}

	def update = {
		def userInstance = userService.get(params.long("id"))
		if (userInstance) {
			if (params.version) {
				def version = params.version.toLong()
				if (userInstance.version > version) {
					
					userInstance.errors.rejectValue("version", "default.optimistic.locking.failure", [message(code: 'user.label', default: 'User')] as Object[], "Another user has updated this User while you were editing")
					render(view: "edit", model: [userInstance: userInstance])
					return
				}
			}
			if(assignPasswords(userInstance,params)){
				userInstance.properties = params
				// Hash the password
				if (!userInstance.hasErrors() && userInstance.save(flush: true)) {
					flash.message = "${message(code: 'default.updated.message', args: [message(code: 'user.label', default: 'User'), userInstance.id])}"
					redirect(action: "show", id: userInstance.id)
				}
				else {
					render(view: "edit", model: [userInstance: userInstance])
				}
			}
			else{
				userInstance.errors.rejectValue("password","the passwords did not match")
				render(view: "edit", model: [userInstance: userInstance])
			}
		}
		else {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'User'), params.id])}"
			redirect(action: "list")
		}
	}
	
	private boolean assignPasswords(userInstance,params){
		if(params.new_password!=null && params.new_password!="")
		{
			if(params.new_password==params.renew_password){
				userInstance.password = springSecurityService.encodePassword(params.new_password)
				params.remove("new_password");
				params.remove("renew_password");
				return true
			}
			else
			{
				return false
			}
		}
		else{
			return true;
		}
	}
	
	@Secured(['ROLE_ADMIN'])
	def delete = {
		def userInstance = userService.get(params.long("id"))
		if (userInstance) {
			try {
				userInstance.delete(flush: true)
				flash.message = "${message(code: 'default.deleted.message', args: [message(code: 'user.label', default: 'User'), params.id])}"
				redirect(action: "list")
			}
			catch (org.springframework.dao.DataIntegrityViolationException e) {
				flash.message = "${message(code: 'default.not.deleted.message', args: [message(code: 'user.label', default: 'User'), params.id])}"
				redirect(action: "show", id: params.id)
			}
		}
		else {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'User'), params.id])}"
			redirect(action: "list")
		}
	}
}
