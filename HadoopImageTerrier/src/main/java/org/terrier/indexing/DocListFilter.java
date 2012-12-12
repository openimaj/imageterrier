package org.terrier.indexing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.openimaj.util.filter.Filter;

public class DocListFilter implements Filter<String> {
	Set<String> filterSet;
	public DocListFilter(FileSystem fs, Path filterFile) {
		filterSet = new HashSet<String>();
		try {
			FSDataInputStream is = fs.open(filterFile);
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String l = null;
			while((l = reader.readLine())!=null){
				filterSet.add(l.trim());
			}
		} catch (IOException e) {
			System.err.println("Failed to load file filter");
		}
	}

	@Override
	public boolean accept(String object) {
		return this.filterSet.contains(object.trim());
	}

}
