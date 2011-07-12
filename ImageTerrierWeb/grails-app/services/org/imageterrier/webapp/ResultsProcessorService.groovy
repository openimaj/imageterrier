package org.imageterrier.webapp
import org.springframework.security.access.prepost.PostFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.model.Permission
import org.springframework.transaction.annotation.Transactional

class ResultsProcessorService {
	static transactional = false
	
	def aclPermissionFactory
	def aclService
	def aclUtilService
	def springSecurityService


	def serviceMethod() {

	}
	
	@PreAuthorize("hasPermission(#id, 'org.imageterrier.webapp.ResultsProcessor', admin) or hasRole('ROLE_ADMIN')")
	ResultsProcessor get(long id) {
		ResultsProcessor.get id
	}
}
