package org.imageterrier.webapp.security
import org.springframework.security.access.prepost.PostFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.model.Permission
import org.springframework.transaction.annotation.Transactional


class UserService {
	static transactional = false
	
	def aclPermissionFactory
	def aclService
	def aclUtilService
	def springSecurityService


	def serviceMethod() {

	}
	
	@PreAuthorize("hasPermission(#id, 'org.imageterrier.webapp.security.User', admin) or hasRole('ROLE_ADMIN')")
	User get(long id) {
		User.get id
	}
}
