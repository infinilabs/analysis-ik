package org.wltea.analyzer;

import java.util.HashSet;
import java.util.Set;

import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.seg.ISegmenter;

public class Context{

	private boolean isMaxWordLength = false;
	private int buffOffset;
	private int available;
    private int lastAnalyzed;
    private int cursor;
    private char[] segmentBuff;

    private Set<ISegmenter> buffLocker;

	private IKSortedLinkSet lexemeSet;


    Context(char[] segmentBuff , boolean isMaxWordLength){
    	this.isMaxWordLength = isMaxWordLength;
    	this.segmentBuff = segmentBuff;
    	this.buffLocker = new HashSet<ISegmenter>(4);
    	this.lexemeSet = new IKSortedLinkSet();
	}


    public void resetContext(){
    	buffLocker.clear();
    	lexemeSet = new IKSortedLinkSet();
    	buffOffset = 0;
    	available = 0;
    	lastAnalyzed = 0;
    	cursor = 0;
    }

	public boolean isMaxWordLength() {
		return isMaxWordLength;
	}

	public void setMaxWordLength(boolean isMaxWordLength) {
		this.isMaxWordLength = isMaxWordLength;
	}

	public int getBuffOffset() {
		return buffOffset;
	}


	public void setBuffOffset(int buffOffset) {
		this.buffOffset = buffOffset;
	}

	public int getLastAnalyzed() {
		return lastAnalyzed;
	}


	public void setLastAnalyzed(int lastAnalyzed) {
		this.lastAnalyzed = lastAnalyzed;
	}


	public int getCursor() {
		return cursor;
	}


	public void setCursor(int cursor) {
		this.cursor = cursor;
	}

	public void lockBuffer(ISegmenter segmenter){
		this.buffLocker.add(segmenter);
	}

	public void unlockBuffer(ISegmenter segmenter){
		this.buffLocker.remove(segmenter);
	}


	public boolean isBufferLocked(){
		return this.buffLocker.size() > 0;
	}

	public int getAvailable() {
		return available;
	}

	public void setAvailable(int available) {
		this.available = available;
	}




	public Lexeme firstLexeme() {
		return this.lexemeSet.pollFirst();
	}


	public Lexeme lastLexeme() {
		return this.lexemeSet.pollLast();
	}


	public void addLexeme(Lexeme lexeme){
		if(!Dictionary.isStopWord(segmentBuff , lexeme.getBegin() , lexeme.getLength())){
			this.lexemeSet.addLexeme(lexeme);
		}
	}


	public int getResultSize(){
		return this.lexemeSet.size();
	}


	public void excludeOverlap(){
		 this.lexemeSet.excludeOverlap();
	}


	private class IKSortedLinkSet{
		private Lexeme head;
		private Lexeme tail;
		private int size;

		private IKSortedLinkSet(){
			this.size = 0;
		}

		private void addLexeme(Lexeme lexeme){
			if(this.size == 0){
				this.head = lexeme;
				this.tail = lexeme;
				this.size++;
				return;

			}else{
				if(this.tail.compareTo(lexeme) == 0){
					return;

				}else if(this.tail.compareTo(lexeme) < 0){
					this.tail.setNext(lexeme);
					lexeme.setPrev(this.tail);
					this.tail = lexeme;
					this.size++;
					return;

				}else if(this.head.compareTo(lexeme) > 0){
					this.head.setPrev(lexeme);
					lexeme.setNext(this.head);
					this.head = lexeme;
					this.size++;
					return;

				}else{

					Lexeme l = this.tail;
					while(l != null && l.compareTo(lexeme) > 0){
						l = l.getPrev();
					}
					if(l.compareTo(lexeme) == 0){
						return;

					}else if(l.compareTo(lexeme) < 0){
						lexeme.setPrev(l);
						lexeme.setNext(l.getNext());
						l.getNext().setPrev(lexeme);
						l.setNext(lexeme);
						this.size++;
						return;

					}
				}
			}

		}

		private Lexeme pollFirst(){
			if(this.size == 1){
				Lexeme first = this.head;
				this.head = null;
				this.tail = null;
				this.size--;
				return first;
			}else if(this.size > 1){
				Lexeme first = this.head;
				this.head = first.getNext();
				first.setNext(null);
				this.size --;
				return first;
			}else{
				return null;
			}
		}


		private Lexeme pollLast(){
			if(this.size == 1){
				Lexeme last = this.head;
				this.head = null;
				this.tail = null;
				this.size--;
				return last;

			}else if(this.size > 1){
				Lexeme last = this.tail;
				this.tail = last.getPrev();
				last.setPrev(null);
				this.size--;
				return last;

			}else{
				return null;
			}
		}

	
		private void excludeOverlap(){
			if(this.size > 1){
				Lexeme one = this.head;
				Lexeme another = one.getNext();
				do{
					if(one.isOverlap(another)
							&& Lexeme.TYPE_CJK_NORMAL == one.getLexemeType()
							&& Lexeme.TYPE_CJK_NORMAL == another.getLexemeType()){

						another = another.getNext();

						one.setNext(another);
						if(another != null){
							another.setPrev(one);
						}
						this.size--;

					}else{
						one = another;
						another = another.getNext();
					}
				}while(another != null);
			}
		}

		private int size(){
			return this.size;
		}


	}

}
