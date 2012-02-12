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
 * The Original Code is PositionAwareSplitWrapper.java
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
package org.imageterrier.hadoop.mapreduce;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Constructor;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.terrier.utility.io.WrappedIOException;

/** 
 * A split implementation that knows which it is in the scheme of things
 * Based on the original Terrier PositionAwareSplit, but updated for Hadoop 0.20+
 * 
 * @author Jonathon Hare
 * @author Richard McCreadie
 * @param <T> the {@link FileSplit} type
 */
public class PositionAwareSplitWrapper<T extends FileSplit & Writable> extends FileSplit implements Writable {
	/** the wrapped split */
	protected T split;
	
	/** the index of this split */
	protected int splitnum;
	
	/** Make a new split, for use in Writable serialization */
	public PositionAwareSplitWrapper() {
		super(null,0,0,null);
		splitnum=-1;
	}
	
	/** Make a new split with the specified attributs 
	 * @param _split 
	 * @param _splitnum 
	 */
	public PositionAwareSplitWrapper(T _split, int _splitnum) {
		super(null,0,0,null);
		split = _split;
		splitnum = _splitnum;
	}

	/**
	 * Get the index of this split
	 * @return the splitnum
	 */
	public int getSplitIndex() {
		return splitnum;
	}

	/**
	 * Set the index of this split
	 * @param splitnum the splitnum to set
	 */
	public void setSplitIndex(int splitnum) {
		this.splitnum = splitnum;
	}

	/**
	 * Get the wrapped split
	 * @return the split
	 */
	public T getSplit() {
		return split;
	}

	/**
	 * Set the wrapped split
	 * @param split the split to set
	 */
	public void setSplit(T split) {
		this.split = split;
	}

	@Override
	public long getLength() {
		return split.getLength();
	}

	@Override
	public String[] getLocations() throws IOException {
		return split.getLocations();
	}

	@Override
	public Path getPath() {
		return split.getPath();
	}

	@Override
	public long getStart() {
		return split.getStart();
	}

	@Override
	public String toString() {
		return split.toString();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void readFields(DataInput in) throws IOException {
		try {
			final String className = in.readUTF();
			Class<?> c = Class.forName(className, false, this.getClass().getClassLoader());
			Constructor<?> constr = c.getDeclaredConstructor();
			constr.setAccessible(true);
			split = (T)constr.newInstance();
			split.readFields(in);
			splitnum = in.readInt();
		} catch (Exception e) {
			throw new WrappedIOException("Error during the reading of fields of a new PositionAwareSplitWrapper", e);
		} 
	}

	/** {@inheritDoc} */
	@Override
	public void write(DataOutput out) throws IOException {
		out.writeUTF(split.getClass().getName());
		split.write(out);
		out.writeInt(splitnum);
	}
}
