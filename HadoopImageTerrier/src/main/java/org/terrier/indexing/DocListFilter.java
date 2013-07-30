package org.terrier.indexing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.openimaj.util.function.Predicate;

public class DocListFilter implements Predicate<String> {
	Set<String> filterSet;

	public DocListFilter(FileSystem fs, Path filterFile) {
		filterSet = new HashSet<String>();
		try {
			final FSDataInputStream is = fs.open(filterFile);
			final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String l = null;
			while ((l = reader.readLine()) != null) {
				filterSet.add(l.trim());
			}
		} catch (final IOException e) {
			System.err.println("Failed to load file filter");
		}
	}

	/**
	 * returns true if the doc should be indexed
	 */
	@Override
	public boolean test(String object) {
		return this.filterSet.contains(object.trim());
	}

}
