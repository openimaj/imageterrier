package org.imageterrier.tools.multi;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.openimaj.time.Timer;
import org.terrier.compression.BitFileBuffered;
import org.terrier.compression.BitInSeekable;
import org.terrier.structures.Index;
import org.terrier.structures.InvertedIndex;

final class IndexLoader implements Runnable {
	private List<File> indexFiles;
	private List<IndexLoadCompleteListener> listeners = new ArrayList<IndexLoadCompleteListener>();

	/**
	 * @param bbcWorldSearcher
	 * @param baseDir
	 */
	public IndexLoader(List<File> indexFiles) {
		this.indexFiles = indexFiles;
	}

	@Override
	public void run() {
		// Load all the indecies
		ArrayList<Index> indecies = new ArrayList<Index>();
		Timer timer = Timer.timer();
		for (File file : indexFiles) {
			String indexName = file.getName();
			System.out.println("Loading index: " + indexName);
			Index index = Index.createIndex(file.getAbsolutePath(), "index");
			InvertedIndex ii = index.getInvertedIndex();
			for (BitInSeekable bf : ii.getBitFiles()) {
				BitFileBuffered bff = (BitFileBuffered) bf;
				Field dbl;
				try {
					dbl = BitFileBuffered.class.getDeclaredField("buffer_size");
					dbl.setAccessible(true);
					Field modifiersField = Field.class.getDeclaredField("modifiers");
					modifiersField.setAccessible(true);
					modifiersField.setInt(dbl, dbl.getModifiers() &~Modifier.FINAL);
					dbl.setInt(bff, 1024*16);
				} catch (Exception e) {
				}

			}
			indecies.add(index);
		}
		System.out.println(String.format("Indexes loaded took: %fs",timer.duration()/1000f));
		fireLoadedIndexes(indecies);
	}
	private void fireLoadedIndexes(ArrayList<Index> indecies) {
		for (IndexLoadCompleteListener l : this.listeners) {
			l.indexesLoaded(indecies);
		}
	}
	public static interface IndexLoadCompleteListener{
		public void indexesLoaded(List<Index> indexes);
	}
	public void addLoadCompleteListener(IndexLoadCompleteListener indexLoadCompleteListener) {
		this.listeners .add(indexLoadCompleteListener);
	}
}