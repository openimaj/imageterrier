package org.imageterrier.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.imageterrier.basictools.BasicTerrierConfig;
import org.imageterrier.locfile.QLFDocument;
import org.imageterrier.querying.parser.QLFDocumentQuery;
import org.jgrapht.Graph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.kohsuke.args4j.CmdLineException;
import org.openimaj.image.feature.local.keypoints.quantised.QuantisedKeypoint;
import org.openimaj.io.ReadWriteableASCII;
import org.openimaj.io.WriteableASCII;
import org.terrier.matching.ResultSet;
import org.terrier.querying.Manager;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.Index;
import org.terrier.utility.ApplicationSetup;

public class ImageClusterTool {
	
	static class ScoredImage{
		String name;
		double score;
		public ScoredImage(String name, double score){this.name = name; this.score=score;}
	}
	static class QueryResultsList extends ArrayList<ScoredImageList> implements ReadWriteableASCII{

		private boolean first = true;

		@Override
		public void readASCII(Scanner in) throws IOException {
			while(in.hasNextLine()){
				ScoredImageList list = new ScoredImageList();
				list.readASCII(in);
				in.nextLine();
				this.add(list);
			}
			
		}

		@Override
		public String asciiHeader() {
			return "";
		}

		@Override
		public void writeASCII(PrintWriter out) throws IOException {
			for(ScoredImageList list : this){
				if(first){
					first  = false;
				}
				else
					out.println();
				list.writeASCII(out);
			}
		}

		public SimpleGraph<String, DefaultEdge> asGraph() {
			SimpleGraph<String, DefaultEdge> ret = new SimpleGraph<String, DefaultEdge>(DefaultEdge.class);
			for(ScoredImageList list : this){
				ret.addVertex(list.query);
				for(ScoredImage result : list){
					if(result.name.equals(list.query)) continue;
					ret.addVertex(result.name);
					ret.addEdge(result.name, list.query);
				}
			}
			return ret;
		}
	}
	static class ScoredImageList extends ArrayList<ScoredImage> implements ReadWriteableASCII{
		String query;
		public ScoredImageList(String queryImageName) {
			this.query = queryImageName;
		}

		public ScoredImageList() {
			// TODO Auto-generated constructor stub
		}

		@Override
		public void readASCII(Scanner in) throws IOException {
			this.query = in.next();
			int count = in.nextInt();
			for(int i = 0; i < count; i++){
				ScoredImage image = new ScoredImage(in.next(), in.nextDouble());
				this.add(image);
			}
		}

		@Override
		public String asciiHeader() {
			return "";
		}

		@Override
		public void writeASCII(PrintWriter out) throws IOException {
			out.print(query);
			out.print(" ");
			out.print(this.size());
			out.print(" ");
			for(ScoredImage scoredImage : this){
				out.print(scoredImage.name + " " + scoredImage.score + " ");
			}
		}
		
	}
	
	ImageClusterToolOptions options = new ImageClusterToolOptions();
	
	static {
		BasicTerrierConfig.configure();
	}
	
	public ImageClusterTool(String[] args) throws CmdLineException {
		options.prepare(args);
	}

	private void run() {
		// Construct the query graph (either by performing the index search or by loading an existing set of results)
		SimpleGraph<String,DefaultEdge> imageGraph = options.getImageGraph();
		// Find the connected components
		ConnectivityInspector<String, DefaultEdge> isp = new ConnectivityInspector<String, DefaultEdge>(imageGraph);
		List<Set<String>> imageClusters = isp.connectedSets();
		// Output the clusters
		options.outputClusters(imageClusters);
		
	}
	
	public static void main(String args[]){
		try{
			ImageClusterTool tool = new ImageClusterTool(args);
			tool.run();
		}
		catch(CmdLineException e){
			System.err.println(e.getMessage());
		}
	}
}
