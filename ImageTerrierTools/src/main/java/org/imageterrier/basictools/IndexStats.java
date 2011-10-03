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
 * The Original Code is IndexStats.java
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

import java.io.IOException;

import java.util.Arrays;
import java.util.Map.Entry;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineOptionsProvider;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ProxyOptionHandler;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.IterablePosting;

public class IndexStats {
	static {
		BasicTerrierConfig.configure();
	}
	
	enum Mode implements CmdLineOptionsProvider {
		TF {
			@Override
			public void execute(Index index) {
				Lexicon<String> lexicon = index.getLexicon();
				
				for (Entry<String, LexiconEntry> le : lexicon) {
					System.out.format("%s\t%d\t%d\n", le.getKey(), le.getValue().getDocumentFrequency(), le.getValue().getFrequency());
				}
			}
		},
		DOC {
			@Option(name="--docid", usage="document id")
			private int docid = -1;
			
			@Option(name="--key", usage="document metdata key")
			private String key;
			
			@Option(name="--value", usage="document metdata value")
			private String value;
			
			@Override
			public void execute(Index index) throws IOException {
				if ((docid>=0 & key==null & value==null) || (docid==-1 && (key!=null && value!=null))) {
					if (docid==-1) {
						docid = index.getMetaIndex().getDocument(key, value);
					}
					
					Lexicon<String> lexicon = index.getLexicon();
					
					System.out.println(Arrays.toString(index.getMetaIndex().getAllItems(docid)));
					
					for (Entry<String, LexiconEntry> le : lexicon) {
						IterablePosting posting = index.getInvertedIndex().getPostings((BitIndexPointer) le.getValue());
						
						while (posting.next() != IterablePosting.EOL) {
							if (posting.getId() == docid)
								System.out.println(le.getKey() + "\t" + posting.getFrequency());
						}
					}
				} else {
					System.err.println("Either docid or both key and value must be given.");
				}
			}
		},
		COUNT {

			@Override
			public void execute(Index index) throws IOException {
				int ndocs = index.getDocumentIndex().getNumberOfDocuments();
				
				Lexicon<String> lexicon = index.getLexicon();
				
				for (int i=0; i<ndocs; i++) {
					int count = 0;
					
					for (Entry<String, LexiconEntry> le : lexicon) {
						IterablePosting posting = index.getInvertedIndex().getPostings((BitIndexPointer) le.getValue());
						
						while (posting.next() != IterablePosting.EOL) {
							if (posting.getId() == i)
								count += posting.getFrequency() ;
						}
					}
					
					System.out.println(index.getMetaIndex().getItem("docno", i) + " " + index.getDocumentIndex().getDocumentEntry(i).getDocumentLength() + " " + count);
				}
			}
			
		}
		;
		
		@Override
		public Object getOptions() {
			return this;
		}
		
		public abstract void execute(Index index) throws IOException;
	}
	
	@Option(name = "--index", aliases="-i", usage="index to inspect", required=true)
	private String index_file;
	
	@Option(name = "--mode", aliases="-m", usage="inspection mode", required=true, handler=ProxyOptionHandler.class)
	private Mode mode;
	
	Index index;
	
	public void execute() throws IOException {
		index = Index.createIndex(index_file, "index");
		
		mode.execute(index);
	}
	
	public static void main(String [] args) throws IOException {
		IndexStats stats = new IndexStats();
		CmdLineParser parser = new CmdLineParser(stats);
		
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("Usage: java -jar ImageTerrier.jar IndexStats [options...]");
			parser.printUsage(System.err);
			
			if (stats.mode == null) {
				for (Mode m : Mode.values()) {
					System.err.println();
					System.err.println(m + " options: ");
					new CmdLineParser(m).printUsage(System.err);
				}
			}
			return;
		}
		
		stats.execute();
	}
}
