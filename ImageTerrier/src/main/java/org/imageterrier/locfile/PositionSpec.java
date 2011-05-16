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
 * The Original Code is PositionSpec.java
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
package org.imageterrier.locfile;

import java.util.Arrays;

import org.openimaj.feature.local.Location;
import org.openimaj.feature.local.ScaleSpaceLocation;
import org.openimaj.feature.local.SpatialLocation;
import org.openimaj.image.feature.local.affine.AffineSimulationKeypoint.AffineSimulationKeypointLocation;
import org.openimaj.image.feature.local.keypoints.KeypointLocation;
import org.openimaj.util.hash.HashCodeUtil;


/**
 * The PositionSpec class defines the type of position that will be stored in the
 * payload of each term-document posting, together with the methods required for
 * encoding and decoding this information.
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 */
public class PositionSpec {
	/**
	 * An enum containing all the supported location payload formats
	 * and methods to encode Location objects as integer arrays, and get 
	 * them back again.
	 * 
	 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
	 */
	public static enum PositionSpecMode {
		/**
		 * No location information should be stored
		 */
		NONE(0) {
			@Override
			public int[] encodePosition(Location l, int[] positionBits, double [] lowerBounds, double [] upperBounds) {
				return new int[0];
			}

			@Override
			public Location decodePosition(int[] data, int[] positionBits, double[] lowerBounds, double[] upperBounds) {
				return null;
			}
		},
		/**
		 * Store the x and y coordinates of each visual term
		 */
		SPATIAL(2) {
			@Override
			public int[] encodePosition(Location l, int[] positionBits, double [] lowerBounds, double [] upperBounds) {
				int [] pos = new int[npos];

				pos[0] = encode(l.getOrdinate(0).floatValue(), lowerBounds[0], upperBounds[0], positionBits[0]);
				pos[1] = encode(l.getOrdinate(1).floatValue(), lowerBounds[1], upperBounds[1], positionBits[1]);

				return pos;
			}

			@Override
			public Location decodePosition(int[] data, int[] positionBits, double[] lowerBounds, double[] upperBounds) {
				return new SpatialLocation(
						(float)decode(data[0], lowerBounds[0], upperBounds[0], positionBits[0]),
						(float)decode(data[1], lowerBounds[1], upperBounds[1], positionBits[1])
						);
			}
		},
		/**
		 * Store the scale of each visual term
		 */
		SCALE(1) {
			@Override
			public int[] encodePosition(Location l, int[] positionBits, double [] lowerBounds, double [] upperBounds) {
				int [] pos = new int[npos];

				pos[0] = encode(l.getOrdinate(2).floatValue(), lowerBounds[0], upperBounds[0], positionBits[0]);

				return pos;
			}
			
			@Override
			public Location decodePosition(int[] data, int[] positionBits, double[] lowerBounds, double[] upperBounds) {
				return new ScaleSpaceLocation( 
						0,
						0,
						(float)decode(data[0], lowerBounds[0], upperBounds[0], positionBits[0])
						);
			}
		},
		/**
		 * Store the orientation of each visual term
		 */
		ORI(1) {
			@Override
			public int[] encodePosition(Location l, int[] positionBits, double [] lowerBounds, double [] upperBounds) {
				int [] pos = new int[npos];

				pos[0] = encode(l.getOrdinate(3).floatValue(), lowerBounds[0], upperBounds[0], positionBits[0]);

				return pos;
			}
			
			@Override
			public Location decodePosition(int[] data, int[] positionBits, double[] lowerBounds, double[] upperBounds) {
				return new KeypointLocation(
						0,
						0,
						0,
						(float)decode(data[0], lowerBounds[0], upperBounds[0], positionBits[0])
						);
			}
		},
		/**
		 * Store the x and y coordinates of each visual term together with its respective scale
		 */
		SPATIAL_SCALE(3) {
			@Override
			public int[] encodePosition(Location l, int[] positionBits, double [] lowerBounds, double [] upperBounds) {
				int [] pos = new int[npos];

				pos[0] = encode(l.getOrdinate(0).floatValue(), lowerBounds[0], upperBounds[0], positionBits[0]);
				pos[1] = encode(l.getOrdinate(1).floatValue(), lowerBounds[1], upperBounds[1], positionBits[1]);
				pos[2] = encode(l.getOrdinate(2).floatValue(), lowerBounds[2], upperBounds[2], positionBits[2]);

				return pos;
			}
			
			@Override
			public Location decodePosition(int[] data, int[] positionBits, double[] lowerBounds, double[] upperBounds) {
				return new ScaleSpaceLocation( 
						(float)decode(data[0], lowerBounds[0], upperBounds[0], positionBits[0]),
						(float)decode(data[1], lowerBounds[1], upperBounds[1], positionBits[1]),
						(float)decode(data[2], lowerBounds[2], upperBounds[2], positionBits[2])
						);
			}
		},
		/**
		 * Store the x and y coordinates of each visual term together with its respective scale and orientation
		 */
		SPATIAL_SCALE_ORI(4) {
			@Override
			public int[] encodePosition(Location l, int[] positionBits, double [] lowerBounds, double [] upperBounds) {
				int [] pos = new int[npos];

				pos[0] = encode(l.getOrdinate(0).floatValue(), lowerBounds[0], upperBounds[0], positionBits[0]);
				pos[1] = encode(l.getOrdinate(1).floatValue(), lowerBounds[1], upperBounds[1], positionBits[1]);
				pos[2] = encode(l.getOrdinate(2).floatValue(), lowerBounds[2], upperBounds[2], positionBits[2]);
				pos[3] = encode(l.getOrdinate(3).floatValue(), lowerBounds[3], upperBounds[3], positionBits[3]);

				return pos;
			}
			
			@Override
			public Location decodePosition(int[] data, int[] positionBits, double[] lowerBounds, double[] upperBounds) {
				return new KeypointLocation(
						(float)decode(data[0], lowerBounds[0], upperBounds[0], positionBits[0]),
						(float)decode(data[1], lowerBounds[1], upperBounds[1], positionBits[1]),
						(float)decode(data[2], lowerBounds[2], upperBounds[2], positionBits[2]),
						(float)decode(data[3], lowerBounds[3], upperBounds[3], positionBits[3])
						);
			}
		},
		/**
		 * Store the theta and tilt parameters from the affine simulation that created the visual term
		 */
		AFFINE(2) {
			@Override
			public int[] encodePosition(Location l, int[] positionBits, double [] lowerBounds, double [] upperBounds) {
				int [] pos = new int[npos];

				if (l instanceof AffineSimulationKeypointLocation) {
					pos[0] = encode(l.getOrdinate(4).floatValue(), lowerBounds[0], upperBounds[0], positionBits[0]);
					pos[1] = encode(l.getOrdinate(5).floatValue(), lowerBounds[1], upperBounds[1], positionBits[1]);
				}
				
				return pos;
			}
			
			@Override
			public Location decodePosition(int[] data, int[] positionBits, double[] lowerBounds, double[] upperBounds) {
				return new AffineSimulationKeypointLocation(
						0,
						0,
						0,
						0,
						(float)decode(data[0], lowerBounds[0], upperBounds[0], positionBits[0]),
						(float)decode(data[1], lowerBounds[1], upperBounds[1], positionBits[1]),
						0
						);
			}
		},
		/**
		 * Store the x and y coordinates together with the theta and tilt parameters 
		 * from the affine simulation that created the visual term
		 */
		SPATIAL_AFFINE(4) {
			@Override
			public int[] encodePosition(Location l, int[] positionBits, double [] lowerBounds, double [] upperBounds) {
				int [] pos = new int[npos];
				
				pos[0] = encode(l.getOrdinate(0).floatValue(), lowerBounds[0], upperBounds[0], positionBits[0]);
				pos[1] = encode(l.getOrdinate(1).floatValue(), lowerBounds[1], upperBounds[1], positionBits[1]);
				
				if (l instanceof AffineSimulationKeypointLocation) {
					pos[2] = encode(l.getOrdinate(4).floatValue(), lowerBounds[2], upperBounds[2], positionBits[2]);
					pos[3] = encode(l.getOrdinate(5).floatValue(), lowerBounds[3], upperBounds[3], positionBits[3]);
				}

				return pos;
			}
			
			@Override
			public Location decodePosition(int[] data, int[] positionBits, double[] lowerBounds, double[] upperBounds) {
				return new AffineSimulationKeypointLocation(
						(float)decode(data[0], lowerBounds[0], upperBounds[0], positionBits[0]), //x
						(float)decode(data[1], lowerBounds[1], upperBounds[1], positionBits[1]), //y
						0, //sca
						0, //ori
						(float)decode(data[2], lowerBounds[2], upperBounds[2], positionBits[2]), //theta
						(float)decode(data[3], lowerBounds[3], upperBounds[3], positionBits[3]), //tilt
						0 //index
						);
			}
		}, 
		/**
		 * Store the index from the affine simulation that created the visual term.
		 * Note that the value isn't encoded (as it's already an unsigned integer);
		 * the number of bits must however be big enough to store the value in the
		 * index however.
		 */
		AFFINE_INDEX(1) {
			@Override
			public int[] encodePosition(Location l, int[] positionBits, double [] lowerBounds, double [] upperBounds) {
				int [] pos = new int[npos];
				
				if (l instanceof AffineSimulationKeypointLocation) {
					pos[0] = l.getOrdinate(6).intValue();
				}

				return pos;
			}
			
			@Override
			public Location decodePosition(int[] data, int[] positionBits, double[] lowerBounds, double[] upperBounds) {
				return new AffineSimulationKeypointLocation(
						0, //x
						0, //y
						0, //sca
						0, //ori
						0, //theta
						0, //tilt
						data[0]//index
						);
			}
		}
		;

		/** The number of ordinates stored by the PositionSpecMode */
		public final int npos;

		PositionSpecMode(int npos) {
			this.npos = npos;
		}

		/**
		 * Encode the Location object as an array of unsigned n-bit integers
		 * @param l the location to encode
		 * @param positionBits the number of bits for each ordinate
		 * @param lowerBounds the minimum value per ordinate
		 * @param upperBounds the maximum value per ordinate
		 * @return an array of integers encoding the ordinates of the Location
		 */
		public abstract int [] encodePosition(Location l, int [] positionBits, double [] lowerBounds, double [] upperBounds);
		
		/**
		 * Decode the ordinates stored in the data array into a Location object.
		 * @param data the data to decode
		 * @param positionBits the number of bits for each ordinate
		 * @param lowerBounds the minimum value per ordinate
		 * @param upperBounds the maximum value per ordinate
		 * @return a location object with the ordinates from the data
		 */
		public abstract Location decodePosition(int[] data, int [] positionBits, double [] lowerBounds, double [] upperBounds);

		/**
		 * Encode value as an nbits UNSIGNED integer
		 * 
		 * @param value value to encode
		 * @param lower minimum allowed value
		 * @param upper maximum allowed value
		 * @param nbits number of bits to use
		 * @return an integer encoding the given value
		 */
		protected int encode(double value, double lower, double upper, int nbits) {
			if (value < lower) return 0;

			int maxval = (int) (Math.pow(2,nbits) - 1);
			if (value >= upper) return maxval;

			return (int) ((value - lower)*((maxval - 0) / (upper - lower)));
		}
		
		/**
		 * Decode value from an nbits UNSIGNED integer
		 * 
		 * @param value value to decode
		 * @param lower minimum allowed value
		 * @param upper maximum allowed value
		 * @param nbits number of bits to use
		 * @return decoded value
		 */
		protected double decode(int value, double lower, double upper, int nbits) {
			int maxval = (int) (Math.pow(2,nbits) - 1);
			
			return value*((upper - lower) / (maxval - 0)) + lower;
		}
	}

	protected PositionSpecMode mode;
	protected int [] positionBits;
	protected double [] lowerBounds;
	protected double [] upperBounds;

	/**
	 * Construct a PositionSpec with the given parameters.
	 * @param mode the mode
	 * @param positionBits the number of bits to use for encoding each ordinate
	 * @param lowerBounds the minimum allowed value for each ordinate 
	 * @param upperBounds the maximum allowed value for each ordinate
	 */
	public PositionSpec(PositionSpecMode mode, int [] positionBits, double [] lowerBounds, double [] upperBounds) {
		this.mode = mode;
		this.positionBits = positionBits == null ? new int[0] : positionBits;
		this.lowerBounds = lowerBounds;
		this.upperBounds = upperBounds;

		if (positionBits.length != mode.npos)
			throw new RuntimeException("invalid number of positionBits");
		if (lowerBounds != null && lowerBounds.length != mode.npos)
			throw new RuntimeException("invalid number of lowerBounds");
		if (upperBounds != null && upperBounds.length != mode.npos)
			throw new RuntimeException("invalid number of upperBounds");
	}
	
	/**
	 * Get the number of bits for each ordinate
	 * @return the number of bits for each ordinate
	 */
	public int [] getPositionBits() {
		return positionBits;
	}

	/**
	 * Set the number of bits for each ordinate
	 * @param positionBits the bits per ordinate
	 */
	public void setPositionBits(int [] positionBits) {
		this.positionBits = positionBits;
	}

	/**
	 * Get the encoded position of the current term
	 * in the document. 
	 * @param document the document
	 * @return the encoded position
	 */
	public int [] getPosition(QLFDocument<?> document) {
		return mode.encodePosition(document.getLocation(), positionBits, lowerBounds, upperBounds);
	}

	/**
	 * Get the position mode.
	 * @return the mode
	 */
	public PositionSpecMode getMode() {
		return mode;
	}

	/**
	 * Get the minimum allowed value for each ordinate
	 * @return the minimum allowed value for each ordinate
	 */
	public double[] getLowerBounds() {
		return lowerBounds;
	}
	
	/**
	 * Get the maximum allowed value for each ordinate
	 * @return the maximum allowed value for each ordinate
	 */
	public double[] getUpperBounds() {
		return upperBounds;
	}
	
	/**
	 * Decode the encoded position data into a {@link Location} object.
	 * @param encodedData the encoded data.
	 * @return a new Location object constructed from the encoded data.
	 */
	public Location decode(int [] encodedData) {
		return mode.decodePosition(encodedData, positionBits, lowerBounds, upperBounds);
	}

	private String numsToString(int [] nos) {
		if (nos.length == 0) return "";
		String s = "" + nos[0];
		for (int i=1; i<nos.length; i++) s+=","+nos[i];
		return s;
	}
	private String numsToString(double [] nos) {
		if (nos.length == 0) return "";
		String s = "" + nos[0];
		for (int i=1; i<nos.length; i++) s+=","+nos[i];
		return s;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return mode.name()+"(" + numsToString(positionBits) + ";" + numsToString(lowerBounds) + ";" + numsToString(upperBounds)+")";
	}
	
	/**
	 * Decode a PositionSpec from a String produced by {@link #toString()}.
	 * @param string String containing the data required to create a PositionSpec.
	 * @return a new PositionSpec.
	 */
	public static PositionSpec decode(String string) {
		PositionSpecMode mode = PositionSpecMode.valueOf(string.substring(0, string.indexOf("(")));
		
		String dataStr = string.substring(string.indexOf("(")+1, string.length()-1);
		String [] data = dataStr.split(";");
	
		int [] positionBits = new int[mode.npos];
		double [] lowerBounds = new double[mode.npos];
		double [] upperBounds = new double[mode.npos];
		
		int i=0; for (String s : data[0].split(",")) positionBits[i++] = Integer.parseInt(s.trim());
		i=0; for (String s : data[1].split(",")) lowerBounds[i++] = Double.parseDouble(s.trim());
		i=0; for (String s : data[2].split(",")) upperBounds[i++] = Double.parseDouble(s.trim());
		
		return new PositionSpec(mode, positionBits, lowerBounds, upperBounds);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int result = HashCodeUtil.SEED;
		
		result = HashCodeUtil.hash(result, mode);
		result = HashCodeUtil.hash(result, positionBits);
		result = HashCodeUtil.hash(result, lowerBounds);
		result = HashCodeUtil.hash(result, upperBounds);
		
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) return true;
	    if ( !(obj instanceof PositionSpec) ) return false; 
		
	    PositionSpec spec = (PositionSpec) obj;
	    
		return mode.equals(spec.mode) &&
			Arrays.equals(positionBits, spec.positionBits) &&
			Arrays.equals(lowerBounds, spec.lowerBounds) &&
			Arrays.equals(upperBounds, spec.upperBounds);
	}
}
