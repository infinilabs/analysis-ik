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

import java.util.Stack;
import java.util.TreeSet;

/**
 * IK分词歧义裁决器
 */
class IKArbitrator {
	private static final int LONG_CN_WORD_LENGTH_THRESHOLD = 10;
	private static final int LONG_CN_WORD_COUNT_THRESHOLD = 5;
	private static final int TOTAL_LEXEME_COUNT_THRESHOLD = 50;
	private static final int DENSE_CROSS_PATH_SIZE_THRESHOLD = 20;
	private static final int DENSE_OVERLAP_RATIO_THRESHOLD = 4;

	IKArbitrator(){
		
	}
	
	/**
	 * 分词歧义处理
//	 * @param orgLexemes
	 * @param useSmart
	 */
	void process(AnalyzeContext context , boolean useSmart){
		QuickSortSet orgLexemes = context.getOrgLexemes();
		Lexeme orgLexeme = orgLexemes.pollFirst();
		
		LexemePath crossPath = new LexemePath();
		while(orgLexeme != null){
			if(!crossPath.addCrossLexeme(orgLexeme)){
				//找到与crossPath不相交的下一个crossPath	
				if(crossPath.size() == 1 || !useSmart){
					//crossPath没有歧义 或者 不做歧义处理
					//直接输出当前crossPath
					context.addLexemePath(crossPath);
				}else{
					//对当前的crossPath进行歧义处理
					LexemePath judgeResult = this.judge(crossPath);
					//输出歧义处理结果judgeResult
					context.addLexemePath(judgeResult);
				}
				
				//把orgLexeme加入新的crossPath中
				crossPath = new LexemePath();
				crossPath.addCrossLexeme(orgLexeme);
			}
			orgLexeme = orgLexemes.pollFirst();
		}
		
		
		//处理最后的path
		if(crossPath.size() == 1 || !useSmart){
			//crossPath没有歧义 或者 不做歧义处理
			//直接输出当前crossPath
			context.addLexemePath(crossPath);
		}else{
			//对当前的crossPath进行歧义处理
			LexemePath judgeResult = this.judge(crossPath);
			//输出歧义处理结果judgeResult
			context.addLexemePath(judgeResult);
		}
	}
	
/**
	 * 为过于复杂的交叉路径构造低成本兜底路径。
	 * 该逻辑是ik_smart歧义裁决的性能护栏，避免在高密度crossPath上进行昂贵的回溯裁决。
	 * @param crossPath 当前待裁决的交叉词元路径
	 * @return 如果路径复杂度超过阈值则返回兜底路径，否则返回null
	 */
	private LexemePath tryBuildFallbackPathForComplexCrossPath(LexemePath crossPath) {
		if (crossPath == null || crossPath.isEmpty()) {
			return null;
		}

		if (!this.shouldFallbackForComplexCrossPath(crossPath)) {
			return null;
		}

		return this.buildFallbackPath(crossPath);
	}

	/**
	 * 判断当前交叉路径是否已经复杂到需要跳过正常回溯裁决。
	 */
	private boolean shouldFallbackForComplexCrossPath(LexemePath crossPath) {
		if (crossPath.size() > TOTAL_LEXEME_COUNT_THRESHOLD) {
			return true;
		}

		int longCnWordCount = 0;
		long totalLexemeLength = 0;
		QuickSortSet.Cell current = crossPath.getHead();
		while (current != null && current.getLexeme() != null) {
			Lexeme lexeme = current.getLexeme();

			totalLexemeLength += lexeme.getLength();

			if (lexeme.getLexemeType() == Lexeme.TYPE_CNWORD
					&& lexeme.getLength() > LONG_CN_WORD_LENGTH_THRESHOLD) {
				longCnWordCount++;
			}
			current = current.getNext();
		}

		int pathLength = crossPath.getPathEnd() - crossPath.getPathBegin();
		if (pathLength <= 0) {
			return false;
		}

		return longCnWordCount > LONG_CN_WORD_COUNT_THRESHOLD
				&& crossPath.size() >= DENSE_CROSS_PATH_SIZE_THRESHOLD
				&& totalLexemeLength >= (long) pathLength * DENSE_OVERLAP_RATIO_THRESHOLD;
	}

	/**
	 * 构造低成本兜底路径。保留首词，并将其后的覆盖区间合成为一个词元。
	 */
	private LexemePath buildFallbackPath(LexemePath crossPath) {
		Lexeme firstLexeme = crossPath.peekFirst();
		if (firstLexeme == null) {
			return null;
		}

		LexemePath fallbackPath = new LexemePath();
		if (!fallbackPath.addNotCrossLexeme(firstLexeme)) {
			return null;
		}

		int remainStart = firstLexeme.getBegin() + firstLexeme.getLength();
		int remainLength = crossPath.getPathEnd() - remainStart;

		if (remainLength > 0) {
			Lexeme remainLexeme = new Lexeme(
					firstLexeme.getOffset(),
					remainStart,
					remainLength,
					Lexeme.TYPE_CNWORD);
			if (!fallbackPath.addNotCrossLexeme(remainLexeme)) {
				return null;
			}
		}

		return fallbackPath;
	}

	/**
	 * 歧义识别
	 * @param crossPath 歧义路径
	 * @return
	 */
	private LexemePath judge(LexemePath crossPath){
		// 首先判断当前crossPath是否过于复杂，如果是则直接返回低成本兜底路径
		LexemePath fallbackPath = this.tryBuildFallbackPathForComplexCrossPath(crossPath);
		if (fallbackPath != null) {
			return fallbackPath;
		}

		QuickSortSet.Cell lexemeCell = crossPath.getHead();

		//候选路径集合
		TreeSet<LexemePath> pathOptions = new TreeSet<LexemePath>();
		//候选结果路径
		LexemePath option = new LexemePath();
		
		//对crossPath进行一次遍历,同时返回本次遍历中有冲突的Lexeme栈
		Stack<QuickSortSet.Cell> lexemeStack = this.forwardPath(lexemeCell , option);
		
		//当前词元链并非最理想的，加入候选路径集合
		pathOptions.add(option.copy());
		
		//存在歧义词，处理
		QuickSortSet.Cell c = null;
		while(!lexemeStack.isEmpty()){
			c = lexemeStack.pop();
			//回滚词元链
			this.backPath(c.getLexeme() , option);
			//从歧义词位置开始，递归，生成可选方案
			this.forwardPath(c , option);
			pathOptions.add(option.copy());
		}
		
		//返回集合中的最优方案
		return pathOptions.first();

	}
	
	/**
	 * 向前遍历，添加词元，构造一个无歧义词元组合
//	 * @param LexemePath path
	 * @return
	 */
	private Stack<QuickSortSet.Cell> forwardPath(QuickSortSet.Cell lexemeCell , LexemePath option){
		//发生冲突的Lexeme栈
		Stack<QuickSortSet.Cell> conflictStack = new Stack<QuickSortSet.Cell>();
		QuickSortSet.Cell c = lexemeCell;
		//迭代遍历Lexeme链表
		while(c != null && c.getLexeme() != null){
			if(!option.addNotCrossLexeme(c.getLexeme())){
				//词元交叉，添加失败则加入lexemeStack栈
				conflictStack.push(c);
			}
			c = c.getNext();
		}
		return conflictStack;
	}
	
	/**
	 * 回滚词元链，直到它能够接受指定的词元
//	 * @param lexeme
	 * @param l
	 */
	private void backPath(Lexeme l  , LexemePath option){
		while(option.checkCross(l)){
			option.removeTail();
		}
		
	}
	
}
