package org.terrier.utility;

/**
 * Extensions to the ArrayUtils class. Methods here
 * should eventually find there way back into ArrayUtils.
 *  
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 */
public class ExtendedArrayUtils extends ArrayUtils {
	/**
	 * parse comma delimited double
	 * @param src
	 * @return double[]
	 */
	public static double[] parseCommaDelimitedDoubles(String src)
	{
		final String[] parts = ArrayUtils.parseCommaDelimitedString(src);
		if (parts.length == 0)
			return new double[0];
		double[] rtr = new double[parts.length];
		for(int i=0;i<parts.length;i++)
			rtr[i] = Double.parseDouble(parts[i]);
		return rtr;
	}

}
