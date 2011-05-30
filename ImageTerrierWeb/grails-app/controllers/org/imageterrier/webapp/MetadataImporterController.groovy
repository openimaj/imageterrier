package org.imageterrier.webapp

class MetadataImporterController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index = {
        redirect(action: "list", params: params)
    }

    def list = {
        params.max = Math.min(params.max ? params.int('max') : 10, 100)
        [metadataImporterInstanceList: MetadataImporter.list(params), metadataImporterInstanceTotal: MetadataImporter.count()]
    }

    def create = {
        def metadataImporterInstance = new MetadataImporter()
        metadataImporterInstance.properties = params
        return [metadataImporterInstance: metadataImporterInstance]
    }

    def save = {
        def metadataImporterInstance = new MetadataImporter(params)
        if (metadataImporterInstance.save(flush: true)) {
            flash.message = "${message(code: 'default.created.message', args: [message(code: 'metadataImporter.label', default: 'MetadataImporter'), metadataImporterInstance.id])}"
            redirect(action: "show", id: metadataImporterInstance.id)
        }
        else {
            render(view: "create", model: [metadataImporterInstance: metadataImporterInstance])
        }
    }

    def show = {
        def metadataImporterInstance = MetadataImporter.get(params.id)
        if (!metadataImporterInstance) {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'metadataImporter.label', default: 'MetadataImporter'), params.id])}"
            redirect(action: "list")
        }
        else {
            [metadataImporterInstance: metadataImporterInstance]
        }
    }

    def edit = {
        def metadataImporterInstance = MetadataImporter.get(params.id)
        if (!metadataImporterInstance) {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'metadataImporter.label', default: 'MetadataImporter'), params.id])}"
            redirect(action: "list")
        }
        else {
            return [metadataImporterInstance: metadataImporterInstance]
        }
    }

    def update = {
        def metadataImporterInstance = MetadataImporter.get(params.id)
        if (metadataImporterInstance) {
            if (params.version) {
                def version = params.version.toLong()
                if (metadataImporterInstance.version > version) {
                    
                    metadataImporterInstance.errors.rejectValue("version", "default.optimistic.locking.failure", [message(code: 'metadataImporter.label', default: 'MetadataImporter')] as Object[], "Another user has updated this MetadataImporter while you were editing")
                    render(view: "edit", model: [metadataImporterInstance: metadataImporterInstance])
                    return
                }
            }
            metadataImporterInstance.properties = params
            if (!metadataImporterInstance.hasErrors() && metadataImporterInstance.save(flush: true)) {
                flash.message = "${message(code: 'default.updated.message', args: [message(code: 'metadataImporter.label', default: 'MetadataImporter'), metadataImporterInstance.id])}"
                redirect(action: "show", id: metadataImporterInstance.id)
            }
            else {
                render(view: "edit", model: [metadataImporterInstance: metadataImporterInstance])
            }
        }
        else {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'metadataImporter.label', default: 'MetadataImporter'), params.id])}"
            redirect(action: "list")
        }
    }

    def delete = {
        def metadataImporterInstance = MetadataImporter.get(params.id)
        if (metadataImporterInstance) {
            try {
                metadataImporterInstance.delete(flush: true)
                flash.message = "${message(code: 'default.deleted.message', args: [message(code: 'metadataImporter.label', default: 'MetadataImporter'), params.id])}"
                redirect(action: "list")
            }
            catch (org.springframework.dao.DataIntegrityViolationException e) {
                flash.message = "${message(code: 'default.not.deleted.message', args: [message(code: 'metadataImporter.label', default: 'MetadataImporter'), params.id])}"
                redirect(action: "show", id: params.id)
            }
        }
        else {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'metadataImporter.label', default: 'MetadataImporter'), params.id])}"
            redirect(action: "list")
        }
    }
}
