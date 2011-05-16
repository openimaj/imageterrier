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
 * The Original Code is ApplicationSetupUtils.java
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
package org.imageterrier.basictools;

import org.terrier.utility.ApplicationSetup;

/**
 * Utility functions for getting and setting things
 * in {@link ApplicationSetup}.
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 */
public class ApplicationSetupUtils {
	/** 
	 * Returns the value for the specified property, given 
	 * a default value, in case the property was not defined
	 * during the initialization of the system.
	 * 
	 * The property values are read from the properties file. If the value 
	 * of the property <tt>terrier.usecontext</tt> is true, then the properties
	 * file is overridden by the context. If the value of the property 
	 * <tt>terrier.usecontext</tt> is false, then the properties file is overridden 
	 * @param key The property to be returned
	 * @param defaultValue The default value used, in case it is not defined
	 * @return the value for the given property.
	 */
	public static int getProperty(String key, int defaultValue) {
		String vs = ApplicationSetup.getProperty(key, defaultValue+"");
		
		if (vs.trim().length() > 0) {
			return Integer.parseInt(vs);
		}
		return defaultValue;
	}
	
	/** 
	 * Returns the value for the specified property, given 
	 * a default value, in case the property was not defined
	 * during the initialization of the system.
	 * 
	 * The property values are read from the properties file. If the value 
	 * of the property <tt>terrier.usecontext</tt> is true, then the properties
	 * file is overridden by the context. If the value of the property 
	 * <tt>terrier.usecontext</tt> is false, then the properties file is overridden 
	 * @param key The property to be returned
	 * @param defaultValue The default value used, in case it is not defined
	 * @return the value for the given property.
	 */
	public static float getProperty(String key, float defaultValue) {
		String vs = ApplicationSetup.getProperty(key, defaultValue+"");
		
		if (vs.trim().length() > 0) {
			return Float.parseFloat(vs);
		}
		return defaultValue;
	}
	
	/** 
	 * Returns the value for the specified property, given 
	 * a default value, in case the property was not defined
	 * during the initialization of the system.
	 * 
	 * The property values are read from the properties file. If the value 
	 * of the property <tt>terrier.usecontext</tt> is true, then the properties
	 * file is overridden by the context. If the value of the property 
	 * <tt>terrier.usecontext</tt> is false, then the properties file is overridden 
	 * @param key The property to be returned
	 * @param defaultValue The default value used, in case it is not defined
	 * @return the value for the given property.
	 */
	public static double getProperty(String key, double defaultValue) {
		String vs = ApplicationSetup.getProperty(key, defaultValue+"");
		
		if (vs.trim().length() > 0) {
			return Double.parseDouble(vs);
		}
		return defaultValue;
	}
}
