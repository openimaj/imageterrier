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

import org.openimaj.feature.local.Location;
import org.openimaj.image.feature.local.affine.AffineSimulationKeypoint.AffineSimulationKeypointLocation;


public class PositionSpec {
	public static enum PositionSpecMode {
		NONE(0) {
			@Override
			public int[] encodePosition(Location l, int[] positionBits, double [] lowerBounds, double [] upperBounds) {
				return new int[0];
			}
		},
		SPATIAL(2) {
			@Override
			public int[] encodePosition(Location l, int[] positionBits, double [] lowerBounds, double [] upperBounds) {
				int [] pos = new int[npos];

				pos[0] = encode(l.getOrdinate(0).floatValue(), lowerBounds[0], upperBounds[0], positionBits[0]);
				pos[1] = encode(l.getOrdinate(1).floatValue(), lowerBounds[1], upperBounds[1], positionBits[1]);

				return pos;
			}
		},
		SCALE(1) {
			@Override
			public int[] encodePosition(Location l, int[] positionBits, double [] lowerBounds, double [] upperBounds) {
				int [] pos = new int[npos];

				pos[0] = encode(l.getOrdinate(2).floatValue(), lowerBounds[0], upperBounds[0], positionBits[0]);

				return pos;
			}
		},
		ORI(1) {
			@Override
			public int[] encodePosition(Location l, int[] positionBits, double [] lowerBounds, double [] upperBounds) {
				int [] pos = new int[npos];

				pos[0] = encode(l.getOrdinate(3).floatValue(), lowerBounds[0], upperBounds[0], positionBits[0]);

				return pos;
			}
		},
		SPATIAL_SCALE(3) {
			@Override
			public int[] encodePosition(Location l, int[] positionBits, double [] lowerBounds, double [] upperBounds) {
				int [] pos = new int[npos];

				pos[0] = encode(l.getOrdinate(0).floatValue(), lowerBounds[0], upperBounds[0], positionBits[0]);
				pos[1] = encode(l.getOrdinate(1).floatValue(), lowerBounds[1], upperBounds[1], positionBits[1]);
				pos[2] = encode(l.getOrdinate(2).floatValue(), lowerBounds[2], upperBounds[2], positionBits[2]);

				return pos;
			}
		},
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
		},
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
		},
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
		}
		;

		public int npos;

		PositionSpecMode(int npos) {
			this.npos = npos;
		}

		public abstract int [] encodePosition(Location l, int [] positionBits, double [] lowerBounds, double [] upperBounds);

		/**
		 * Encode value as an nbits UNSIGNED integer
		 * 
		 * @param value
		 * @param lower
		 * @param upper
		 * @param nbits
		 * @return
		 */
		protected int encode(double value, double lower, double upper, int nbits) {
			if (value < lower) return 0;

			int maxval = (int) (Math.pow(2,nbits) - 1);
			if (value >= upper) return maxval;

			return (int) ((value - lower)*((maxval - 0) / (upper - lower)));
		}
	}

	protected PositionSpecMode mode;
	protected int [] positionBits;
	protected double [] lowerBounds;
	protected double [] upperBounds;

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
	
	public int [] getPositionBits() {
		return positionBits;
	}

	public void setPositionBits(int [] positionBits) {
		this.positionBits = positionBits;
	}

	public int [] getPosition(QLFDocument<?> document) {
		return mode.encodePosition(document.getLocation(), positionBits, lowerBounds, upperBounds);
	}

	public PositionSpecMode getMode() {
		return mode;
	}

	public double[] getLowerBounds() {
		return lowerBounds;
	}
	
	public double[] getUpperBounds() {
		return upperBounds;
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
	
	@Override
	public String toString() {
		return mode.name()+"(" + numsToString(positionBits) + ";" + numsToString(lowerBounds) + ";" + numsToString(upperBounds)+")";
	}
	
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
	
	public static void main(String [] args) {
		PositionSpec spec = new PositionSpec(PositionSpecMode.SPATIAL, new int[] {2,3}, new double[] {0,1}, new double[]{10,11});

		System.out.println(spec);
		
		System.out.println(PositionSpec.decode(spec.toString()));
	}
}
