package org.imageterrier.webapp

class ImageCollectionController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index = {
        redirect(action: "list", params: params)
    }

    def list = {
        params.max = Math.min(params.max ? params.int('max') : 10, 100)
        [imageCollectionInstanceList: ImageCollection.list(params), imageCollectionInstanceTotal: ImageCollection.count()]
    }

    def create = {
        def imageCollectionInstance = new ImageCollection()
        imageCollectionInstance.properties = params
        return [imageCollectionInstance: imageCollectionInstance]
    }

    def save = {
        def imageCollectionInstance = new ImageCollection(params)
        if (imageCollectionInstance.save(flush: true)) {
            flash.message = "${message(code: 'default.created.message', args: [message(code: 'imageCollection.label', default: 'ImageCollection'), imageCollectionInstance.id])}"
            redirect(action: "show", id: imageCollectionInstance.id)
        }
        else {
            render(view: "create", model: [imageCollectionInstance: imageCollectionInstance])
        }
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

    def edit = {
        def imageCollectionInstance = ImageCollection.get(params.id)
        if (!imageCollectionInstance) {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'imageCollection.label', default: 'ImageCollection'), params.id])}"
            redirect(action: "list")
        }
        else {
            return [imageCollectionInstance: imageCollectionInstance]
        }
    }

    def update = {
        def imageCollectionInstance = ImageCollection.get(params.id)
        if (imageCollectionInstance) {
            if (params.version) {
                def version = params.version.toLong()
                if (imageCollectionInstance.version > version) {
                    
                    imageCollectionInstance.errors.rejectValue("version", "default.optimistic.locking.failure", [message(code: 'imageCollection.label', default: 'ImageCollection')] as Object[], "Another user has updated this ImageCollection while you were editing")
                    render(view: "edit", model: [imageCollectionInstance: imageCollectionInstance])
                    return
                }
            }
            imageCollectionInstance.properties = params
            if (!imageCollectionInstance.hasErrors() && imageCollectionInstance.save(flush: true)) {
                flash.message = "${message(code: 'default.updated.message', args: [message(code: 'imageCollection.label', default: 'ImageCollection'), imageCollectionInstance.id])}"
                redirect(action: "show", id: imageCollectionInstance.id)
            }
            else {
                render(view: "edit", model: [imageCollectionInstance: imageCollectionInstance])
            }
        }
        else {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'imageCollection.label', default: 'ImageCollection'), params.id])}"
            redirect(action: "list")
        }
    }

    def delete = {
        def imageCollectionInstance = ImageCollection.get(params.id)
        if (imageCollectionInstance) {
            try {
                imageCollectionInstance.delete(flush: true)
                flash.message = "${message(code: 'default.deleted.message', args: [message(code: 'imageCollection.label', default: 'ImageCollection'), params.id])}"
                redirect(action: "list")
            }
            catch (org.springframework.dao.DataIntegrityViolationException e) {
                flash.message = "${message(code: 'default.not.deleted.message', args: [message(code: 'imageCollection.label', default: 'ImageCollection'), params.id])}"
                redirect(action: "show", id: params.id)
            }
        }
        else {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'imageCollection.label', default: 'ImageCollection'), params.id])}"
            redirect(action: "list")
        }
    }
}
