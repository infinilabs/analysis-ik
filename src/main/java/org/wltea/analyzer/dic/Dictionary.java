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
 * 
 */
package org.wltea.analyzer.dic;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.wltea.analyzer.cfg.Configuration;

import java.io.*;
import java.util.Collection;
import java.util.List;

/**
 * 词典管理类,单子模式
 */
public class Dictionary {


	/*
	 * 词典单子实例
	 */
	private static Dictionary singleton;

    private DictSegment _MainDict;

    private DictSegment _SurnameDict;

    private DictSegment _QuantifierDict;

    private DictSegment _SuffixDict;

    private DictSegment _PrepDict;

    private DictSegment _StopWords;

	
	/**
	 * 配置对象
	 */
	private Configuration configuration;
    private ESLogger logger=null;
    private static boolean dictInited=false;
    private Environment environment;
    public static final String PATH_DIC_MAIN = "ik/main.dic";
    public static final String PATH_DIC_SURNAME = "ik/surname.dic";
    public static final String PATH_DIC_QUANTIFIER = "ik/quantifier.dic";
    public static final String PATH_DIC_SUFFIX = "ik/suffix.dic";
    public static final String PATH_DIC_PREP = "ik/preposition.dic";
    public static final String PATH_DIC_STOP = "ik/stopword.dic";
    private Dictionary(){
        logger = Loggers.getLogger("ik-analyzer");
    }
    static{
        singleton = new Dictionary();
    }
//    public Configuration getConfig(){
//        return  configuration;
//    }
//	private Dictionary(Configuration cfg){
//		this.cfg = cfg;
//		this.loadMainDict();
//		this.loadStopWordDict();
//		this.loadQuantifierDict();
//	}

    public void Init(Settings indexSettings){

        if(!dictInited){
            environment =new Environment(indexSettings);
            configuration=new Configuration(indexSettings);
            loadMainDict();
            loadSurnameDict();
            loadQuantifierDict();
            loadSuffixDict();
            loadPrepDict();
            loadStopWordDict();
            dictInited=true;
        }
    }

	/**
	 * 词典初始化
	 * 由于IK Analyzer的词典采用Dictionary类的静态方法进行词典初始化
	 * 只有当Dictionary类被实际调用时，才会开始载入词典，
	 * 这将延长首次分词操作的时间
	 * 该方法提供了一个在应用加载阶段就初始化字典的手段
	 * @return Dictionary
	 */
//	public static Dictionary initial(Configuration cfg){
//		if(singleton == null){
//			synchronized(Dictionary.class){
//				if(singleton == null){
//					singleton = new Dictionary();
//					return singleton;
//				}
//			}
//		}
//		return singleton;
//	}
	
	/**
	 * 获取词典单子实例
	 * @return Dictionary 单例对象
	 */
	public static Dictionary getSingleton(){
		if(singleton == null){
			throw new IllegalStateException("词典尚未初始化，请先调用initial方法");
		}
		return singleton;
	}
	
	/**
	 * 批量加载新词条
	 * @param words Collection<String>词条列表
	 */
	public void addWords(Collection<String> words){
		if(words != null){
			for(String word : words){
				if (word != null) {
					//批量加载词条到主内存词典中
					singleton._MainDict.fillSegment(word.trim().toLowerCase().toCharArray());
				}
			}
		}
	}
	
	/**
	 * 批量移除（屏蔽）词条
	 * @param words
	 */
	public void disableWords(Collection<String> words){
		if(words != null){
			for(String word : words){
				if (word != null) {
					//批量屏蔽词条
					singleton._MainDict.disableSegment(word.trim().toLowerCase().toCharArray());
				}
			}
		}
	}
	
	/**
	 * 检索匹配主词典
	 * @param charArray
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray){
		return singleton._MainDict.match(charArray);
	}
	
	/**
	 * 检索匹配主词典
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray , int begin, int length){
		return singleton._MainDict.match(charArray, begin, length);
	}
	
	/**
	 * 检索匹配量词词典
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInQuantifierDict(char[] charArray , int begin, int length){
		return singleton._QuantifierDict.match(charArray, begin, length);
	}
	
	
	/**
	 * 从已匹配的Hit中直接取出DictSegment，继续向下匹配
	 * @param charArray
	 * @param currentIndex
	 * @param matchedHit
	 * @return Hit
	 */
	public Hit matchWithHit(char[] charArray , int currentIndex , Hit matchedHit){
		DictSegment ds = matchedHit.getMatchedDictSegment();
		return ds.match(charArray, currentIndex, 1 , matchedHit);
	}
	
	
	/**
	 * 判断是否是停止词
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return boolean
	 */
	public boolean isStopWord(char[] charArray , int begin, int length){			
		return singleton._StopWords.match(charArray, begin, length).isMatch();
	}	
	
	/**
	 * 加载主词典及扩展词典
	 */
	private void loadMainDict(){
		//建立一个主词典实例
		_MainDict = new DictSegment((char)0);
		//读取主词典文件
        File file= new File(environment.configFile(), Dictionary.PATH_DIC_MAIN);

        InputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
			String theWord = null;
			do {
				theWord = br.readLine();
				if (theWord != null && !"".equals(theWord.trim())) {
					_MainDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
				}
			} while (theWord != null);
			
		} catch (IOException e) {
            logger.error("ik-analyzer",e);

        }finally{
			try {
				if(is != null){
                    is.close();
                    is = null;
				}
			} catch (IOException e) {
                logger.error("ik-analyzer",e);
			}
		}
		//加载扩展词典
		this.loadExtDict();
	}	
	
	/**
	 * 加载用户配置的扩展词典到主词库表
	 */
	private void loadExtDict(){
		//加载扩展词典配置
		List<String> extDictFiles  = configuration.getExtDictionarys();
		if(extDictFiles != null){
			InputStream is = null;
			for(String extDictName : extDictFiles){
				//读取扩展词典文件
                logger.info("加载扩展词典：" + extDictName);
                File file=new File(environment.configFile(), extDictName);
                try {
                    is = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    logger.error("ik-analyzer",e);
                }

				//如果找不到扩展的字典，则忽略
				if(is == null){
					continue;
				}
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
					String theWord = null;
					do {
						theWord = br.readLine();
						if (theWord != null && !"".equals(theWord.trim())) {
							//加载扩展词典数据到主内存词典中
							_MainDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
						}
					} while (theWord != null);
					
				} catch (IOException e) {
                    logger.error("ik-analyzer",e);
                }finally{
					try {
						if(is != null){
		                    is.close();
		                    is = null;
						}
					} catch (IOException e) {
                        logger.error("ik-analyzer",e);
                    }
				}
			}
		}		
	}
	
	/**
	 * 加载用户扩展的停止词词典
	 */
	private void loadStopWordDict(){
		//建立一个主词典实例
        _StopWords = new DictSegment((char)0);
		//加载扩展停止词典
		List<String> extStopWordDictFiles  = configuration.getExtStopWordDictionarys();
		if(extStopWordDictFiles != null){
			InputStream is = null;
			for(String extStopWordDictName : extStopWordDictFiles){
//				logger.info("加载扩展停止词典：" + extStopWordDictName);

				//读取扩展词典文件
                File file=new File(environment.configFile(), extStopWordDictName);
                try {
                    is = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    logger.error("ik-analyzer",e);
                }
				//如果找不到扩展的字典，则忽略
				if(is == null){
					continue;
				}
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
					String theWord = null;
					do {
						theWord = br.readLine();
						if (theWord != null && !"".equals(theWord.trim())) {
							//加载扩展停止词典数据到内存中
                            _StopWords.fillSegment(theWord.trim().toLowerCase().toCharArray());
						}
					} while (theWord != null);
					
				} catch (IOException e) {
                    logger.error("ik-analyzer",e);
					
				}finally{
					try {
						if(is != null){
		                    is.close();
		                    is = null;
						}
					} catch (IOException e) {
                        logger.error("ik-analyzer",e);
					}
				}
			}
		}		
	}
	
	/**
	 * 加载量词词典
	 */
	private void loadQuantifierDict(){
		//建立一个量词典实例
		_QuantifierDict = new DictSegment((char)0);
		//读取量词词典文件
        File file=new File(environment.configFile(),Dictionary.PATH_DIC_QUANTIFIER);
        InputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            logger.error("ik-analyzer",e);
        }
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
			String theWord = null;
			do {
				theWord = br.readLine();
				if (theWord != null && !"".equals(theWord.trim())) {
					_QuantifierDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
				}
			} while (theWord != null);
			
		} catch (IOException ioe) {
			logger.error("Quantifier Dictionary loading exception.");
			
		}finally{
			try {
				if(is != null){
                    is.close();
                    is = null;
				}
			} catch (IOException e) {
                logger.error("ik-analyzer",e);
			}
		}
	}


    private void loadSurnameDict(){

        _SurnameDict = new DictSegment((char)0);
        File file=new File(environment.configFile(),Dictionary.PATH_DIC_SURNAME);
        InputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            logger.error("ik-analyzer",e);
        }
        if(is == null){
            throw new RuntimeException("Surname Dictionary not found!!!");
        }
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
            String theWord;
            do {
                theWord = br.readLine();
                if (theWord != null && !"".equals(theWord.trim())) {
                    _SurnameDict.fillSegment(theWord.trim().toCharArray());
                }
            } while (theWord != null);
//            logger.info("[Dict Loading] {},SurnameDict Size:{}",file.toString(),_SurnameDict.getDicNum());
        } catch (IOException e) {
            logger.error("ik-analyzer",e);
        }finally{
            try {
                if(is != null){
                    is.close();
                    is = null;
                }
            } catch (IOException e) {
                logger.error("ik-analyzer",e);
            }
        }
    }



    private void loadSuffixDict(){

        _SuffixDict = new DictSegment((char)0);
        File file=new File(environment.configFile(),Dictionary.PATH_DIC_SUFFIX);
        InputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            logger.error("ik-analyzer",e);
        }
        if(is == null){
            throw new RuntimeException("Suffix Dictionary not found!!!");
        }
        try {

            BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
            String theWord;
            do {
                theWord = br.readLine();
                if (theWord != null && !"".equals(theWord.trim())) {
                    _SuffixDict.fillSegment(theWord.trim().toCharArray());
                }
            } while (theWord != null);
//            logger.info("[Dict Loading] {},SuffixDict Size:{}",file.toString(),_SuffixDict.getDicNum());
        } catch (IOException e) {
            logger.error("ik-analyzer",e);
        }finally{
            try {
                if(is != null){
                    is.close();
                    is = null;
                }
            } catch (IOException e) {
                logger.error("ik-analyzer",e);
            }
        }
    }


    private void loadPrepDict(){

        _PrepDict = new DictSegment((char)0);
        File file=new File(environment.configFile(),Dictionary.PATH_DIC_PREP);
        InputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            logger.error("ik-analyzer",e);
        }
        if(is == null){
            throw new RuntimeException("Preposition Dictionary not found!!!");
        }
        try {

            BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
            String theWord;
            do {
                theWord = br.readLine();
                if (theWord != null && !"".equals(theWord.trim())) {

                    _PrepDict.fillSegment(theWord.trim().toCharArray());
                }
            } while (theWord != null);
//            logger.info("[Dict Loading] {},PrepDict Size:{}",file.toString(),_PrepDict.getDicNum());
        } catch (IOException e) {
            logger.error("ik-analyzer",e);
        }finally{
            try {
                if(is != null){
                    is.close();
                    is = null;
                }
            } catch (IOException e) {
                logger.error("ik-analyzer",e);
            }
        }
    }

    public static Dictionary getInstance(){
        return Dictionary.singleton;
    }
	
}
