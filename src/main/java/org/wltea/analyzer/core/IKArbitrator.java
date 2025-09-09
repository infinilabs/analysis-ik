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
					QuickSortSet.Cell headCell = crossPath.getHead();
					LexemePath judgeResult = this.judge(headCell, crossPath.getPathLength());
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
			QuickSortSet.Cell headCell = crossPath.getHead();
			LexemePath judgeResult = this.judge(headCell, crossPath.getPathLength());
			//输出歧义处理结果judgeResult
			context.addLexemePath(judgeResult);
		}
	}
	
	/**
	 * 检测是否为叠词模式
	 * @param lexemeCell 词元链表头
	 * @return 如果检测到叠词模式返回简化的路径，否则返回null
	 */
	private LexemePath detectRepeatedWords(QuickSortSet.Cell lexemeCell) {
		if (lexemeCell == null || lexemeCell.getLexeme() == null) {
			return null;
		}
		
		// 检查是否有连续的长词元（长度>10）或大量重复词元
		QuickSortSet.Cell current = lexemeCell;
		int longLexemeCount = 0;
		int totalCount = 0;
		Lexeme firstLexeme = null;
		Lexeme lastLexeme = null;
		
		while (current != null && current.getLexeme() != null) {
			Lexeme lexeme = current.getLexeme();
			if (firstLexeme == null) {
				firstLexeme = lexeme;
			}
			lastLexeme = lexeme;
			
			if (lexeme.getLength() > 10) {
				longLexemeCount++;
			}
			totalCount++;
			
			// 如果发现多个长词元或词元总数过多，认为是叠词
			if (longLexemeCount > 5 || totalCount > 50) {
				// 构造简化路径：第一个词元 + 剩余部分合并为一个词元
				LexemePath simplifiedPath = new LexemePath();
				
				// 添加第一个词元
				simplifiedPath.addNotCrossLexeme(firstLexeme);
				
				// 如果有剩余部分，创建一个合并的词元
				if (totalCount > 1 && lastLexeme != null) {
					// 计算剩余部分的起始位置和长度
					int remainStart = firstLexeme.getBegin() + firstLexeme.getLength();
					int remainEnd = lastLexeme.getBegin() + lastLexeme.getLength();
					int remainLength = remainEnd - remainStart;
					
					if (remainLength > 0) {
						// 创建一个表示剩余部分的词元
						// offset 应该是第一个词元的 offset + 第一个词元的长度
						int remainOffset = firstLexeme.getOffset() + firstLexeme.getLength();
						Lexeme remainLexeme = new Lexeme(remainOffset, remainStart, remainLength, Lexeme.TYPE_CNCHAR);
						simplifiedPath.addNotCrossLexeme(remainLexeme);
					}
				}
				
				return simplifiedPath;
			}
			
			current = current.getNext();
		}
		
		return null; // 没有检测到叠词模式
	}

	/**
	 * 歧义识别
	 * @param lexemeCell 歧义路径链表头
	 * @param fullTextLength 歧义路径文本长度
	 * @return
	 */
	private LexemePath judge(QuickSortSet.Cell lexemeCell , int fullTextLength){
		// 首先检测是否为叠词模式，如果是则直接返回简化路径
		LexemePath simplifiedPath = this.detectRepeatedWords(lexemeCell);
		if (simplifiedPath != null) {
			//System.out.println("Detected repeated words pattern, using simplified path");
			return simplifiedPath;
		}
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
			//限制大长度叠词，避免性能问题和整数溢出
			if(c.getLexeme().getLength() > 10){
				//System.out.println("already repeat words 10 times");
				//跳过过长的词元
				c = c.getNext();
				continue;
			}

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
