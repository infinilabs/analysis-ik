/**
 * 
 */
package org.wltea.analyzer.dic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.wltea.analyzer.cfg.Configuration;

public class Dictionary {

	public static final String PATH_DIC_MAIN = "ik/main.dic";
	public static final String PATH_DIC_SURNAME = "ik/surname.dic";
	public static final String PATH_DIC_QUANTIFIER = "ik/quantifier.dic";
	public static final String PATH_DIC_SUFFIX = "ik/suffix.dic";
	public static final String PATH_DIC_PREP = "ik/preposition.dic";
	public static final String PATH_DIC_STOP = "ik/stopword.dic";
	private static final Dictionary singleton;

	static{
		singleton = new Dictionary();
	}

	private DictSegment _MainDict;

	private DictSegment _SurnameDict;

	private DictSegment _QuantifierDict;

	private DictSegment _SuffixDict;

	private DictSegment _PrepDict;

	private DictSegment _StopWords;

    private Environment environment;
    private Configuration configuration;
    private ESLogger logger=null;
    private static boolean dictInited=false;
	private Dictionary(){
        logger = Loggers.getLogger("ik-analyzer");
	}



    public void Init(Settings settings){

//        logger.info("[Init Setting] {}",settings.getAsMap().toString());

            if(!dictInited){
                environment =new Environment(settings);
                configuration=new Configuration(settings);
                loadMainDict();
                loadSurnameDict();
                loadQuantifierDict();
                loadSuffixDict();
                loadPrepDict();
                loadStopWordDict();
                dictInited=true;
            }
    }

	private void loadMainDict(){
		_MainDict = new DictSegment((char)0);

        File file= new File(environment.configFile(), Dictionary.PATH_DIC_MAIN);

//        logger.info("[Main Dict Loading] {}",file.getAbsolutePath());
//        logger.info("[Environment] {}",environment.homeFile());
//        logger.info("[Environment] {}",environment.workFile());
//        logger.info("[Environment] {}",environment.workWithClusterFile());
//        logger.info("[Environment] {}",environment.dataFiles());
//        logger.info("[Environment] {}",environment.dataWithClusterFiles());
//        logger.info("[Environment] {}",environment.configFile());
//        logger.info("[Environment] {}",environment.pluginsFile());
//        logger.info("[Environment] {}",environment.logsFile());

        InputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(is == null){
        	throw new RuntimeException("Main Dictionary not found!!!");
        }

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
			String theWord;
			do {
				theWord = br.readLine();
				if (theWord != null && !"".equals(theWord.trim())) {
					_MainDict.fillSegment(theWord.trim().toCharArray());
				}
			} while (theWord != null);
         logger.info("[Dict Loading] {},MainDict Size:{}",file.toString(),_MainDict.getDicNum());
		} catch (IOException ioe) {
			System.err.println("Main Dictionary loading exception.");
			ioe.printStackTrace();

		}finally{
			try {
				if(is != null){
                    is.close();
                    is = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


		List<String> extDictFiles  = configuration.getExtDictionarys();
		if(extDictFiles != null){
			for(String extDictName : extDictFiles){

                File tempFile=new File(environment.configFile(),extDictName);
                try {
                    is = new FileInputStream(tempFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                if(is == null){
					continue;
				}
				try {

					BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
					String theWord;
					do {
						theWord = br.readLine();
						if (theWord != null && !"".equals(theWord.trim())) {


							_MainDict.fillSegment(theWord.trim().toCharArray());
						}
					} while (theWord != null);
                 logger.info("[Dict Loading] {},MainDict Size:{}",file.toString(),_MainDict.getDicNum());
				} catch (IOException ioe) {
					System.err.println("Extension Dictionary loading exception.");
					ioe.printStackTrace();

				}finally{
					try {
						if(is != null){
		                    is.close();
		                    is = null;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
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
            e.printStackTrace();
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
         logger.info("[Dict Loading] {},SurnameDict Size:{}",file.toString(),_SurnameDict.getDicNum());
		} catch (IOException ioe) {
			System.err.println("Surname Dictionary loading exception.");
			ioe.printStackTrace();

		}finally{
			try {
				if(is != null){
                    is.close();
                    is = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	private void loadQuantifierDict(){

		_QuantifierDict = new DictSegment((char)0);
        File file=new File(environment.configFile(),Dictionary.PATH_DIC_QUANTIFIER);
        InputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(is == null){
        	throw new RuntimeException("Quantifier Dictionary not found!!!");
        }
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
			String theWord;
			do {
				theWord = br.readLine();
				if (theWord != null && !"".equals(theWord.trim())) {
					_QuantifierDict.fillSegment(theWord.trim().toCharArray());
				}
			} while (theWord != null);
        logger.info("[Dict Loading] {},QuantifierDict Size:{}",file.toString(),_QuantifierDict.getDicNum());
		} catch (IOException ioe) {
			System.err.println("Quantifier Dictionary loading exception.");
			ioe.printStackTrace();

		}finally{
			try {
				if(is != null){
                    is.close();
                    is = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
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
            e.printStackTrace();
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
            logger.info("[Dict Loading] {},SuffixDict Size:{}",file.toString(),_SuffixDict.getDicNum());
		} catch (IOException ioe) {
			System.err.println("Suffix Dictionary loading exception.");
			ioe.printStackTrace();

		}finally{
			try {
				if(is != null){
                    is.close();
                    is = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
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
            e.printStackTrace();
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
            logger.info("[Dict Loading] {},PrepDict Size:{}",file.toString(),_PrepDict.getDicNum());
		} catch (IOException ioe) {
			System.err.println("Preposition Dictionary loading exception.");
			ioe.printStackTrace();

		}finally{
			try {
				if(is != null){
                    is.close();
                    is = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	private void loadStopWordDict(){

		_StopWords = new DictSegment((char)0);
        File file=new File(environment.configFile(),Dictionary.PATH_DIC_STOP);
        InputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(is == null){
        	throw new RuntimeException("Stopword Dictionary not found!!!");
        }
		try {

            BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
			String theWord;
			do {
				theWord = br.readLine();
				if (theWord != null && !"".equals(theWord.trim())) {
					_StopWords.fillSegment(theWord.trim().toCharArray());
				}
			} while (theWord != null);
            logger.info("[Dict Loading] {},Stopwords Size:{}",file.toString(),_StopWords.getDicNum());
		} catch (IOException ioe) {
			System.err.println("Stopword Dictionary loading exception.");
			ioe.printStackTrace();

		}finally{
			try {
				if(is != null){
                    is.close();
                    is = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


		List<String> extStopWordDictFiles  = configuration.getExtStopWordDictionarys();
		if(extStopWordDictFiles != null){
			for(String extStopWordDictName : extStopWordDictFiles){
                File tempFile=new File(environment.configFile(),extStopWordDictName);
                try {
                    is = new FileInputStream(tempFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                if(is == null){
					continue;
				}
				try {

                    BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
					String theWord;
					do {
						theWord = br.readLine();
						if (theWord != null && !"".equals(theWord.trim())) {


							_StopWords.fillSegment(theWord.trim().toCharArray());
						}
					} while (theWord != null);
                    logger.info("[Dict Loading] {},Stopwords Size:{}",tempFile.toString(),_StopWords.getDicNum());
				} catch (IOException ioe) {
					System.err.println("Extension Stop word Dictionary loading exception.");
					ioe.printStackTrace();

				}finally{
					try {
						if(is != null){
		                    is.close();
		                    is = null;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}

	public static Dictionary getInstance(){
		return Dictionary.singleton;
	}

	public static void loadExtendWords(Collection<String> extWords){
		if(extWords != null){
			for(String extWord : extWords){
				if (extWord != null) {

					singleton._MainDict.fillSegment(extWord.trim().toCharArray());
				}
			}
		}
	}


	public static void loadExtendStopWords(Collection<String> extStopWords){
		if(extStopWords != null){
			for(String extStopWord : extStopWords){
				if (extStopWord != null) {

					singleton._StopWords.fillSegment(extStopWord.trim().toCharArray());
				}
			}
		}
	}


	public static Hit matchInMainDict(char[] charArray){
		return singleton._MainDict.match(charArray);
	}


	public static Hit matchInMainDict(char[] charArray , int begin, int length){
		return singleton._MainDict.match(charArray, begin, length);
	}


	public static Hit matchInMainDictWithHit(char[] charArray , int currentIndex , Hit matchedHit){
		DictSegment ds = matchedHit.getMatchedDictSegment();
		return ds.match(charArray, currentIndex, 1 , matchedHit);
	}

	
	public static Hit matchInSurnameDict(char[] charArray , int begin, int length){
		return singleton._SurnameDict.match(charArray, begin, length);
	}





















	/**
	 * 检索匹配量词词典
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return Hit 匹配结果描述
	 */
	public static Hit matchInQuantifierDict(char[] charArray , int begin, int length){
		return singleton._QuantifierDict.match(charArray, begin, length);
	}

	/**
	 * 检索匹配在后缀词典
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return Hit 匹配结果描述
	 */
	public static Hit matchInSuffixDict(char[] charArray , int begin, int length){
		return singleton._SuffixDict.match(charArray, begin, length);
	}






















	/**
	 * 检索匹配介词、副词词典
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return  Hit 匹配结果描述
	 */
	public static Hit matchInPrepDict(char[] charArray , int begin, int length){
		return singleton._PrepDict.match(charArray, begin, length);
	}

	/**
	 * 判断是否是停止词
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return boolean
	 */
	public static boolean isStopWord(char[] charArray , int begin, int length){			
		return singleton._StopWords.match(charArray, begin, length).isMatch();
	}	
}
