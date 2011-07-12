package org.imageterrier.webapp

class MetadataDeserializer {
	String name
	String description
	String groovyClosure
	
	transient def compiledClosure
	transient def compilationError
	static constraints = {
	}
	
	def afterLoad = {
		GroovyShell gs = new GroovyShell(this.getClass().getClassLoader())
		try{
			compiledClosure = gs.evaluate(groovyClosure)
			compilationError = null
		}
		catch(Exception e){
			compiledClosure = gs.evaluate("return {a -> a}")
			compilationError = e;
		}
	}
	
	def deserialize(String metadata) {
		return compiledClosure(metadata)
	}
	
	def deserialize(Metadata metadata) {
		return compiledClosure(metadata.data)
	}
}
