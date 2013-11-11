/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 * provided by Linliangyi and copyright 2012 by Oolong studio
 * 
 */
package org.wltea.analyzer.core;

import java.util.Arrays;

/**
 * 
 * 英文字符及阿拉伯数字子分词器
 */
class LetterSegmenter implements ISegmenter {
	
	//子分词器标签
	static final String SEGMENTER_NAME = "LETTER_SEGMENTER";
	//链接符号
	private static final char[] Letter_Connector = new char[]{'#' , '&' , '+' , '-' , '.' , '@' , '_'};
	
	//数字符号
	private static final char[] Num_Connector = new char[]{',' , '.'};
	
	/*
	 * 词元的开始位置，
	 * 同时作为子分词器状态标识
	 * 当start > -1 时，标识当前的分词器正在处理字符
	 */
	private int start;
	/*
	 * 记录词元结束位置
	 * end记录的是在词元中最后一个出现的Letter但非Sign_Connector的字符的位置
	 */
	private int end;
	
	/*
	 * 字母起始位置
	 */
	private int englishStart;

	/*
	 * 字母结束位置
	 */
	private int englishEnd;
	
	/*
	 * 阿拉伯数字起始位置
	 */
	private int arabicStart;
	
	/*
	 * 阿拉伯数字结束位置
	 */
	private int arabicEnd;
	
	LetterSegmenter(){
		Arrays.sort(Letter_Connector);
		Arrays.sort(Num_Connector);
		this.start = -1;
		this.end = -1;
		this.englishStart = -1;
		this.englishEnd = -1;
		this.arabicStart = -1;
		this.arabicEnd = -1;
	}


	/* (non-Javadoc)
	 * @see org.wltea.analyzer.core.ISegmenter#analyze(org.wltea.analyzer.core.AnalyzeContext)
	 */
	public void analyze(AnalyzeContext context) {
		boolean bufferLockFlag = false;
		//处理英文字母
		bufferLockFlag = this.processEnglishLetter(context) || bufferLockFlag;
		//处理阿拉伯字母
		bufferLockFlag = this.processArabicLetter(context) || bufferLockFlag;
		//处理混合字母(这个要放最后处理，可以通过QuickSortSet排除重复)
		bufferLockFlag = this.processMixLetter(context) || bufferLockFlag;
		
		//判断是否锁定缓冲区
		if(bufferLockFlag){
			context.lockBuffer(SEGMENTER_NAME);
		}else{
			//对缓冲区解锁
			context.unlockBuffer(SEGMENTER_NAME);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.wltea.analyzer.core.ISegmenter#reset()
	 */
	public void reset() {
		this.start = -1;
		this.end = -1;
		this.englishStart = -1;
		this.englishEnd = -1;
		this.arabicStart = -1;
		this.arabicEnd = -1;
	}	
	
	/**
	 * 处理数字字母混合输出
	 * 如：windos2000 | linliangyi2005@gmail.com
//	 * @param input
	 * @param context
	 * @return
	 */
	private boolean processMixLetter(AnalyzeContext context){
		boolean needLock = false;
		
		if(this.start == -1){//当前的分词器尚未开始处理字符
			if(CharacterUtil.CHAR_ARABIC == context.getCurrentCharType()
					|| CharacterUtil.CHAR_ENGLISH == context.getCurrentCharType()){
				//记录起始指针的位置,标明分词器进入处理状态
				this.start = context.getCursor();
				this.end = start;
			}
			
		}else{//当前的分词器正在处理字符			
			if(CharacterUtil.CHAR_ARABIC == context.getCurrentCharType()
					|| CharacterUtil.CHAR_ENGLISH == context.getCurrentCharType()){
				//记录下可能的结束位置
				this.end = context.getCursor();
				
			}else if(CharacterUtil.CHAR_USELESS == context.getCurrentCharType()
						&& this.isLetterConnector(context.getCurrentChar())){
				//记录下可能的结束位置
				this.end = context.getCursor();
			}else{
				//遇到非Letter字符，输出词元
				Lexeme newLexeme = new Lexeme(context.getBufferOffset() , this.start , this.end - this.start + 1 , Lexeme.TYPE_LETTER);
				context.addLexeme(newLexeme);
				this.start = -1;
				this.end = -1;
			}			
		}
		
		//判断缓冲区是否已经读完
		if(context.isBufferConsumed()){
			if(this.start != -1 && this.end != -1){
				//缓冲以读完，输出词元
				Lexeme newLexeme = new Lexeme(context.getBufferOffset() , this.start , this.end - this.start + 1 , Lexeme.TYPE_LETTER);
				context.addLexeme(newLexeme);
				this.start = -1;
				this.end = -1;
			}
		}
		
		//判断是否锁定缓冲区
		if(this.start == -1 && this.end == -1){
			//对缓冲区解锁
			needLock = false;
		}else{
			needLock = true;
		}
		return needLock;
	}
	
	/**
	 * 处理纯英文字母输出
	 * @param context
	 * @return
	 */
	private boolean processEnglishLetter(AnalyzeContext context){
		boolean needLock = false;
		
		if(this.englishStart == -1){//当前的分词器尚未开始处理英文字符	
			if(CharacterUtil.CHAR_ENGLISH == context.getCurrentCharType()){
				//记录起始指针的位置,标明分词器进入处理状态
				this.englishStart = context.getCursor();
				this.englishEnd = this.englishStart;
			}
		}else {//当前的分词器正在处理英文字符	
			if(CharacterUtil.CHAR_ENGLISH == context.getCurrentCharType()){
				//记录当前指针位置为结束位置
				this.englishEnd =  context.getCursor();
			}else{
				//遇到非English字符,输出词元
				Lexeme newLexeme = new Lexeme(context.getBufferOffset() , this.englishStart , this.englishEnd - this.englishStart + 1 , Lexeme.TYPE_ENGLISH);
				context.addLexeme(newLexeme);
				this.englishStart = -1;
				this.englishEnd= -1;
			}
		}
		
		//判断缓冲区是否已经读完
		if(context.isBufferConsumed()){
			if(this.englishStart != -1 && this.englishEnd != -1){
				//缓冲以读完，输出词元
				Lexeme newLexeme = new Lexeme(context.getBufferOffset() , this.englishStart , this.englishEnd - this.englishStart + 1 , Lexeme.TYPE_ENGLISH);
				context.addLexeme(newLexeme);
				this.englishStart = -1;
				this.englishEnd= -1;
			}
		}	
		
		//判断是否锁定缓冲区
		if(this.englishStart == -1 && this.englishEnd == -1){
			//对缓冲区解锁
			needLock = false;
		}else{
			needLock = true;
		}
		return needLock;			
	}
	
	/**
	 * 处理阿拉伯数字输出
	 * @param context
	 * @return
	 */
	private boolean processArabicLetter(AnalyzeContext context){
		boolean needLock = false;
		
		if(this.arabicStart == -1){//当前的分词器尚未开始处理数字字符	
			if(CharacterUtil.CHAR_ARABIC == context.getCurrentCharType()){
				//记录起始指针的位置,标明分词器进入处理状态
				this.arabicStart = context.getCursor();
				this.arabicEnd = this.arabicStart;
			}
		}else {//当前的分词器正在处理数字字符	
			if(CharacterUtil.CHAR_ARABIC == context.getCurrentCharType()){
				//记录当前指针位置为结束位置
				this.arabicEnd = context.getCursor();
			}else if(CharacterUtil.CHAR_USELESS == context.getCurrentCharType()
					&& this.isNumConnector(context.getCurrentChar())){
				//不输出数字，但不标记结束
			}else{
				////遇到非Arabic字符,输出词元
				Lexeme newLexeme = new Lexeme(context.getBufferOffset() , this.arabicStart , this.arabicEnd - this.arabicStart + 1 , Lexeme.TYPE_ARABIC);
				context.addLexeme(newLexeme);
				this.arabicStart = -1;
				this.arabicEnd = -1;
			}
		}
		
		//判断缓冲区是否已经读完
		if(context.isBufferConsumed()){
			if(this.arabicStart != -1 && this.arabicEnd != -1){
				//生成已切分的词元
				Lexeme newLexeme = new Lexeme(context.getBufferOffset() ,  this.arabicStart , this.arabicEnd - this.arabicStart + 1 , Lexeme.TYPE_ARABIC);
				context.addLexeme(newLexeme);
				this.arabicStart = -1;
				this.arabicEnd = -1;
			}
		}
		
		//判断是否锁定缓冲区
		if(this.arabicStart == -1 && this.arabicEnd == -1){
			//对缓冲区解锁
			needLock = false;
		}else{
			needLock = true;
		}
		return needLock;		
	}	

	/**
	 * 判断是否是字母连接符号
	 * @param input
	 * @return
	 */
	private boolean isLetterConnector(char input){
		int index = Arrays.binarySearch(Letter_Connector, input);
		return index >= 0;
	}
	
	/**
	 * 判断是否是数字连接符号
	 * @param input
	 * @return
	 */
	private boolean isNumConnector(char input){
		int index = Arrays.binarySearch(Num_Connector, input);
		return index >= 0;
	}
}
