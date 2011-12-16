/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * 
 */

import org.wltea.analyzer.help.CharacterHelper;

/**
 * @author Administrator
 *
 */
public class CharacterTest {

	public void testSBC2DBCChar(){
		char a = '‘';


		System.out.println((int)a);
		System.out.println(CharacterHelper.regularize(a));
		System.out.println((int)CharacterHelper.regularize(a));
		
		String sss  = "智灵通乳酸钙冲剂(5g\14袋)-1244466518522.txt";
		System.out.println(sss.replaceAll("[\\\\]", "每"));
	}
}
