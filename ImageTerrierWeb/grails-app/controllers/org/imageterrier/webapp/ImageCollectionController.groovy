package org.imageterrier.webapp

class ImageCollectionController {

	static allowedMethods = [save: "POST", update: "POST"]

	def index = {
		redirect(action: "list", params: params)
	}

	def list = {
		params.max = Math.min(params.max ? params.int('max') : 10, 100)
		[imageCollectionInstanceList: ImageCollection.list(params), imageCollectionInstanceTotal: ImageCollection.count()]
	}
	def show = {
		def imageCollectionInstance = ImageCollection.get(params.id)
		if (!imageCollectionInstance) {
			flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'imageCollection.label', default: 'ImageCollection'), params.id])}"
			redirect(action: "list")
		}
		else {
			[imageCollectionInstance: imageCollectionInstance]
		}
	}
}
