package org.imageterrier.webapp.bootstrap
import org.imageterrier.webapp.*

class ImporterCreationService{
	static transactional = false
	
	def serviceMethod() {

	}
	
	private String getText(String item) {
		return getClass().getResourceAsStream(item).text
	}
}