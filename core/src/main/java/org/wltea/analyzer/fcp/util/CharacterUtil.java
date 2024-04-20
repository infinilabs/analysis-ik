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
 * 字符集识别工具类
 */
package org.wltea.analyzer.fcp.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 字符集识别工具类
 */
public class CharacterUtil {
	
	public static final String CHAR_USELESS = "<CHAR_USELESS>";
	
	public static final String CHAR_ENGLISH = "<CHAR_ENGLISH>";

	public static final String CHAR_NUMBER = "<CHAR_NUMBER>";

	public static final String CHAR_NUMBER_DOT = "<CHAR_NUMBER_DOT>";

	public static final String ALPHANUM = "<ALPHANUM>";

	public static final String CHAR_CHINESE = "<CHAR_CHINESE>";

	public static final String COMBINE_WORD = "<COMBINE_WORD>";

	public static final String CHAR_MAPPING = "<CHAR_MAPPING>";

	public static final String CHAR_BLANK = "<CHAR_BLANK>";

	// pinyin
	public static final String CHAR_PINYIN = "<CHAR_PINYIN>";
	// pinyin 前缀
	public static final String CHAR_PINYIN_PRE = "<CHAR_PINYIN_PRE>";

	private static Map<String, Integer> order;
	static {
		// value 越小，排序越靠前，用于区分在同一个 position 上的不同 type 之间的排序
		order = new HashMap<>();
		order.put(CHAR_CHINESE, 0);
		order.put(CHAR_PINYIN_PRE, 5);
		order.put(CHAR_PINYIN, 10);

		order.put(CHAR_USELESS, 0);
		order.put(CHAR_MAPPING, 10);
	}

	public static int getOrderByType(String type) {
		return order.getOrDefault(type, 0);
	}



	/**
	 * 识别字符类型
	 * @param input
	 * @return int CharacterUtil定义的字符类型常量
	 */
	public static String identifyCharType(int input){

		if (input >= '0' && input <= '9') {
			return CHAR_NUMBER;
		} else if ((input >= 'a' && input <= 'z')
				|| (input >= 'A' && input <= 'Z')) {
			return CHAR_ENGLISH;
		} else {
			Character.UnicodeBlock ub = Character.UnicodeBlock.of(input);

			if(ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
					|| ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
					|| ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A){
				//目前已知的中文字符UTF-8集合
				return CHAR_CHINESE;

			}
		}
		//其他的不做处理的字符
		return CHAR_USELESS;

	}

	/**
	 * 进行字符规格化（全角转半角，大写转小写处理）
	 * @param input
	 * @return char
	 */
	public static int regularize(int input){
		if (input == 12288) {
			input = 32;

		}else if (input > 65280 && input < 65375) {
			input = input - 65248;

		}else if (input >= 'A' && input <= 'Z') {
			input += 32;
		}


		return input;
	}
}
