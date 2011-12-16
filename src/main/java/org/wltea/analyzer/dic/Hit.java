/**
 * 
 */
package org.wltea.analyzer.dic;

public class Hit {

	private static final int UNMATCH = 0x00000000;

	private static final int MATCH = 0x00000001;

	private static final int PREFIX = 0x00000010;
	
	

	private int hitState = UNMATCH;
	

	private DictSegment matchedDictSegment; 

	private int begin;

	private int end;
	
	

	public boolean isMatch() {
		return (this.hitState & MATCH) > 0;
	}

	public void setMatch() {
		this.hitState = this.hitState | MATCH;
	}

	public boolean isPrefix() {
		return (this.hitState & PREFIX) > 0;
	}

	public void setPrefix() {
		this.hitState = this.hitState | PREFIX;
	}

	public boolean isUnmatch() {
		return this.hitState == UNMATCH ;
	}

	public void setUnmatch() {
		this.hitState = UNMATCH;
	}
	
	public DictSegment getMatchedDictSegment() {
		return matchedDictSegment;
	}
	
	public void setMatchedDictSegment(DictSegment matchedDictSegment) {
		this.matchedDictSegment = matchedDictSegment;
	}
	
	public int getBegin() {
		return begin;
	}
	
	public void setBegin(int begin) {
		this.begin = begin;
	}
	
	public int getEnd() {
		return end;
	}
	
	public void setEnd(int end) {
		this.end = end;
	}	
	
}
