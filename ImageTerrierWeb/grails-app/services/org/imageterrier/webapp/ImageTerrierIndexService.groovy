package org.imageterrier.webapp
import org.springframework.security.access.prepost.PostFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.model.Permission
import org.springframework.transaction.annotation.Transactional

class ImageTerrierIndexService {
	static transactional = false
	
	def aclPermissionFactory
	def aclService
	def aclUtilService
	def springSecurityService


	def serviceMethod() {

	}
	
	@PreAuthorize("hasPermission(#id, 'org.imageterrier.webapp.ImageTerrierIndex', admin) or hasRole('ROLE_ADMIN')")
	ImageTerrierIndex get(long id) {
		ImageTerrierIndex.get id
	}
}
