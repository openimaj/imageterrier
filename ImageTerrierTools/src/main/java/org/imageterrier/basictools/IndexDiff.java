package org.imageterrier.basictools;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.DocumentIndexEntry;
import org.terrier.structures.Index;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.IterablePosting;

public class IndexDiff {
	static {
		BasicTerrierConfig.configure();
	}
	
	@Option(name = "--index1", aliases="-i1", usage="first index to inspect", required=true)
	File index1;
	
	@Option(name = "--index2", aliases="-i2", usage="second index to inspect", required=true)
	File index2;
	
	public void diff() throws IOException {
		Index i1 = Index.createIndex(index1.getAbsolutePath(), "index");
		Index i2 = Index.createIndex(index2.getAbsolutePath(), "index");
		
		//lexicon
		if (!diffLexicon(i1, i2)) return;
		
		//document
		if (!diffDocument(i1, i2)) return;
		
		//inverted
		if (!diffInverted(i1, i2)) return;
		
		//meta
		if (!diffMeta(i1, i2)) return;
	}
	
	private boolean diffDocument(Index i1, Index i2) throws IOException {
		System.out.println("Comparing document");
		if (i1.getDocumentIndex().getNumberOfDocuments() != i2.getDocumentIndex().getNumberOfDocuments()) {
			System.err.println("ERROR: DIFFERENT number of documents");
			return false;
		} else {
			System.out.println("INFO: "+ i1.getDocumentIndex().getNumberOfDocuments() + " documents indexed");
		}
		
		for (int i=0; i<i1.getDocumentIndex().getNumberOfDocuments(); i++) {
			DocumentIndexEntry d1 = i1.getDocumentIndex().getDocumentEntry(i);
			DocumentIndexEntry d2 = i2.getDocumentIndex().getDocumentEntry(i2.getMetaIndex().getDocument("docno", i1.getMetaIndex().getItem("path", i).replace("/", "")));
			
			if (d1.getDocumentLength() != d2.getDocumentLength()) {
				System.err.println("ERROR: Document length DIFFERS");				
			}
		}
		
		return true;
	}

	private boolean diffMeta(Index i1, Index i2) {
		// TODO Auto-generated method stub
		return true;
	}

	private boolean diffInverted(Index i1, Index i2) throws IOException {
		Iterator<Map.Entry<String, LexiconEntry>> iter1 = i1.getLexicon().iterator();
		Iterator<Map.Entry<String, LexiconEntry>> iter2 = i2.getLexicon().iterator();
		
		for (int i=0; i<i1.getLexicon().numberOfEntries(); i++) {
			IterablePosting postings1 = i1.getInvertedIndex().getPostings((BitIndexPointer) iter1.next().getValue());
			LexiconEntry bip = iter2.next().getValue();
			
			while (postings1.next() != IterablePosting.EOL) {
				int freq1 = postings1.getFrequency();
				int l1 = postings1.getDocumentLength();
				int id1 = postings1.getId();
				
				int id2 = i2.getMetaIndex().getDocument("docno", i1.getMetaIndex().getItem("path", id1).replace("/", ""));
				IterablePosting postings2 = i2.getInvertedIndex().getPostings((BitIndexPointer) bip);
				postings2.next(id2);
				int l2 = postings2.getDocumentLength();
				int freq2 = postings2.getFrequency();
				
				if (l1 != l2) {
					System.err.println("ERROR: Different lengths");
					return false;
				}
				
				if (freq1 != freq2) {
					System.err.println("ERROR: Different frequencies");
					return false;
				}
			}
		}
		
		return true;
	}

	private boolean diffLexicon(Index i1, Index i2) {
		System.out.println("Comparing lexicon");
		int d = Math.abs(i1.getLexicon().numberOfEntries() - i2.getLexicon().numberOfEntries());
		if (d>0) {
			System.err.println("ERROR: Lexicons have the DIFFERENT size");
			return false;
		} else {
			System.out.println("INFO: Lexicons have " + i1.getLexicon().numberOfEntries() +" entries");
		}
		
		
		Iterator<Map.Entry<String, LexiconEntry>> iter1 = i1.getLexicon().iterator();
		Iterator<Map.Entry<String, LexiconEntry>> iter2 = i2.getLexicon().iterator();
		for (int i=0; i<i1.getLexicon().numberOfEntries(); i++) {
			Map.Entry<String, LexiconEntry> e1 = iter1.next();
			Map.Entry<String, LexiconEntry> e2 = iter2.next();
			
			if (!e1.getKey().equals(e2.getKey())) {
				System.err.println("Lexicon keys are DIFFERENT");
				return false;
			}
			
			if (e1.getValue().getDocumentFrequency() != e2.getValue().getDocumentFrequency()) {
				System.err.println("Lexicon Doc Frequencies are DIFFERENT");
				return false;
			}
			
			if (e1.getValue().getFrequency() != e2.getValue().getFrequency()) {
				System.err.println("Lexicon Frequencies are DIFFERENT");
				return false;
			}
			
			if (e1.getValue().getNumberOfEntries() != e2.getValue().getNumberOfEntries()) {
				System.err.println("Lexicon Num Entries are DIFFERENT");
				return false;
			}
			
			if (e1.getValue().getTermId() != e2.getValue().getTermId()) {
				System.err.println("Lexicon Term IDs are DIFFERENT");
				return false;
			}
		}
		return true;
	}

	public static void main(String[] args) throws Exception {
		args = new String[] {
				"-i1", "/Users/jsh2/testing/local-qf.idx",
				"-i2", "/Users/jsh2/testing/test.index",
		};
		
		
		IndexDiff diff = new IndexDiff();
		CmdLineParser parser = new CmdLineParser(diff);
		try {
		    parser.parseArgument(args);
		} catch(CmdLineException e) {
		    System.err.println(e.getMessage());
		    return;
		}
		
		diff.diff();
	}
}
