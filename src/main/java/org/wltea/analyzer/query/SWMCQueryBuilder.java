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
package org.wltea.analyzer.query;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

/**
 * Single Word Multi Char Query Builder
 * IK分词算法专用
 * @author linliangyi
 *
 */
public class SWMCQueryBuilder {

	/**
	 * 生成SWMCQuery
	 * @param fieldName
	 * @param keywords
	 * @param quickMode
	 * @return Lucene Query
	 */
	public static Query create(String fieldName ,String keywords , boolean quickMode){
		if(fieldName == null || keywords == null){
			throw new IllegalArgumentException("参数 fieldName 、 keywords 不能为null.");
		}
		//1.对keywords进行分词处理
		List<Lexeme> lexemes = doAnalyze(keywords);
		//2.根据分词结果，生成SWMCQuery
		Query _SWMCQuery = getSWMCQuery(fieldName , lexemes , quickMode);
		return _SWMCQuery;
	}
	
	/**
	 * 分词切分，并返回结链表
	 * @param keywords
	 * @return
	 */
	private static List<Lexeme> doAnalyze(String keywords){
		List<Lexeme> lexemes = new ArrayList<Lexeme>();

		IKSegmenter ikSeg = new IKSegmenter(new StringReader(keywords));
		try{
			Lexeme l = null;
			while( (l = ikSeg.next()) != null){
				lexemes.add(l);
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		return lexemes;
	}
	
	
	/**
	 * 根据分词结果生成SWMC搜索
	 * @param fieldName
//	 * @param pathOption
	 * @param quickMode
	 * @return
	 */
	private static Query getSWMCQuery(String fieldName , List<Lexeme> lexemes , boolean quickMode){
		//构造SWMC的查询表达式
		StringBuffer keywordBuffer = new StringBuffer();
		//精简的SWMC的查询表达式
		StringBuffer keywordBuffer_Short = new StringBuffer();
		//记录最后词元长度
		int lastLexemeLength = 0;
		//记录最后词元结束位置
		int lastLexemeEnd = -1;
		
		int shortCount = 0;
		int totalCount = 0;
		for(Lexeme l : lexemes){
			totalCount += l.getLength();
			//精简表达式
			if(l.getLength() > 1){
				keywordBuffer_Short.append(' ').append(l.getLexemeText());
				shortCount += l.getLength();
			}
			
			if(lastLexemeLength == 0){
				keywordBuffer.append(l.getLexemeText());				
			}else if(lastLexemeLength == 1 && l.getLength() == 1
					&& lastLexemeEnd == l.getBeginPosition()){//单字位置相邻，长度为一，合并)
				keywordBuffer.append(l.getLexemeText());
			}else{
				keywordBuffer.append(' ').append(l.getLexemeText());
				
			}
			lastLexemeLength = l.getLength();
			lastLexemeEnd = l.getEndPosition();
		}

		//借助lucene queryparser 生成SWMC Query
		QueryParser qp = new QueryParser(Version.LUCENE_40, fieldName, new StandardAnalyzer(Version.LUCENE_40));
		qp.setDefaultOperator(QueryParser.AND_OPERATOR);
		qp.setAutoGeneratePhraseQueries(true);
		
		if(quickMode && (shortCount * 1.0f / totalCount) > 0.5f){
			try {
				//System.out.println(keywordBuffer.toString());
				Query q = qp.parse(keywordBuffer_Short.toString());
				return q;
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
		}else{
			if(keywordBuffer.length() > 0){
				try {
					//System.out.println(keywordBuffer.toString());
					Query q = qp.parse(keywordBuffer.toString());
					return q;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
}
