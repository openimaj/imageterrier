package org.imageterrier.webapp

import org.imageterrier.basictools.BasicSearcher
import org.imageterrier.basictools.BasicSearcherOptions
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

class ImageTerrierIndex {
    File indexPath
    String name
    String description

    static belongsTo = [imageCollections : ImageCollection]
    
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
		def path = indexLocation.getAbsolutePath()
		def searcher = loadIndex()
		String[] args = (setting.arguments + " -i " + path + " -l " + limit).trim().split("\\s+")
		
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
	def search(File imageFile, QueryOptions options, int limit=10) {
		def opts = reloadOptions(options.options, limit)
		def bs = loadIndex()
		
		def resultsSet = bs.search(imageFile, null, options)
		limit = Math.min(limit, resultsSet.getDocids().size())
		
		def res = []
		println "Preparing results"
		for (int i=0; i<limit; i++) {
			int id = resultsSet.getDocids()[i]
			
			def metaResults = ImageMetadata.findByImageTerrierId(id)
			metaResults[score] = resultsSet.getScores()[i]
			res << metaResults
		}
		return res
	}
}
