/**
 * 
 */
package org.wltea.analyzer.seg;

import org.wltea.analyzer.Context;


public interface ISegmenter {
	

	void nextLexeme(char[] segmentBuff , Context context);
	
	
	void reset();
}
