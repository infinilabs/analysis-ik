/**
 * 
 */
package org.wltea.analyzer.seg;

import java.util.ArrayList;
import java.util.List;

import org.wltea.analyzer.Context;
import org.wltea.analyzer.Lexeme;
import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.dic.Hit;
import org.wltea.analyzer.help.CharacterHelper;


public class CJKSegmenter implements ISegmenter {

	private int doneIndex;

	private List<Hit> hitList;
	
	public CJKSegmenter(){
		doneIndex = -1;
		hitList = new ArrayList<Hit>();
	}

	public void nextLexeme(char[] segmentBuff , Context context) {

		
		char input = segmentBuff[context.getCursor()];
		
		if(CharacterHelper.isCJKCharacter(input)){
			if(hitList.size() > 0){
				
				Hit[] tmpArray = hitList.toArray(new Hit[hitList.size()]);
				for(Hit hit : tmpArray){
					hit = Dictionary.matchInMainDictWithHit(segmentBuff, context.getCursor() , hit);
					
					if(hit.isMatch()){
						
						if(hit.getBegin() > doneIndex + 1){
							
							processUnknown(segmentBuff , context , doneIndex + 1 , hit.getBegin()- 1);
						}
						
						Lexeme newLexeme = new Lexeme(context.getBuffOffset() , hit.getBegin() , context.getCursor() - hit.getBegin() + 1 , Lexeme.TYPE_CJK_NORMAL);
						context.addLexeme(newLexeme);
						
						if(doneIndex < context.getCursor()){
							doneIndex = context.getCursor();
						}
						
						if(hit.isPrefix()){
							
						}else{ 
							
							hitList.remove(hit);
						}
						
					}else if(hit.isPrefix()){
						
					}else if(hit.isUnmatch()){
						
						hitList.remove(hit);
					}
				}
			}
			
			
			Hit hit = Dictionary.matchInMainDict(segmentBuff, context.getCursor() , 1);
			if(hit.isMatch()){
				
				if(context.getCursor() > doneIndex + 1){
					
					processUnknown(segmentBuff , context , doneIndex + 1 , context.getCursor()- 1);
				}
				
				Lexeme newLexeme = new Lexeme(context.getBuffOffset() , context.getCursor() , 1 , Lexeme.TYPE_CJK_NORMAL);
				context.addLexeme(newLexeme);
				
				if(doneIndex < context.getCursor()){
					doneIndex = context.getCursor();
				}

				if(hit.isPrefix()){
					
					hitList.add(hit);
				}
				
			}else if(hit.isPrefix()){
				
				hitList.add(hit);
				
			}else if(hit.isUnmatch()){
				if(doneIndex >= context.getCursor()){
					
					return;
				}
				
				
				processUnknown(segmentBuff , context , doneIndex + 1 , context.getCursor());
				
				doneIndex = context.getCursor();
			}
			
		}else {
			if(hitList.size() > 0
					&&  doneIndex < context.getCursor() - 1){
				for(Hit hit : hitList){
					
					if(doneIndex < hit.getEnd()){
						
						processUnknown(segmentBuff , context , doneIndex + 1 , hit.getEnd());
					}
				}
			}
			
			hitList.clear();
			
			if(doneIndex < context.getCursor()){
				doneIndex = context.getCursor();
			}
		}
		
		
		if(context.getCursor() == context.getAvailable() - 1){ 
			if( hitList.size() > 0 
				&& doneIndex < context.getCursor()){
				for(Hit hit : hitList){
					
					if(doneIndex < hit.getEnd() ){
						
						processUnknown(segmentBuff , context , doneIndex + 1 , hit.getEnd());
					}
				}
			}
			
			hitList.clear();;
		}
		
		
		if(hitList.size() == 0){
			context.unlockBuffer(this);
			
		}else{
			context.lockBuffer(this);
	
		}
	}

	private void processUnknown(char[] segmentBuff , Context context , int uBegin , int uEnd){
		Lexeme newLexeme = null;
		
		Hit hit = Dictionary.matchInPrepDict(segmentBuff, uBegin, 1);		
		if(hit.isUnmatch()){
			if(uBegin > 0){
				hit = Dictionary.matchInSurnameDict(segmentBuff, uBegin - 1 , 1);
				if(hit.isMatch()){
					
					newLexeme = new Lexeme(context.getBuffOffset() , uBegin - 1 , 1 , Lexeme.TYPE_CJK_SN);
					context.addLexeme(newLexeme);		
				}
			}			
		}
		
		
		for(int i = uBegin ; i <= uEnd ; i++){
			newLexeme = new Lexeme(context.getBuffOffset() , i , 1  , Lexeme.TYPE_CJK_UNKNOWN);
			context.addLexeme(newLexeme);		
		}
		
		hit = Dictionary.matchInPrepDict(segmentBuff, uEnd, 1);
		if(hit.isUnmatch()){
			int length = 1;
			while(uEnd < context.getAvailable() - length){
				hit = Dictionary.matchInSuffixDict(segmentBuff, uEnd + 1 , length);
				if(hit.isMatch()){
					
					newLexeme = new Lexeme(context.getBuffOffset() , uEnd + 1  , length , Lexeme.TYPE_CJK_SF);
					context.addLexeme(newLexeme);
					break;
				}
				if(hit.isUnmatch()){
					break;
				}
				length++;
			}
		}		
	}
	
	public void reset() {
		
		doneIndex = -1;
		hitList.clear();
	}
}
