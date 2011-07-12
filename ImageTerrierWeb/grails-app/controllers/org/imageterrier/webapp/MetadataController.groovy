package org.imageterrier.webapp

class MetadataController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index = {
        redirect(action: "list", params: params)
    }

    def list = {
        params.max = Math.min(params.max ? params.int('max') : 10, 100)
        [metadataInstanceList: Metadata.list(params), metadataInstanceTotal: Metadata.count()]
    }
	def show = {
		def metadataInstance = Metadata.get(params.id)
		if (!metadataInstance) {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'metadata.label', default: 'Metdata'), params.id])}"
			redirect(action: "list")
		}
		else {
			[metadataInstance: metadataInstance]
		}
	}

}
