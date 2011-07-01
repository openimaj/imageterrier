package org.imageterrier.webapp

class MetadataImporter {
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
    
    void importData(File data, ImageCollection collection, ImageTerrierIndex index) {
        compiledClosure(data, collection, index)
    }
    
    static Metadata createMetadata(int imageTerrierId, String imageURL, String data, ImageCollection collection) {
        Metadata md = new Metadata()
	    md.imageTerrierId = imageTerrierId
        md.imageURL = imageURL
        md.data = data
/*        md.imageCollection = collection*/
        
        collection.addToMetadata(md)
        
        return md
    }
}
