package org.imageterrier.webapp

class Metadata {
    String imageTerrierId
    String imageURL
    String data
    ImageCollection collection
    
    static belongsTo = [imageCollection : ImageCollection]
    
    static constraints = {
    }
}
