/**
 * 
 */
package org.wltea.analyzer.lucene;

import org.apache.lucene.search.DefaultSimilarity;


public class IKSimilarity extends DefaultSimilarity {

	private static final long serialVersionUID = 7558565500061194774L;

	
	public float coord(int overlap, int maxOverlap) {
		float overlap2 = (float)Math.pow(2, overlap);
		float maxOverlap2 = (float)Math.pow(2, maxOverlap);
		return (overlap2 / maxOverlap2);
	}	
}
