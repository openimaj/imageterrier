package org.imageterrier.webapp

class QueryOptionsController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index = {
        redirect(action: "list", params: params)
    }

    def list = {
        params.max = Math.min(params.max ? params.int('max') : 10, 100)
        [queryOptionsInstanceList: QueryOptions.list(params), queryOptionsInstanceTotal: QueryOptions.count()]
    }

    def create = {
        def queryOptionsInstance = new QueryOptions()
        queryOptionsInstance.properties = params
        return [queryOptionsInstance: queryOptionsInstance]
    }

    def save = {
        def queryOptionsInstance = new QueryOptions(params)
        if (queryOptionsInstance.save(flush: true)) {
            flash.message = "${message(code: 'default.created.message', args: [message(code: 'queryOptions.label', default: 'QueryOptions'), queryOptionsInstance.id])}"
            redirect(action: "show", id: queryOptionsInstance.id)
        }
        else {
            render(view: "create", model: [queryOptionsInstance: queryOptionsInstance])
        }
    }

    def show = {
        def queryOptionsInstance = QueryOptions.get(params.id)
        if (!queryOptionsInstance) {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'queryOptions.label', default: 'QueryOptions'), params.id])}"
            redirect(action: "list")
        }
        else {
            [queryOptionsInstance: queryOptionsInstance]
        }
    }

    def edit = {
        def queryOptionsInstance = QueryOptions.get(params.id)
        if (!queryOptionsInstance) {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'queryOptions.label', default: 'QueryOptions'), params.id])}"
            redirect(action: "list")
        }
        else {
            return [queryOptionsInstance: queryOptionsInstance]
        }
    }

    def update = {
        def queryOptionsInstance = QueryOptions.get(params.id)
        if (queryOptionsInstance) {
            if (params.version) {
                def version = params.version.toLong()
                if (queryOptionsInstance.version > version) {
                    
                    queryOptionsInstance.errors.rejectValue("version", "default.optimistic.locking.failure", [message(code: 'queryOptions.label', default: 'QueryOptions')] as Object[], "Another user has updated this QueryOptions while you were editing")
                    render(view: "edit", model: [queryOptionsInstance: queryOptionsInstance])
                    return
                }
            }
            queryOptionsInstance.properties = params
            if (!queryOptionsInstance.hasErrors() && queryOptionsInstance.save(flush: true)) {
                flash.message = "${message(code: 'default.updated.message', args: [message(code: 'queryOptions.label', default: 'QueryOptions'), queryOptionsInstance.id])}"
                redirect(action: "show", id: queryOptionsInstance.id)
            }
            else {
                render(view: "edit", model: [queryOptionsInstance: queryOptionsInstance])
            }
        }
        else {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'queryOptions.label', default: 'QueryOptions'), params.id])}"
            redirect(action: "list")
        }
    }

    def delete = {
        def queryOptionsInstance = QueryOptions.get(params.id)
        if (queryOptionsInstance) {
            try {
                queryOptionsInstance.delete(flush: true)
                flash.message = "${message(code: 'default.deleted.message', args: [message(code: 'queryOptions.label', default: 'QueryOptions'), params.id])}"
                redirect(action: "list")
            }
            catch (org.springframework.dao.DataIntegrityViolationException e) {
                flash.message = "${message(code: 'default.not.deleted.message', args: [message(code: 'queryOptions.label', default: 'QueryOptions'), params.id])}"
                redirect(action: "show", id: params.id)
            }
        }
        else {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'queryOptions.label', default: 'QueryOptions'), params.id])}"
            redirect(action: "list")
        }
    }
}
