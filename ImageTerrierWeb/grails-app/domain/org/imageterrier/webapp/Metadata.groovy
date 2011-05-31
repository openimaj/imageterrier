package org.imageterrier.webapp

class Metadata {
    int imageTerrierId
    String imageURL
    String data
    ImageCollection collection
    
    static belongsTo = [imageCollection : ImageCollection]
    
    static constraints = {
    }
}
