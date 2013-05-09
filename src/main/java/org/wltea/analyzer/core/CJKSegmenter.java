
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

import java.util.LinkedList;
import java.util.List;

import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.dic.Hit;


/**
 *  中文-日韩文子分词器
 */
class CJKSegmenter implements ISegmenter {
	
	//子分词器标签
	static final String SEGMENTER_NAME = "CJK_SEGMENTER";
	//待处理的分词hit队列
	private List<Hit> tmpHits;
	
	
	CJKSegmenter(){
		this.tmpHits = new LinkedList<Hit>();
	}

	/* (non-Javadoc)
	 * @see org.wltea.analyzer.core.ISegmenter#analyze(org.wltea.analyzer.core.AnalyzeContext)
	 */
	public void analyze(AnalyzeContext context) {
		if(CharacterUtil.CHAR_USELESS != context.getCurrentCharType()){
			
			//优先处理tmpHits中的hit
			if(!this.tmpHits.isEmpty()){
				//处理词段队列
				Hit[] tmpArray = this.tmpHits.toArray(new Hit[this.tmpHits.size()]);
				for(Hit hit : tmpArray){
					hit = Dictionary.getSingleton().matchWithHit(context.getSegmentBuff(), context.getCursor() , hit);
					if(hit.isMatch()){
						//输出当前的词
						Lexeme newLexeme = new Lexeme(context.getBufferOffset() , hit.getBegin() , context.getCursor() - hit.getBegin() + 1 , Lexeme.TYPE_CNWORD);
						context.addLexeme(newLexeme);
						
						if(!hit.isPrefix()){//不是词前缀，hit不需要继续匹配，移除
							this.tmpHits.remove(hit);
						}
						
					}else if(hit.isUnmatch()){
						//hit不是词，移除
						this.tmpHits.remove(hit);
					}					
				}
			}			
			
			//*********************************
			//再对当前指针位置的字符进行单字匹配
			Hit singleCharHit = Dictionary.getSingleton().matchInMainDict(context.getSegmentBuff(), context.getCursor(), 1);
			if(singleCharHit.isMatch()){//首字成词
				//输出当前的词
				Lexeme newLexeme = new Lexeme(context.getBufferOffset() , context.getCursor() , 1 , Lexeme.TYPE_CNWORD);
				context.addLexeme(newLexeme);

				//同时也是词前缀
				if(singleCharHit.isPrefix()){
					//前缀匹配则放入hit列表
					this.tmpHits.add(singleCharHit);
				}
			}else if(singleCharHit.isPrefix()){//首字为词前缀
				//前缀匹配则放入hit列表
				this.tmpHits.add(singleCharHit);
			}
			

		}else{
			//遇到CHAR_USELESS字符
			//清空队列
			this.tmpHits.clear();
		}
		
		//判断缓冲区是否已经读完
		if(context.isBufferConsumed()){
			//清空队列
			this.tmpHits.clear();
		}
		
		//判断是否锁定缓冲区
		if(this.tmpHits.size() == 0){
			context.unlockBuffer(SEGMENTER_NAME);
			
		}else{
			context.lockBuffer(SEGMENTER_NAME);
		}
	}

	/* (non-Javadoc)
	 * @see org.wltea.analyzer.core.ISegmenter#reset()
	 */
	public void reset() {
		//清空队列
		this.tmpHits.clear();
	}

}
