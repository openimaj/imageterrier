package org.imageterrier.webapp

class Metadata {
    int imageTerrierId
    String imageURL
    String data
    
    static belongsTo = [imageCollection : ImageCollection]
    
    static constraints = {
    }
}
