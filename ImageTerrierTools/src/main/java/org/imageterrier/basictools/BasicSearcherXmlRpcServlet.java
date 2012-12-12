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
 * The Original Code is BasicSearcherXmlRpcServlet.java
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

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Basic search methods to be exposed as XmlRpc methods
 */
public class BasicSearcherXmlRpcServlet {
	public static BasicSearcher<BasicSearcherOptions > searcher;
	public static BasicSearcherOptions options;

	public List<Map<String, Object>> search(String localImageFile, boolean useNeighbours) throws Exception {
		return search(localImageFile, options.getLimit());
	}

	public List<Map<String, Object>> search(String localImageFile, int limit) throws Exception {
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		List<DocidScore> rs = searcher.search(new File(localImageFile), null, options);

		if (limit<=0) limit = rs.size();

		for (DocidScore docidscore : rs) {
			File file = searcher.getFile(searcher.index,docidscore.first);

			if (docidscore.second <= 0) break; //filter 0 results

			Map<String, Object> entry = new HashMap<String, Object>();
			entry.put("name", file.toString());
			entry.put("score", docidscore.second);
			results.add(entry);
		}

		return results;
	}

	public List<Map<String, Object>> search(byte[] imageBytes) throws Exception {
		return search(imageBytes, options.getLimit());
	}

	public List<Map<String, Object>> search(byte[] imageBytes, int limit) throws Exception {
		System.out.format("Search: %d byte image, limit:%d\n", imageBytes.length, limit);
		File tmp = File.createTempFile("imageterrier-xmlrpc-image", ".jpg");
		FileOutputStream fos = new FileOutputStream(tmp);
		fos.write(imageBytes);
		fos.close();
		List<Map<String, Object>> res = search(tmp.toString(), limit);
		System.out.println(tmp);
		//tmp.delete();
		System.out.println("Result: " + res);
		return res;
	}
}
