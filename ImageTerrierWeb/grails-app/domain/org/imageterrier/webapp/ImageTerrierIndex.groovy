package org.imageterrier.webapp

class ImageTerrierIndex {
    File indexPath
    String name
    String description
    
    static hasMany = [collections : ImageCollection]
    static belongsTo = ImageCollection
    
    static constraints = {
    }
}
