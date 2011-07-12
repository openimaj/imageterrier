package org.imageterrier.webapp

class ImageCollection {
	String name
	String description
	MetadataDeserializer deserializer
	static belongsTo = [index:ImageTerrierIndex]
	static hasMany = [metadata : Metadata]
	
	static constraints = {
	}
}
