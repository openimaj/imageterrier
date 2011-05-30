package org.imageterrier.webapp

class ImageCollection {
    String name
    String description
    MetadataDeserializer deserializer
    
    static hasMany = [metadata : Metadata, indexes : ImageTerrierIndex]

    static constraints = {
    }
}
