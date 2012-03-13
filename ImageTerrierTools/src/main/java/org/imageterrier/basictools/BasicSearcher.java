/**
 * ImageTerrier - The Terabyte Retriever for Images
 * Webpage: http://www.imageterrier.org/
 * Contact: jsh2@ecs.soton.ac.uk
 * Electronics and Computer Science, University of Southampton
 * http://www.ecs.soton.ac.uk/
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is BasicSearcher.java
 *
 * The Original Code is Copyright (C) 2011 the University of Southampton
 * and the original contributors.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Jonathon Hare <jsh2@ecs.soton.ac.uk> (original contributor)
 *   Sina Samangooei <ss@ecs.soton.ac.uk>
 *   David Dupplaw <dpd@ecs.soton.ac.uk>
 */
package org.imageterrier.basictools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jline.ArgumentCompletor;
import jline.ConsoleReader;
import jline.FileNameCompletor;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.metadata.XmlRpcSystemImpl;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.webserver.ServletWebServer;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.imageterrier.basictools.BasicTerrierConfig;
import org.imageterrier.features.FeatureTask;
import org.imageterrier.features.QuantiserTask;
import org.imageterrier.locfile.QLFDocument;
import org.imageterrier.querying.parser.QLFDocumentQuery;
import org.imageterrier.structures.indexing.QuantiserIndex;
import org.imageterrier.toolopts.ScoreModifierType;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.quantised.QuantisedLocalFeature;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.ml.clustering.Cluster;
import org.openimaj.tools.localfeature.LocalFeatureMode;
import org.terrier.matching.ResultSet;
import org.terrier.querying.Manager;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.Index;
import org.terrier.utility.ApplicationSetup;


public class BasicSearcher {	
	static {
		BasicTerrierConfig.configure();
	}

	protected Index index;
	protected Cluster<?,?> cluster;
	
	public BasicSearcher(BasicSearcherOptions options) {
		File filename = options.getIndex();
		String filenameStr = filename.getAbsolutePath();
		index = Index.createIndex(filenameStr, "index");
		
		getQuantizer(); //preload the quantizer
	}
	
	public <T extends QuantisedLocalFeature<?>> ResultSet search(QLFDocument<T> query, BasicSearcherOptions options) {
		QLFDocumentQuery<T> q = new QLFDocumentQuery<T>(query);
		
		Manager manager = new Manager(index);
		SearchRequest request = manager.newSearchRequest("foo");
		request.setQuery(q);
		options.getMatchingModelType().configureRequest(request, q);
		ApplicationSetup.setProperty("matching.dsms", options.getScoreModifierType().getScoreModifierClass(index.getInvertedIndex()));
		
		ApplicationSetup.setProperty("ignore.low.idf.terms","false");
		
		if (options.getLimit() == 0)
			ApplicationSetup.setProperty("matching.retrieved_set_size", ""+index.getCollectionStatistics().getNumberOfDocuments());
		else
			ApplicationSetup.setProperty("matching.retrieved_set_size", ""+options.getLimit());
		
		manager.runPreProcessing(request);
		manager.runMatching(request);
		manager.runPostProcessing(request);
		manager.runPostFilters(request);
		
		return request.getResultSet();
	}
	
	public String getDocumentId(int docno) {
		try {
			return index.getMetaIndex().getItem("docno", docno);
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}
	public String getPath(int docid) throws IOException {
		return index.getMetaIndex().getItem("path", docid).replace(".fv.loc", "");
	}
	public File getFile(int docid) throws IOException {
		return new File(index.getIndexProperty("index.image.base.path", "/") + index.getMetaIndex().getItem("path", docid).replace(".fv.loc", ""));
	}
	
	public Cluster<?,?> getQuantizer() {
		if (cluster == null) {
			cluster = (Cluster<?,?>) ((QuantiserIndex)index.getIndexStructure("featureQuantiser")).getQuantiser();
			cluster.optimize(false);
		}
		return cluster;
	}

	public ResultSet search(File imageFile, int [] coords, BasicSearcherOptions options) throws Exception {
		Cluster<?,?> quantizer = getQuantizer();
		
		long t1 = System.currentTimeMillis();
		
		//process the image
		LocalFeatureMode mode = LocalFeatureMode.valueOf(index.getIndexProperty("index.feature.type", ""));
		LocalFeatureList<?> features = FeatureTask.computeFeatures(imageFile, mode.getOptions());
		
		LocalFeatureList<QuantisedLocalFeature<?>> qfeatures;
		if (options.getSoftQuantNeighbours() == 0)
			qfeatures = QuantiserTask.quantiseFeatures(quantizer, features);
		else
			qfeatures = QuantiserTask.quantiseFeaturesSoft(quantizer, features, options.getSoftQuantNeighbours());
		
		QLFDocument<QuantisedLocalFeature<?>> d = new QLFDocument<QuantisedLocalFeature<?>>(qfeatures, "query", null);
		
		if (coords != null) {
			Rectangle r = new Rectangle(coords[0], coords[1], coords[2], coords[3]);
			d.filter(r);
		}
		
		long t2 = System.currentTimeMillis();
		
		ResultSet rs = search(d, options);
		
		long t3 = System.currentTimeMillis();
		
		if (options.timeQuery()) {
			System.out.println("[INFO] Feature extraction took:	" + ((t2-t1) / 1000.0) + " secs");
			System.out.println("[INFO] Search took:				" + ((t3-t2) / 1000.0) + " secs");
		}
		
		return rs;
	}
	
	public void printResultSet(ResultSet rs, int limit) throws IOException {
		if (limit<=0) limit = rs.getDocids().length;
		
		for (int i=0; i<limit; i++) {
			File file = getFile(rs.getDocids()[i]);

			if (rs.getScores()[i] <= 0) break; //filter 0 results 
			
			System.out.format("%s\t%f\n", file, rs.getScores()[i]);
		}
	}
	
	public void displayResults(String title, ResultSet rs, int limit) throws IOException {
		if (limit<=0) limit = rs.getDocids().length;
		List<File> files = new ArrayList<File>();
		
		for (int i=0; i<limit; i++) {
			File file = getFile(rs.getDocids()[i]);

			if (rs.getScores()[i] <= 0) break; //filter 0 results 
			
			files.add(file);
		}
		
		displayImage(title, files.toArray(new File[files.size()]));
	}
	
	public void displayImage(String title, File... images) {
		if (images.length == 1) {
			try {
				DisplayUtilities.display(ImageUtilities.readMBF(images[0]), title);
			} catch (IOException e) {
				System.err.println("Unable to load " + images[0]);
			}
		} else {
			List<MBFImage> bimages = new ArrayList<MBFImage>();
			for (int i=0; i<images.length; i++) {
				try {
					bimages.add(ImageUtilities.readMBF(images[i]));
				} catch (IOException e) {
					System.err.println("Unable to load " + images[i]);
				}
			}
			DisplayUtilities.display(title, bimages.toArray(new MBFImage[bimages.size()]));
		}
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		BasicSearcherOptions options = new BasicSearcherOptions();
		CmdLineParser parser = new CmdLineParser(options);
		
	    try {
		    parser.parseArgument(args);
		} catch(CmdLineException e) {
		    System.err.println(e.getMessage());
		    System.err.println("Usage: java -jar ImageTerrier.jar [options...]");
		    parser.printUsage(System.err);
		    return;
		}

		BasicSearcher searcher = new BasicSearcher(options);

		if (!options.isInteractive() && !options.isServer()) {
			if (options.getQueryImage() == null) {
				System.err.println("Error: No query image provided.");
				System.err.println("Usage: java -jar ImageTerrier.jar [options...]");
			    parser.printUsage(System.err);
				return;
			}
			
			if (options.getQueryImage().isDirectory()) {
				for (File f : options.getQueryImage().listFiles()) {
					try {
						ResultSet rs = searcher.search(f, options.getRoiCoords(), options);
						System.out.println("Results from querying with: " + f.getName());
						searcher.printResultSet(rs, options.getLimit());
					} catch (Exception e) {
						//ignore it
					}
				}
			} else {
				if (options.displayQuery())
					searcher.displayImage("Query:  " + options.getQueryImage(), options.getQueryImage());
			
				ResultSet rs = searcher.search(options.getQueryImage(), options.getRoiCoords(), options); 
				searcher.printResultSet(rs, options.getLimit());
			
				if (options.displayResults())
					searcher.displayResults("Results:  " + options.getQueryImage(), rs, options.getLimit());
			}
		} else if (options.isServer()) { 
			BasicSearcherXmlRpcServlet.options = options;
			BasicSearcherXmlRpcServlet.searcher = searcher;
			XmlRpcServlet servlet = new XmlRpcServlet() {
				private static final long serialVersionUID = 1L;

				@Override
				protected XmlRpcHandlerMapping newXmlRpcHandlerMapping() throws XmlRpcException {
					PropertyHandlerMapping mapping = (PropertyHandlerMapping) super.newXmlRpcHandlerMapping();
					XmlRpcSystemImpl.addSystemHandler(mapping);
					return mapping;
                }
			};
	        ServletWebServer webServer = new ServletWebServer(servlet, options.serverPort());
	        webServer.start();
		} else {
			//interactive mode -- useful for debugging
			//Note to self: jline is awesome!
			ConsoleReader cr = new ConsoleReader();
			ArgumentCompletor ac = new ArgumentCompletor(new FileNameCompletor());
			ac.setStrict(false);
			cr.addCompletor(ac);
			while (true) {
				String cmd = cr.readLine("query> ").trim();
			
				if (cmd.length() == 0)
					continue;
			
				if (cmd.equals("exit") || cmd.equals("quit"))
					break;				
				
				if (!cmd.contains("-i ")) cmd += " -i " + options.getIndex();
				BasicSearcherOptions interactiveOptions = new BasicSearcherOptions();
				CmdLineParser interactiveParser = new CmdLineParser(interactiveOptions);
				
			    try {
			    	interactiveParser.parseArgument(cmd.split("\\s+"));
			    	
			    	if (interactiveOptions.getQueryImage() == null) {
			    		System.out.println("Error: No query image specified.");
			    		continue;
			    	}
			    	
			    	if (!interactiveOptions.getIndex().equals(options.getIndex())) {
			    		//reload index
			    		searcher = new BasicSearcher(interactiveOptions);
			    		options = interactiveOptions;
			    	}
			    	
			    	//print a description of the query
			    	System.out.print("SELECT IMG_FILE, SCORE FROM " + interactiveOptions.getIndex() + " WHERE IMG");
			    	if (interactiveOptions.getScoreModifierType() != ScoreModifierType.NONE) {
			    		System.out.print(" CONTAINS ");
			    	} else {
			    		System.out.print(" IS_SIMILAR_TO ");
			    	}
			    	System.out.print(interactiveOptions.getQueryImage());
			    	if (interactiveOptions.getRoiCoordsString() != null) 
			    		System.out.print("(" + interactiveOptions.getRoiCoordsString() + ")");
			    	if (interactiveOptions.getLimit() != 0) {
			    		System.out.print(" LIMIT " + interactiveOptions.getLimit());
			    	} else if (options.getLimit() != 0) {
			    		System.out.print(" LIMIT " + options.getLimit());
			    	}
			    	System.out.println();
			    	
			    	int limit = options.getLimit();
			    	if (interactiveOptions.getLimit() != 0) limit = interactiveOptions.getLimit();
			    	
			    	if (interactiveOptions.displayQuery())
						searcher.displayImage("Query:  " + interactiveOptions.getQueryImage(), interactiveOptions.getQueryImage());
					
					ResultSet rs = searcher.search(interactiveOptions.getQueryImage(), interactiveOptions.getRoiCoords(), interactiveOptions); 
					searcher.printResultSet(rs, limit);
					
					if (interactiveOptions.displayResults())
						searcher.displayResults("Results:  " + interactiveOptions.getQueryImage(), rs, limit);
			    } catch (CmdLineException cle) {
			    	System.out.println("Syntax error:" + cle.getMessage());
			    } catch (Exception e) {
			    	e.printStackTrace();
			    	System.out.println("Error: " + e.getMessage());
			    }
			}
		}
	}
}
