package org.imageterrier.tools.multi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openimaj.io.IOUtils;
import org.openimaj.ml.clustering.CentroidsProvider;
import org.openimaj.ml.clustering.SpatialClusters;
import org.openimaj.ml.clustering.assignment.Assigner;
import org.openimaj.ml.clustering.assignment.hard.ExactByteAssigner;
import org.openimaj.ml.clustering.assignment.hard.ExactIntAssigner;
import org.openimaj.ml.clustering.assignment.hard.KDTreeByteEuclideanAssigner;
import org.openimaj.ml.clustering.assignment.hard.KDTreeIntEuclideanAssigner;
import org.openimaj.ml.clustering.assignment.soft.ByteKNNAssigner;
import org.openimaj.ml.clustering.assignment.soft.IntKNNAssigner;
import org.openimaj.time.Timer;
import org.openimaj.tools.clusterquantiser.ClusterType;
import org.openimaj.tools.clusterquantiser.ClusterType.ClusterTypeOp;

public final class AssignerLoader implements Runnable {
	private File clusterLocation;
	private SpatialClusters<?> cluster;
	private Assigner<?> hardAssigner;
	private int softNeighbours;
	private boolean exactAassigner;
	private boolean useKryo = true;
	private String kryoExt = ".kryo";
	private List<AssignerLoadListener> listeners = new ArrayList<AssignerLoadListener>();

	/**
	 * @param bbcWorldSearcher
	 */
	public AssignerLoader(File clusterLocation, int softNeighbours, boolean exactAassigner) {
		this.clusterLocation = clusterLocation;
		this.softNeighbours = softNeighbours;
		this.exactAassigner = exactAassigner;
	}

	@Override
	public void run() {
		// Load the quantiser
		try {
			System.out.println("Loading Quantizer");
			Timer timer = Timer.timer();
			prepareQuantizer();
			System.out.println(String.format("Quantizer loaded took: %fs",timer.duration()/1000f));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		for (AssignerLoadListener l : this.listeners) {
			l.loadComplete(hardAssigner);
		}
	}

	public SpatialClusters<?> getClusters() throws IOException {
		if (cluster == null) {
			final ClusterTypeOp quantiserType = ClusterType.sniffClusterType(clusterLocation);
			cluster = IOUtils.read(clusterLocation, quantiserType.getClusterClass());
		}
		return cluster;
	}

	@SuppressWarnings("unchecked")
	public void prepareQuantizer() throws IOException {
		File kryoFile = new File(clusterLocation + kryoExt);
		if(this.useKryo && kryoFile.exists()){
			try{
				System.out.println("Reading assigner from file with kryo...");
				this.hardAssigner = IOUtils.readFromFile(kryoFile);
			}catch(Throwable e){
				System.out.println("... FAILED: ");
				e.printStackTrace();
			}
			if(this.hardAssigner!=null){
				return;
			}
		}
		final SpatialClusters<?> clusters = getClusters();
		if (softNeighbours > 0) {

			if (clusters.getClass().getName().contains("Byte")) {
				this.hardAssigner = new ByteKNNAssigner((CentroidsProvider<byte[]>) clusters, false, softNeighbours);
			} else {
				this.hardAssigner = new IntKNNAssigner((CentroidsProvider<int[]>) clusters, false, softNeighbours);
			}
		} else {
			if (hardAssigner == null) {
				if(!exactAassigner ){
					if (clusters.getClass().getName().contains("Byte")) {
						hardAssigner = new KDTreeByteEuclideanAssigner((CentroidsProvider<byte[]>) clusters);
					} else {
						hardAssigner = new KDTreeIntEuclideanAssigner((CentroidsProvider<int[]>) clusters);
					}
				}else{
					if (clusters.getClass().getName().contains("Byte")) {
						hardAssigner = new ExactByteAssigner((CentroidsProvider<byte[]>) clusters);
					} else {
						hardAssigner = new ExactIntAssigner((CentroidsProvider<int[]>) clusters);
					}
				}

			}
		}
		if(this.useKryo){
			try{
				System.out.println("Writing assigner to file using kryo...");
				IOUtils.writeToFile(this.hardAssigner,kryoFile);
			}catch(Throwable e){
				System.out.println("... FAILED!");
			}
		}
	}
	public static interface AssignerLoadListener{
		public void loadComplete(Assigner<?> assigner);
	}
	public void addLoadListener(AssignerLoadListener listener) {
		this.listeners.add(listener);
	}
}