package org.imageterrier.webapp

import org.imageterrier.basictools.BasicSearcher
import org.imageterrier.basictools.BasicSearcherOptions
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;


class ImageTerrierIndex {
	File indexPath
	String name
	String shortName
	String description
	ImageCollection imageCollection
	
	static constraints = {
	}
	
	//static map of loaded indexes
	static def basicSearchers = [:]
	
	//load an index
	def loadIndex() {
		def path = indexPath.getAbsolutePath()
		if (!basicSearchers.containsKey(path)) {
			String argsStr = "-i " + path
			String[] args = argsStr.split(" ")
			BasicSearcherOptions options = new BasicSearcherOptions();
			CmdLineParser parser = new CmdLineParser(options);
			
			try {
				parser.parseArgument(args);
			} catch(CmdLineException e) {
				e.printStackTrace()
				return null
			}
			
			basicSearchers[path] = new BasicSearcher(options)
		}

		return basicSearchers[path]
	}

	//reload the options object
	def reloadOptions(String setting, int limit=10) {
		def path = indexPath.getAbsolutePath()
		def searcher = loadIndex()
		String[] args = (setting + " -i " + path + " -l " + limit).trim().split("\\s+")
		
		BasicSearcherOptions options = new BasicSearcherOptions();
		CmdLineParser parser = new CmdLineParser(options);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			e.printStackTrace()
			return null
		}
		
		return options
	}
	
	//perform a search using the index
	def search(File imageFile, String options, int limit=10) {
		def opts = reloadOptions(options, limit)
		def bs = loadIndex()
		
		def resultsSet = bs.search(imageFile, null, opts)
		limit = Math.min(limit, resultsSet.getDocids().size())
		
		def res = []
		long time = System.currentTimeMillis();
		log.info("Preparing results id array")
		def idArr = resultsSet.getDocids()[0..limit-1]
		def scoreArr = resultsSet.getScores();
		def scoreMap = [:]
		idArr.eachWithIndex { num,index ->
			scoreMap[num] = scoreArr[index]
		}
		
		log.info("Done, took: " + (System.currentTimeMillis() - time + "ms"))
		time = System.currentTimeMillis();
		log.info("Grabbing all metadata")
		def metadataC = Metadata.createCriteria()
		def metaResults = metadataC.list {
			eq("imageCollection",imageCollection)
			'in'("imageTerrierId",idArr)
		}
		log.info("Done, took: " + (System.currentTimeMillis() - time + "ms"))
		time = System.currentTimeMillis();
		log.info("Deserializing data")
		metaResults.each { metaResult ->
			def score = scoreMap[metaResult.imageTerrierId]
			metaResult = imageCollection.deserializer.deserialize(metaResult.data)
			metaResult["score"] = score
			res << metaResult
		}
		log.info("Done, took: " + (System.currentTimeMillis() - time + "ms"))
		time = System.currentTimeMillis();
		return res
	}
}
