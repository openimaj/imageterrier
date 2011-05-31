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
    }
    
    def destroy = {
    }
    
    private String getText(String item) {
        return getClass().getResourceAsStream(item).text
    }
}
