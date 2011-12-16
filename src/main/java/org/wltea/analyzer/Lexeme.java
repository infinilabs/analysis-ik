/**
 * 
 */
package org.wltea.analyzer;

public final class Lexeme implements Comparable<Lexeme>{
	public static final int TYPE_CJK_NORMAL = 0;
	public static final int TYPE_CJK_SN = 1;
	public static final int TYPE_CJK_SF = 2;
	public static final int TYPE_CJK_UNKNOWN = 3;
	public static final int TYPE_NUM = 10;
	public static final int TYPE_NUMCOUNT = 11;
	public static final int TYPE_LETTER = 20;
	
	private int offset;
    private int begin;
    private int length;
    private String lexemeText;
    private int lexemeType;
    
    private Lexeme prev;
    private Lexeme next;
    
	public Lexeme(int offset , int begin , int length , int lexemeType){
		this.offset = offset;
		this.begin = begin;
		if(length < 0){
			throw new IllegalArgumentException("length < 0");
		}
		this.length = length;
		this.lexemeType = lexemeType;
	}
	

	public boolean equals(Object o){
		if(o == null){
			return false;
		}
		
		if(this == o){
			return true;
		}
		
		if(o instanceof Lexeme){
			Lexeme other = (Lexeme)o;
			if(this.offset == other.getOffset()
					&& this.begin == other.getBegin()
					&& this.length == other.getLength()){
				return true;			
			}else{
				return false;
			}
		}else{		
			return false;
		}
	}

    public int hashCode(){
    	int absBegin = getBeginPosition();
    	int absEnd = getEndPosition();
    	return  (absBegin * 37) + (absEnd * 31) + ((absBegin * absEnd) % getLength()) * 11;
    }
    

	public int compareTo(Lexeme other) {

        if(this.begin < other.getBegin()){
            return -1;
        }else if(this.begin == other.getBegin()){

        	if(this.length > other.getLength()){
        		return -1;
        	}else if(this.length == other.getLength()){
        		return 0;
        	}else {
        		return 1;
        	}
        	
        }else{
        	return 1;
        }
	}
	

	public boolean isOverlap(Lexeme other){
		if(other != null){
			if(this.getBeginPosition() <= other.getBeginPosition() 
					&& this.getEndPosition() >= other.getEndPosition()){
				return true;
				
			}else if(this.getBeginPosition() >= other.getBeginPosition() 
					&& this.getEndPosition() <= other.getEndPosition()){
				return true;
				
			}else {
				return false;
			}
		}
		return false;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public int getBegin() {
		return begin;
	}

	public int getBeginPosition(){
		return offset + begin;
	}

	public void setBegin(int begin) {
		this.begin = begin;
	}


	public int getEndPosition(){
		return offset + begin + length;
	}
	

	public int getLength(){
		return this.length;
	}	
	
	public void setLength(int length) {
		if(this.length < 0){
			throw new IllegalArgumentException("length < 0");
		}
		this.length = length;
	}
	

	public String getLexemeText() {
		if(lexemeText == null){
			return "";
		}
		return lexemeText;
	}

	public void setLexemeText(String lexemeText) {
		if(lexemeText == null){
			this.lexemeText = "";
			this.length = 0;
		}else{
			this.lexemeText = lexemeText;
			this.length = lexemeText.length();
		}
	}

	
	public int getLexemeType() {
		return lexemeType;
	}

	public void setLexemeType(int lexemeType) {
		this.lexemeType = lexemeType;
	}	
	
	public String toString(){
		StringBuffer strbuf = new StringBuffer();
		strbuf.append(this.getBeginPosition()).append("-").append(this.getEndPosition());
		strbuf.append(" : ").append(this.lexemeText).append(" : \t");
		switch(lexemeType) {
			case TYPE_CJK_NORMAL : 
				strbuf.append("CJK_NORMAL");
				break;
			case TYPE_CJK_SF :
				strbuf.append("CJK_SUFFIX");
				break;
			case TYPE_CJK_SN :
				strbuf.append("CJK_NAME");
				break;
			case TYPE_CJK_UNKNOWN :
				strbuf.append("UNKNOWN");
				break;
			case TYPE_NUM : 
				strbuf.append("NUMEBER");
				break;
			case TYPE_NUMCOUNT :
				strbuf.append("COUNT");
				break;
			case TYPE_LETTER :
				strbuf.append("LETTER");
				break;

		}
		return strbuf.toString();
	}

	Lexeme getPrev() {
		return prev;
	}

	void setPrev(Lexeme prev) {
		this.prev = prev;
	}

	Lexeme getNext() {
		return next;
	}

	void setNext(Lexeme next) {
		this.next = next;
	}

	
}