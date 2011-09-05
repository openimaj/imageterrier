package org.imageterrier.hadoop.fs;

import java.io.IOException;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.terrier.utility.io.RandomDataInput;

public class HDFSRandomAccessFile implements RandomDataInput {
	FSDataInputStream in;
	org.apache.hadoop.fs.FileSystem fs;
	String filename;

	public HDFSRandomAccessFile(org.apache.hadoop.fs.FileSystem fs, String filename) throws IOException {
		this.fs = fs;
		this.filename = filename;
		this.in = fs.open(new Path(filename));
	}

	public int read() throws IOException {
		return in.read();
	}

	public int read(byte b[], int off, int len) throws IOException {
		return in.read(in.getPos(), b, off, len);
	}

	public int readBytes(byte b[], int off, int len) throws IOException {
		return in.read(in.getPos(), b, off, len);
	}

	@Override
	public void seek(long pos) throws IOException {
		in.seek(pos);
	}

	@Override
	public long length() throws IOException {
		return fs.getFileStatus(new Path(filename)).getLen();
	}

	@Override
	public void close() throws IOException {
		in.close();
	}

	@Override
	public final double readDouble() throws IOException {
		return in.readDouble();
	}

	@Override
	public final int readUnsignedShort() throws IOException {
		return in.readUnsignedShort();
	}

	@Override
	public final short readShort() throws IOException {
		return in.readShort();
	}

	@Override
	public final int readUnsignedByte() throws IOException {
		return in.readUnsignedByte();
	}

	@Override
	public final byte readByte() throws IOException {
		return in.readByte();
	}

	@Override
	public final boolean readBoolean() throws IOException {
		return in.readBoolean();
	}

	@Override
	public final int readInt() throws IOException {
		return in.readInt();
	}

	@Override
	public final long readLong() throws IOException {
		return in.readLong();
	}

	@Override
	public final float readFloat() throws IOException {
		return in.readFloat();
	}

	@Override
	public final void readFully(byte b[]) throws IOException {
		in.readFully(b);
	}

	@Override
	public final void readFully(byte b[], int off, int len) throws IOException {
		in.readFully(b, off, len);
	}

	@Override
	public int skipBytes(int n) throws IOException {
		return in.skipBytes(n);
	}

	@Override
	public long getFilePointer() throws IOException {
		return in.getPos();
	}

	@Override
	public final char readChar() throws IOException {
		return in.readChar();
	}

	@Override
	public final String readUTF() throws IOException {
		return in.readUTF();
	}

	@Override
	@SuppressWarnings("deprecation")
	public final String readLine() throws IOException {
		return in.readLine();
	}
}
