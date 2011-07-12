package org.imageterrier.webapp

class ResultsProcessor {
	String name
	String description
	String groovyClosure
	String shortName = ""
	
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
			compiledClosure = gs.evaluate("return {a,b -> a}")
			compilationError = e;
		}
		
	}
	
	def process(results){
		return compiledClosure(results,[:])
	}
	
	public static ResultsProcessor update(String name, String shortName, String description, groovyClosure){
		def proc = ResultsProcessor.findAllByName(name)
		if(proc!=null){
			proc.each({it.delete(flush:true)})
		}
		return new ResultsProcessor(
			name:name,
			shortName:shortName,
			description:description,
			groovyClosure:groovyClosure
		).save()
	}
	
	public static loadProcessor(id){
		def resultsProcessor = null;
		if(id != null && id != ""){
			try{
				resultsProcessor = ResultsProcessor.findByShortName(id)
				if(resultsProcessor == null){
					resultsProcessor = ResultsProcessor.get(id)
				}
			}
			catch(Exception e){
				
			}
			
		}
		
		if(resultsProcessor == null){
			def nullProc =  new ResultsProcessor(
				name:"null",
				description:"null",
				groovyClosure:"return {a,b -> a}") // The null processor
			nullProc.afterLoad()
			return nullProc
		}
		else{
			return resultsProcessor
		}
	}
}
