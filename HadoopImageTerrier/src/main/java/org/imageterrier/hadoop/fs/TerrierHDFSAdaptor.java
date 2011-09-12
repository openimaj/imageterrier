package org.imageterrier.hadoop.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.log4j.Logger;
import org.terrier.utility.Files;
import org.terrier.utility.Files.FSCapability;
import org.terrier.utility.io.FileSystem;
import org.terrier.utility.io.RandomDataInput;
import org.terrier.utility.io.RandomDataOutput;

public class TerrierHDFSAdaptor implements FileSystem {
	protected static final Logger logger = Logger.getLogger(TerrierHDFSAdaptor.class);
	
	org.apache.hadoop.fs.FileSystem hdfs;

	public TerrierHDFSAdaptor(Configuration config) throws IOException {
		hdfs = org.apache.hadoop.fs.FileSystem.get(config);
	}

	@Override
	public String name() {
		return "hdfs";
	}

	/** capabilities of the filesystem */
	@Override
	public byte capabilities() {
		return FSCapability.READ | FSCapability.WRITE
				| FSCapability.RANDOM_READ | FSCapability.STAT
				| FSCapability.DEL_ON_EXIT | FSCapability.LS_DIR;
	}

	@Override
	public String[] schemes() {
		return new String[] { "dfs", "hdfs" };
	}

	/** returns true if the path exists */
	@Override
	public boolean exists(String filename) throws IOException {
		if (logger.isDebugEnabled())
			logger.debug("Checking that " + filename + " exists answer="
					+ hdfs.exists(new Path(filename)));
		return hdfs.exists(new Path(filename));
	}

	/** open a file of given filename for reading */
	@Override
	public InputStream openFileStream(String filename) throws IOException {
		if (logger.isDebugEnabled())
			logger.debug("Opening " + filename);
		return hdfs.open(new Path(filename));
	}

	/** open a file of given filename for writing */
	@Override
	public OutputStream writeFileStream(String filename) throws IOException {
		if (logger.isDebugEnabled())
			logger.debug("Creating " + filename);
		return hdfs.create(new Path(filename));
	}

	@Override
	public boolean mkdir(String filename) throws IOException {
		return hdfs.mkdirs(new Path(filename));
	}

	@Override
	public RandomDataOutput writeFileRandom(String filename) throws IOException {
		throw new IOException("HDFS does not support random writing");
	}

	@Override
	public RandomDataInput openFileRandom(String filename) throws IOException {
		return new HDFSRandomAccessFile(hdfs, filename);
	}

	@Override
	public boolean delete(String filename) throws IOException {
		return hdfs.delete(new Path(filename), true);
	}

	@Override
	public boolean deleteOnExit(String filename) throws IOException {
		return hdfs.deleteOnExit(new Path(filename));
	}

	@Override
	public String[] list(String path) throws IOException {
		final FileStatus[] contents = hdfs.listStatus(new Path(path));
		final String[] names = new String[contents.length];
		for (int i = 0; i < contents.length; i++) {
			names[i] = contents[i].getPath().getName();
		}
		return names;
	}

	@Override
	public String getParent(String path) throws IOException {
		return new Path(path).getParent().getName();
	}

	@Override
	public boolean rename(String source, String destination) throws IOException {
		return hdfs.rename(new Path(source), new Path(destination));
	}

	@Override
	public boolean isDirectory(String path) throws IOException {
		return hdfs.getFileStatus(new Path(path)).isDir();
	}

	@Override
	public long length(String path) throws IOException {
		return hdfs.getFileStatus(new Path(path)).getLen();
	}

	@Override
	public boolean canWrite(String path) throws IOException {
		return hdfs.getFileStatus(new Path(path)).getPermission()
				.getUserAction().implies(FsAction.WRITE);
	}

	@Override
	public boolean canRead(String path) throws IOException {
		return hdfs.getFileStatus(new Path(path)).getPermission()
				.getUserAction().implies(FsAction.READ);
	}
	
	public static synchronized void initialiseHDFSAdaptor(Configuration config) throws IOException {
		Files.addFileSystemCapability(new TerrierHDFSAdaptor(config));
	}
}
