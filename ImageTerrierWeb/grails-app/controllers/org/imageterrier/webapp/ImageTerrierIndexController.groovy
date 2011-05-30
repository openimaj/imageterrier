package org.imageterrier.webapp

class ImageTerrierIndexController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index = {
        redirect(action: "list", params: params)
    }

    def list = {
        params.max = Math.min(params.max ? params.int('max') : 10, 100)
        [imageTerrierIndexInstanceList: ImageTerrierIndex.list(params), imageTerrierIndexInstanceTotal: ImageTerrierIndex.count()]
    }

    def create = {
        def imageTerrierIndexInstance = new ImageTerrierIndex()
        imageTerrierIndexInstance.properties = params
        return [imageTerrierIndexInstance: imageTerrierIndexInstance]
    }

    def save = {
        def imageTerrierIndexInstance = new ImageTerrierIndex(params)
        if (imageTerrierIndexInstance.save(flush: true)) {
            flash.message = "${message(code: 'default.created.message', args: [message(code: 'imageTerrierIndex.label', default: 'ImageTerrierIndex'), imageTerrierIndexInstance.id])}"
            redirect(action: "show", id: imageTerrierIndexInstance.id)
        }
        else {
            render(view: "create", model: [imageTerrierIndexInstance: imageTerrierIndexInstance])
        }
    }

    def show = {
        def imageTerrierIndexInstance = ImageTerrierIndex.get(params.id)
        if (!imageTerrierIndexInstance) {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'imageTerrierIndex.label', default: 'ImageTerrierIndex'), params.id])}"
            redirect(action: "list")
        }
        else {
            [imageTerrierIndexInstance: imageTerrierIndexInstance]
        }
    }

    def edit = {
        def imageTerrierIndexInstance = ImageTerrierIndex.get(params.id)
        if (!imageTerrierIndexInstance) {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'imageTerrierIndex.label', default: 'ImageTerrierIndex'), params.id])}"
            redirect(action: "list")
        }
        else {
            return [imageTerrierIndexInstance: imageTerrierIndexInstance]
        }
    }

    def update = {
        def imageTerrierIndexInstance = ImageTerrierIndex.get(params.id)
        if (imageTerrierIndexInstance) {
            if (params.version) {
                def version = params.version.toLong()
                if (imageTerrierIndexInstance.version > version) {
                    
                    imageTerrierIndexInstance.errors.rejectValue("version", "default.optimistic.locking.failure", [message(code: 'imageTerrierIndex.label', default: 'ImageTerrierIndex')] as Object[], "Another user has updated this ImageTerrierIndex while you were editing")
                    render(view: "edit", model: [imageTerrierIndexInstance: imageTerrierIndexInstance])
                    return
                }
            }
            imageTerrierIndexInstance.properties = params
            if (!imageTerrierIndexInstance.hasErrors() && imageTerrierIndexInstance.save(flush: true)) {
                flash.message = "${message(code: 'default.updated.message', args: [message(code: 'imageTerrierIndex.label', default: 'ImageTerrierIndex'), imageTerrierIndexInstance.id])}"
                redirect(action: "show", id: imageTerrierIndexInstance.id)
            }
            else {
                render(view: "edit", model: [imageTerrierIndexInstance: imageTerrierIndexInstance])
            }
        }
        else {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'imageTerrierIndex.label', default: 'ImageTerrierIndex'), params.id])}"
            redirect(action: "list")
        }
    }

    def delete = {
        def imageTerrierIndexInstance = ImageTerrierIndex.get(params.id)
        if (imageTerrierIndexInstance) {
            try {
                imageTerrierIndexInstance.delete(flush: true)
                flash.message = "${message(code: 'default.deleted.message', args: [message(code: 'imageTerrierIndex.label', default: 'ImageTerrierIndex'), params.id])}"
                redirect(action: "list")
            }
            catch (org.springframework.dao.DataIntegrityViolationException e) {
                flash.message = "${message(code: 'default.not.deleted.message', args: [message(code: 'imageTerrierIndex.label', default: 'ImageTerrierIndex'), params.id])}"
                redirect(action: "show", id: params.id)
            }
        }
        else {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'imageTerrierIndex.label', default: 'ImageTerrierIndex'), params.id])}"
            redirect(action: "list")
        }
    }
}
