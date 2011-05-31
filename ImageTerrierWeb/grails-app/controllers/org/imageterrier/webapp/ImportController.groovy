package org.imageterrier.webapp

class ImportController {
    def index = {
        
    }
    
    def importData = {
        ImageCollection collection = new ImageCollection(params.collection)
        ImageTerrierIndex index = new ImageTerrierIndex(params.index)
        
        collection.addToIndexes(index).save()
                
        MetadataImporter importer = MetadataImporter.get(params.importer.id)
        File dataFile = new File(params.datapath)
        
        importer.importData(dataFile, collection, index)
    }
}