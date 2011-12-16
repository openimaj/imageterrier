package org.imageterrier.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.imageterrier.locfile.QLFDocument;
import org.imageterrier.querying.parser.QLFDocumentQuery;
import org.imageterrier.toolopts.MatchingModelType;
import org.imageterrier.toolopts.ScoreModifierType;
import org.imageterrier.tools.ImageClusterTool.QueryResultsList;
import org.imageterrier.tools.ImageClusterTool.ScoredImage;
import org.imageterrier.tools.ImageClusterTool.ScoredImageList;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ProxyOptionHandler;
import org.openimaj.feature.local.list.FileLocalFeatureList;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.image.feature.local.keypoints.quantised.QuantisedKeypoint;
import org.openimaj.io.IOUtils;
import org.openimaj.tools.clusterquantiser.ClusterQuantiserOptions;
import org.terrier.matching.ResultSet;
import org.terrier.querying.Manager;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.Index;
import org.terrier.utility.ApplicationSetup;

public class ImageClusterToolOptions {
	
	enum ClusterOutputMode{
		PLAIN{

			@Override
			public void outputCluster(List<Set<String>> imageClusters,PrintWriter clusterWriter) {
				boolean first = true;
				for(Set<String> cluster : imageClusters){
					if(first) first = false;
					else clusterWriter.println();
					for(String item : cluster){
						clusterWriter.print(item + " ");
					}
					clusterWriter.flush();
				}
			}

			@Override
			public String ext() {
				return "clusters";
			}
			
		},
		HTML{
			String htmlHEAD = "<html><head></head><body>";
			String htmlFOOT = "</body></html>";
			String imgTMPL = "<img style='width:100px' src='%s'/>";
			@Override
			public void outputCluster(List<Set<String>> imageClusters,PrintWriter clusterWriter) {
				boolean first = true;
				clusterWriter.println(htmlHEAD);
				for(Set<String> cluster : imageClusters){
					clusterWriter.println("<div>");
					for(String item : cluster){
						clusterWriter.println(String.format(imgTMPL,item));
					}
					clusterWriter.println("</div>");
					clusterWriter.println("<hr/>");
					clusterWriter.flush();
				}
				clusterWriter.println(htmlFOOT);
			}
			@Override
			public String ext() {
				return "clusters.html";
			}
			
		};
		public abstract void outputCluster(List<Set<String>> imageClusters,PrintWriter output);

		public abstract String ext();
	}
	
	@Option(name = "--index", aliases = "-i", usage = "index path", required = true, metaVar = "path")
	private File index;
	
	@Option(name = "--query-directory", aliases = "-qd", usage = "path containing every query local file in the index", required = true, metaVar = "path")
	private File queryDirectory;
	
	@Option(name = "--image-directory", aliases = "-id", usage = "path containing every image in the index", required = false, metaVar = "path")
	private File imageDirectory = null;
	
	@Option(name="--score-modifier", aliases="-sm", required=false, usage="Use specified model for re-ranking results.", handler=ProxyOptionHandler.class)
	private ScoreModifierType scoreModifier = ScoreModifierType.NONE;
	
	@Option(name="-matching-model", aliases="-mm", required=false, usage="Choose matching model",handler=ProxyOptionHandler.class)
	private MatchingModelType matchingModel = MatchingModelType.TFIDF;
	
	@Option(name = "--limit", aliases = "-l", usage = "limit the number of results returned", metaVar = "number")
	private int limit = 100;
	
	@Option(name = "--query-extention", aliases = "-qe", usage = "query file extention", required = false, metaVar = "path")
	private String quantisedExtention = "jpg.fv.loc";
	
	@Option(name = "--image-extention", aliases = "-ie", usage = "query file extention", required = false, metaVar = "path")
	private String imageExtention = "jpg";
	
	@Option(name = "--query-file-regex", aliases = "-qr", usage = "query file regex", required = false, metaVar = "path")
	private String quantisedFileRegex = null;
	
	@Option(name = "--cluster-output-mode", aliases = "-cm", usage = "How the clusters should be outputed", required = false, metaVar = "path")
	private ClusterOutputMode clusterOutputMode = ClusterOutputMode.PLAIN;

	@Option(name = "--output-base-name", aliases = "-o", usage = "the base name for output files. files will be: output.results and output.clusters", required = false, metaVar = "path")
	private String outputBase = "output";
	private File outputResultsFile = null;
	private File outputClustersFile = null;
	
	@Option(name = "--overwrite-output-results", aliases = "-rr", usage = "if output results file exists, overwrite and regenerate", required = false, metaVar = "path")
	boolean overwriteOutputResults = false;
	
	
	private CmdLineParser parser;

	private PrintWriter resultsStream;

	private SimpleGraph<String, DefaultEdge> existingGraph = null;

	private boolean firstResult = true;

	private PrintWriter clusterWriter;
	
	
	
	public void prepare(String[] args) throws CmdLineException {
		parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
			this.validate();
		} catch (CmdLineException e) {
			String message = "";
			message += e.getMessage() + "\n";
			message += "Usage: java -jar ImageClusterTool.jar [options...] [files...]" + "\n";
			
			StringWriter sw = new StringWriter();
			parser.printUsage(sw, null);
			
			message += sw.toString();
			message += ClusterQuantiserOptions.EXTRA_USAGE_INFO  + "\n";
			
			throw new CmdLineException(parser, message);
		}
		
		if(imageDirectory == null) imageDirectory = queryDirectory;
		// Prepare the output
		outputResultsFile = new File(outputBase + ".results");
		outputClustersFile = new File(outputBase + "." + this.clusterOutputMode.ext());
		
		try{
			if(outputResultsFile.exists()){
				if(overwriteOutputResults){
					outputResultsFile.delete();
					outputResultsFile.createNewFile();
					resultsStream = new PrintWriter(new FileOutputStream(outputResultsFile));
				}
				else{
					existingGraph  = IOUtils.read(outputResultsFile, ImageClusterTool.QueryResultsList.class).asGraph();
				}
			}
			else{
				outputResultsFile.createNewFile();
				resultsStream = new PrintWriter(new FileOutputStream(outputResultsFile));
			}
			
		}
		catch(Exception e){
			throw new CmdLineException(null,"Could not create output results file (" + this.outputResultsFile + "): " + e.getMessage());
		}
		try{
			if(outputClustersFile.exists()){
				outputClustersFile.delete();
			}
			outputClustersFile.createNewFile();
			clusterWriter = new PrintWriter(new FileOutputStream(outputClustersFile));
		}
		catch(Exception e){
			throw new CmdLineException(null,"Could not create output clusters file: " + e.getMessage());
		}
		
	}

	private void validate() throws CmdLineException{
		if(!this.index.exists()) throw new CmdLineException(null,"Index does not exist");
		if(!this.queryDirectory.exists()) throw new CmdLineException(null,"Query directory does not exist");
		if(!this.queryDirectory.isDirectory()) throw new CmdLineException(null,"Query directory is not a directory");
		if(this.imageDirectory != null && !this.imageDirectory.isDirectory()) throw new CmdLineException(null,"Query directory is not a directory");
		
		
	}

	public Index loadIndex() {
		File filename = getIndex();
		String filenameStr = filename.getAbsolutePath();
		Index index = Index.createIndex(filenameStr, "index");
		return index;
	}
	
	public QLFDocument<QuantisedKeypoint> getQuery(String documentName) throws IOException{
		String FILENAME_TEMPLATE = "%s/%s";
		String combinedFileName = String.format(FILENAME_TEMPLATE ,this.queryDirectory.getAbsolutePath(),documentName);
		LocalFeatureList<QuantisedKeypoint> qfeatures = FileLocalFeatureList.read(new File(combinedFileName), QuantisedKeypoint.class);
		QLFDocument<QuantisedKeypoint> d = new QLFDocument<QuantisedKeypoint>(qfeatures, combinedFileName, null);
		return d;
	}

	private File getIndex() {
		return index;
	}
	
	public ScoreModifierType getScoreModifierType() {
		return scoreModifier;
	}
	
	public MatchingModelType getMatchingModelType() {
		return matchingModel;
	}
	
	public int getLimit() {
		return limit;
	}

	public Iterable<QLFDocument<QuantisedKeypoint>> getQueryIterator() {
		return new Iterable<QLFDocument<QuantisedKeypoint>>(){
			@Override
			public Iterator<QLFDocument<QuantisedKeypoint>> iterator() {
				final String[] fileList = ImageClusterToolOptions.this.queryDirectory.list(new FilenameFilter() {
					@Override
					public boolean accept(File file, String name) {
//						System.out.println(name+": " + quantisedFileRegex + ": "+ name.matches(quantisedFileRegex));
						if(ImageClusterToolOptions.this.quantisedFileRegex == null)
							return name.endsWith(ImageClusterToolOptions.this.quantisedExtention);
						else
							return name.matches(ImageClusterToolOptions.this.quantisedFileRegex);
					}
				});
				return new Iterator<QLFDocument<QuantisedKeypoint>>(){
					int index = 0;
					@Override
					public boolean hasNext() {
						return index < fileList.length;
					}

					@Override
					public QLFDocument<QuantisedKeypoint> next() {
						String filename = fileList[index++];
						try {
							return ImageClusterToolOptions.this.getQuery(filename);
						} catch (IOException e) {
							if(this.hasNext()) return this.next();
							return null;
						}
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
			
		};
	}

	public String getImageFromQuery(String query) {
		return new File(this.imageDirectory.getAbsolutePath(),query.replaceAll(quantisedExtention, imageExtention)).getAbsolutePath();
	}
	
	public void writeResults(String query, ResultSet results){
		
	}

	public void outputClusters(List<Set<String>> imageClusters) {
		clusterOutputMode.outputCluster(imageClusters, this.clusterWriter);
	}

	public void appendResults(ScoredImageList results) throws IOException {
		if(firstResult) firstResult  = false;
		else this.resultsStream.println();
		results.writeASCII(this.resultsStream);
		this.resultsStream.flush();
	}

	public SimpleGraph<String, DefaultEdge> getImageGraph() {
		if(existingGraph != null)
			return existingGraph ;
		Index index = this.loadIndex();
		
		Manager manager = new Manager(index);
		Iterable<QLFDocument<QuantisedKeypoint>> queryIterator = this.getQueryIterator();
		SimpleGraph<String,DefaultEdge> imageGraph = new SimpleGraph<String,DefaultEdge>(DefaultEdge.class);
		
		for (QLFDocument<QuantisedKeypoint> query : queryIterator) {
			QLFDocumentQuery<QuantisedKeypoint> q = new QLFDocumentQuery<QuantisedKeypoint>(query);
			File docno = new File(q.getDocument().getProperty("docno"));
			String queryImageName = this.getImageFromQuery(docno.getName());
			System.out.println("Doing query: " + queryImageName);
			SearchRequest request = manager.newSearchRequest("foo");
			request.setQuery(q);
			this.getMatchingModelType().configureRequest(request, q);
			ApplicationSetup.setProperty("matching.dsms", this.getScoreModifierType().getScoreModifierClass(index.getInvertedIndex()));
			
			ApplicationSetup.setProperty("ignore.low.idf.terms","false");
			
			if (this.getLimit() == 0)
				ApplicationSetup.setProperty("matching.retrieved_set_size", ""+index.getCollectionStatistics().getNumberOfDocuments());
			else
				ApplicationSetup.setProperty("matching.retrieved_set_size", ""+this.getLimit());
			
			manager.runPreProcessing(request);
			manager.runMatching(request);
			manager.runPostProcessing(request);
			manager.runPostFilters(request);
			
			ResultSet resultsSet = request.getResultSet();
			int limit = resultsSet.getDocids().length;
			ScoredImageList results = new ScoredImageList(queryImageName);
			imageGraph.addVertex(queryImageName);
			for (int i=0; i<limit; i++) {
				int docid = resultsSet.getDocids()[i];
				String name;
				try {
					name = index.getMetaIndex().getItem("path", docid);
				} catch (IOException e) {
					continue;
				}
				if(resultsSet.getScores()[i] <= 0) continue;
				String resultImageName = this.getImageFromQuery(name);
				results.add(new ScoredImage(resultImageName,resultsSet.getScores()[i]));
				if(!resultImageName.equals(queryImageName)){
					imageGraph.addVertex(resultImageName);
					imageGraph.addEdge(queryImageName, resultImageName);
				}
			}
			try {
				this.appendResults(results);
			} catch (IOException e) {
				System.err.println("Results Writing Failed!");
			}
		}
		return imageGraph;
	}
}
