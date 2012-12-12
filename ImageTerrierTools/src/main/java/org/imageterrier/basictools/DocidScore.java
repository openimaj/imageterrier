package org.imageterrier.basictools;

import org.openimaj.util.pair.IntDoublePair;
import org.terrier.structures.Index;

public class DocidScore extends IntDoublePair{

	public Index index;

	public DocidScore(int i, double d) {
		this.first = i;
		this.second = d;
	}

}
