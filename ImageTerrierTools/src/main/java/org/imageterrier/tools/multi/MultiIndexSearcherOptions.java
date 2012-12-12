package org.imageterrier.tools.multi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.imageterrier.basictools.BasicSearcherOptions;
import org.kohsuke.args4j.Option;

public class MultiIndexSearcherOptions extends BasicSearcherOptions{
	@Option(name = "--index-list", aliases = "-il", usage = "An index to include", required = true, metaVar = "path",multiValued=true)
	public List<File> index = new ArrayList<File>();

	public List<File> getIndexes(){
		return this.index;
	}



}
