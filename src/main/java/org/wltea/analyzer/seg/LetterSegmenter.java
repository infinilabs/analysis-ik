/**
 * 
 */
package org.wltea.analyzer.seg;

import org.wltea.analyzer.Lexeme;
import org.wltea.analyzer.Context;
import org.wltea.analyzer.help.CharacterHelper;

public class LetterSegmenter implements ISegmenter {
	
	public static final char[] Sign_Connector = new char[]{'-','_','.','@','&'};

	private int start;

	private int end;
	

	private int letterStart;


	private int letterEnd;

	private int numberStart;

	private int numberEnd;

	
	public LetterSegmenter(){
		start = -1;
		end = -1;
		letterStart = -1;
		letterEnd = -1;
		numberStart = -1;
		numberEnd = -1;
	}

	public void nextLexeme(char[] segmentBuff , Context context) {


		char input = segmentBuff[context.getCursor()];
		
		boolean bufferLockFlag = false;

		bufferLockFlag = this.processMixLetter(input, context) || bufferLockFlag;

		bufferLockFlag = this.processEnglishLetter(input, context) || bufferLockFlag;

		bufferLockFlag = this.processPureArabic(input, context) || bufferLockFlag;
		

		if(bufferLockFlag){

			context.unlockBuffer(this);
		}else{
			context.lockBuffer(this);
		}
	}
	

	private boolean processMixLetter(char input , Context context){
		boolean needLock = false;
		
		if(start == -1){
			if(isAcceptedCharStart(input)){

				start = context.getCursor();
				end = start;
			}
			
		}else{
			if(isAcceptedChar(input)){

				if(!isLetterConnector(input)){

					end = context.getCursor();					
				}
				
			}else{

				Lexeme newLexeme = new Lexeme(context.getBuffOffset() , start , end - start + 1 , Lexeme.TYPE_LETTER);
				context.addLexeme(newLexeme);

				start = -1;
				end = -1;
			}			
		}
		

		if(context.getCursor() == context.getAvailable() - 1){
			if(start != -1 && end != -1){

				Lexeme newLexeme = new Lexeme(context.getBuffOffset() , start , end - start + 1 , Lexeme.TYPE_LETTER);
				context.addLexeme(newLexeme);
			}

			start = -1;
			end = -1;
		}
		

		if(start == -1 && end == -1){

			needLock = false;
		}else{
			needLock = true;
		}
		return needLock;
	}
	

	private boolean processPureArabic(char input , Context context){
		boolean needLock = false;
		
		if(numberStart == -1){
			if(CharacterHelper.isArabicNumber(input)){

				numberStart = context.getCursor();
				numberEnd = numberStart;
			}
		}else {
			if(CharacterHelper.isArabicNumber(input)){

				numberEnd =  context.getCursor();
			}else{

				Lexeme newLexeme = new Lexeme(context.getBuffOffset() , numberStart , numberEnd - numberStart + 1 , Lexeme.TYPE_LETTER);
				context.addLexeme(newLexeme);

				numberStart = -1;
				numberEnd = -1;
			}
		}
		

		if(context.getCursor() == context.getAvailable() - 1){
			if(numberStart != -1 && numberEnd != -1){

				Lexeme newLexeme = new Lexeme(context.getBuffOffset() , numberStart , numberEnd - numberStart + 1 , Lexeme.TYPE_LETTER);
				context.addLexeme(newLexeme);
			}

			numberStart = -1;
			numberEnd = -1;
		}
		

		if(numberStart == -1 && numberEnd == -1){

			needLock = false;
		}else{
			needLock = true;
		}
		return needLock;		
	}
	

	private boolean processEnglishLetter(char input , Context context){
		boolean needLock = false;
		
		if(letterStart == -1){
			if(CharacterHelper.isEnglishLetter(input)){

				letterStart = context.getCursor();
				letterEnd = letterStart;
			}
		}else {
			if(CharacterHelper.isEnglishLetter(input)){

				letterEnd =  context.getCursor();
			}else{

				Lexeme newLexeme = new Lexeme(context.getBuffOffset() , letterStart , letterEnd - letterStart + 1 , Lexeme.TYPE_LETTER);
				context.addLexeme(newLexeme);

				letterStart = -1;
				letterEnd = -1;
			}
		}
		

		if(context.getCursor() == context.getAvailable() - 1){
			if(letterStart != -1 && letterEnd != -1){

				Lexeme newLexeme = new Lexeme(context.getBuffOffset() , letterStart , letterEnd - letterStart + 1 , Lexeme.TYPE_LETTER);
				context.addLexeme(newLexeme);
			}

			letterStart = -1;
			letterEnd = -1;
		}
		

		if(letterStart == -1 && letterEnd == -1){

			needLock = false;
		}else{
			needLock = true;
		}
		return needLock;			
	}
	

	private boolean isLetterConnector(char input){
		for(char c : Sign_Connector){
			if(c == input){
				return true;
			}
		}
		return false;
	}
	

	private boolean isAcceptedCharStart(char input){
		return CharacterHelper.isEnglishLetter(input) 
				|| CharacterHelper.isArabicNumber(input);
	}
	

	private boolean isAcceptedChar(char input){
		return isLetterConnector(input) 
				|| CharacterHelper.isEnglishLetter(input) 
				|| CharacterHelper.isArabicNumber(input);
	}

	public void reset() {
		start = -1;
		end = -1;
		letterStart = -1;
		letterEnd = -1;
		numberStart = -1;
		numberEnd = -1;		
	}
	

}
