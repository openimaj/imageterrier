package org.imageterrier.webapp

class MetadataDeserializer {
    String name
    String description
    String groovyClosure
    
    transient def compiledClosure
    
    static constraints = {
    }
    
    def afterLoad = {
        GroovyShell gs = new GroovyShell(this.getClass().getClassLoader())
	    compiledClosure = gs.evaluate(groovyClosure)
    }
    
    def deserialize(String metadata) {
        return compiledClosure(metadata)
    }
    
    def deserialize(Metadata metadata) {
        return compiledClosure(metadata.data)
    }
}
