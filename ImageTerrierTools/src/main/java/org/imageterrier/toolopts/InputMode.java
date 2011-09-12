package org.imageterrier.toolopts;

import java.io.File;
import java.io.IOException;

import org.kohsuke.args4j.CmdLineOptionsProvider;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ProxyOptionHandler;
import org.openimaj.tools.clusterquantiser.ClusterType;
import org.openimaj.tools.localfeature.LocalFeatureMode;


/**
 * Define the input for the indexer
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 */
public enum InputMode implements CmdLineOptionsProvider {
	IMAGES {
		@Override
		public InputModeOptions getOptions() {
			return new ImagesModeOptions();
		}
	},
	QUANTISED_FEATURES {
		@Override
		public InputModeOptions getOptions() {
			return new QFModeOptions();
		}
	}
	;
	
	public abstract static class InputModeOptions {
		@Option(name = "--quant-file", aliases = "-q", usage = "path to quantiser file", required = false, metaVar = "path")
		private String quantiserFile;
		
		@Option(name = "--quant-type", aliases = "-qt", usage = "Quantiser type. Defaults to AKM (FastKMeans)", required = false, metaVar = "type", handler=ProxyOptionHandler.class)
		public ClusterType quantiserType = ClusterType.FASTKMEANS;
		
		@Option(name = "--quant-exact", aliases = "-qe", usage = "Load the quantiser in exact mode or not", required = false, metaVar = "type", handler=ProxyOptionHandler.class)
		public boolean quantiserExact = false;
		
		@Option(name = "--feature-type", aliases = "-ft", usage = "Feature type. Defaults to plain DoG/SIFT", required = false, metaVar = "type", handler=ProxyOptionHandler.class)
		public
		LocalFeatureMode featureType = LocalFeatureMode.SIFT;
		
		public String getQuantiserFile() throws IOException {
//			if (quantiserFile == null) {
//				 quantiserFile = File.createTempFile("imageterrier", ".cluster");
//				 quantiserFile.delete();
//			}
			
			return quantiserFile;
		}

		public ClusterType getQuantiserType() {
			return quantiserType;
		}

		public LocalFeatureMode getFeatureType() {
			return featureType;
		}

	}
	
	public static class QFModeOptions extends InputModeOptions {
		@Option(name = "--file-extension", aliases = "-fe", usage = "file extension for quantised feature files", required = false, metaVar = "extension")
		private String fileExtension;
		
		public String getFileExtension() {
			return fileExtension;
		}
	}
	
	public static class ImagesModeOptions extends InputModeOptions {
		@Option(name = "--force-regeneration", aliases = "-f", usage = "force visterm regeneration")
		private boolean forceRegeneration = false;
		
		public boolean forceRegeneration() {
			return forceRegeneration;
		}			
	}	
}
