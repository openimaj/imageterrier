import org.imageterrier.webapp.*
import org.imageterrier.webapp.security.*

import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION
import static org.springframework.security.acls.domain.BasePermission.DELETE
import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils

import org.springframework.security.core.context.SecurityContextHolder as SCH

class BootStrap {
	def springSecurityService
	def aclService
	def aclUtilService
	def objectIdentityRetrievalStrategy
	def sessionFactory
	
	def addPermissionToDefault = { object ->
		aclUtilService.addPermission object, "ss", ADMINISTRATION
		aclUtilService.addPermission object, "admin", ADMINISTRATION
	}
	
/*	def init = {
		MetadataDeserializer md = new MetadataDeserializer(name:"a",description:"b",groovyClosure:"return {}");
		md.save()
		ImageCollection ic = new ImageCollection(name:"a",description:"b",deserializer:md)
		ImageTerrierIndex index = new ImageTerrierIndex(
			name: "a",
			shortName: "b",
			description: "c",
			indexPath: "d",
			imageCollection:ic)
		index.save()
	}*/
	
	def init = { servletContext ->
		loginAsAdmin()
		
		initAuth(servletContext)
		initImporter(servletContext)
		
		def rp = ResultsProcessor.update(
			"Geolocation Processor",
			"geolocation", 
			"Find the average geolocation of the result ",
			getText("/org/imageterrier/webapp/bootstrap/resultsprocessor/GeolocationProcessor.txt")
		)
		
		addPermissionToDefault(rp)
		
		if(QueryOptions.findByName("Affine L1IDF")==null){
			def setting = new QueryOptions(name:"Affine L1IDF",options:"-mm L1IDF -sm AFFINE",description:"")
			setting.save()
			addPermissionToDefault(setting)
			setting = new QueryOptions(name:"Ori L1IDF",options:"-mm L1IDF -sm CONSISTENT_ORI",description:"")
			setting.save()
			addPermissionToDefault(setting)
			setting = new QueryOptions(name:"Scale L1IDF",options:"-mm L1IDF -sm CONSISTENT_SCALE",description:"")
			setting.save()
			addPermissionToDefault(setting)
			setting = new QueryOptions(name:"None",options:"",description:"")
			setting.save()
			addPermissionToDefault(setting)
		}
		
		logout()
	}
	
	def destroy = {
	}
	
	private String getText(String item) {
		return getClass().getResourceAsStream(item).text
	}
	
	/** Authentication init **/

	
	
	def initAuth = {servletContext ->
		
		
		def defaultRoleName = "ROLE_NONE"
		def adminRoleName = "ROLE_ADMIN"
		def indexerRoleName = "ROLE_INDEXER"
		
		def defaultRole = Role.findByAuthority(defaultRoleName) ?: new Role(authority: defaultRoleName,description:"The null user, no login required").save(failOnError: true)
		def adminRole = Role.findByAuthority(adminRoleName) ?: new Role(authority: adminRoleName,description:"Admin, can change everything").save(failOnError: true)
		def indexerRole = Role.findByAuthority(indexerRoleName) ?: new Role(authority: indexerRoleName, description:"Indexers, add new indexes and control their searching").save(failOnError: true)
		
		def admin = User.findByUsername("admin") ?: new User(
			username:"admin",
			enabled:true,
			accountExpired:false,
			accountLocked:false,
			passwordExpired:false,
			password:springSecurityService.encodePassword("admin")
		).save()
		
		def ss = User.findByUsername("ss") ?: new User(
			username:"ss",
			enabled:true,
			accountExpired:false,
			accountLocked:false,
			passwordExpired:false,
			password:springSecurityService.encodePassword("wang")
		).save()
		
		UserRole.create admin, adminRole
		UserRole.create ss, indexerRole
		
		addPermissionToDefault(ss)
		
		aclUtilService.addPermission admin, admin.username, ADMINISTRATION
		
		sessionFactory.currentSession.flush()
	}
	
	private void logout(){
		SCH.clearContext()
	}
	private void loginAsAdmin() {
		// have to be authenticated as an admin to create ACLs
		SCH.context.authentication = new UsernamePasswordAuthenticationToken('admin', 'admin123',AuthorityUtils.createAuthorityList('ROLE_ADMIN'))
	}
	
	/** Importer init **/
	def initImporter = {servletContext ->
		if(MetadataDeserializer.findByName("FlickrCrawler CSV Deserializer")==null){
			new MetadataDeserializer(
				name:"FlickrCrawler CSV Deserializer", 
				description:"Deserialised individual records from an OpenIMAG FlickrCrawler run", 
				groovyClosure:getText("/org/imageterrier/webapp/bootstrap/deserializer/flickrcrawler.groovy.txt")
			).save()
		}
		if(MetadataDeserializer.findByName("FBK Wikipedia Infobox Deserializer")==null){
			new MetadataDeserializer(
				name:"FBK Wikipedia Infobox Deserializer", 
				description:"Deserialised individual records from the FBK Wikipedia Infobox format", 
				groovyClosure:getText("/org/imageterrier/webapp/bootstrap/deserializer/fbk_wikipedia_infobox.groovy.txt")
			).save()
		}
		
		if(MetadataDeserializer.findByName("OS Driving Video Deserializer")==null){
			new MetadataDeserializer(
				name:"OS Driving Video Deserializer", 
				description:"Deserialised individual records from the OS Driving Video CSV file", 
				groovyClosure:getText("/org/imageterrier/webapp/bootstrap/deserializer/osGOPR.groovy.txt")
			).save()
		}
		else{
			def deserializer = MetadataDeserializer.findByName("OS Driving Video Deserializer");
			deserializer.groovyClosure = getText("/org/imageterrier/webapp/bootstrap/deserializer/osGOPR.groovy.txt");
			deserializer.afterLoad()
			deserializer.save();
		}
		
		if(MetadataImporter.findByName("Basic FlickrCrawler CSV Importer")==null){
			new MetadataImporter(
				name:"Basic FlickrCrawler CSV Importer", 
				description:"Imports records from an OpenIMAG FlickrCrawler run",
				groovyClosure:getText("/org/imageterrier/webapp/bootstrap/importer/flickrcrawler.groovy.txt")
			).save()
		}
		if(MetadataImporter.findByName("FBK Wikipedia Infobox Importer")==null){
			new MetadataImporter(
				name:"FBK Wikipedia Infobox Importer", 
				description:"TSV wikipedia importer",
				groovyClosure:getText("/org/imageterrier/webapp/bootstrap/importer/fbk_wikipedia_infobox.groovy.txt")
			).save()
		}
		
		if(MetadataImporter.findByName("OS Driving Video Importer")==null){
			new MetadataImporter(
				name:"OS Driving Video Importer", 
				description:"Deserialised individual records from the OS Driving Video CSV file", 
				groovyClosure:getText("/org/imageterrier/webapp/bootstrap/importer/osGOPR.groovy.txt")
			).save()
		}else{
			def importer = MetadataImporter.findByName("OS Driving Video Importer");
			importer.groovyClosure = getText("/org/imageterrier/webapp/bootstrap/importer/osGOPR.groovy.txt");
			importer.afterLoad()
			importer.save();
		}
	}
	
}
