import org.imageterrier.webapp.*

class BootStrap {

	def init = { servletContext ->
		new MetadataDeserializer(
			name:"FlickrCrawler CSV Deserializer", 
			description:"Deserialised individual records from an OpenIMAG FlickrCrawler run", 
			groovyClosure:getText("/org/imageterrier/webapp/bootstrap/deserializer/flickrcrawler.groovy.txt")).save()
			
		new MetadataDeserializer(
			name:"FBK Wikipedia Infobox Deserializer", 
			description:"Deserialised individual records from the FBK Wikipedia Infobox format", 
			groovyClosure:getText("/org/imageterrier/webapp/bootstrap/deserializer/fbk_wikipedia_infobox.groovy.txt")).save()
			
			
		new MetadataImporter(
			name:"Basic FlickrCrawler CSV Importer", 
			description:"Imports records from an OpenIMAG FlickrCrawler run",
			groovyClosure:getText("/org/imageterrier/webapp/bootstrap/importer/flickrcrawler.groovy.txt")).save()
			
		new MetadataImporter(
			name:"FBK Wikipedia Infobox Importer", 
			description:"TSV wikipedia importer",
			groovyClosure:getText("/org/imageterrier/webapp/bootstrap/importer/fbk_wikipedia_infobox.groovy.txt")).save()
		if(QueryOptions.findByName("Affine L1IDF")==null){
			def setting = new QueryOptions(name:"Affine L1IDF",options:"-mm L1IDF -sm AFFINE",description:"")
			setting.save()
			setting = new QueryOptions(name:"Ori L1IDF",options:"-mm L1IDF -sm CONSISTENT_ORI",description:"")
			setting.save()
			setting = new QueryOptions(name:"Scale L1IDF",options:"-mm L1IDF -sm CONSISTENT_SCALE",description:"")
			setting.save()
			setting = new QueryOptions(name:"None",options:"",description:"")
			setting.save()
		}
	}
	
	def destroy = {
	}
	
	private String getText(String item) {
		return getClass().getResourceAsStream(item).text
	}
}
