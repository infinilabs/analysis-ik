/**
 * 
 */
package org.wltea.analyzer.cfg;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;

import java.io.*;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Properties;

public class Configuration {

	private static String FILE_NAME = "ik/IKAnalyzer.cfg.xml";
	private static final String EXT_DICT = "ext_dict";
	private static final String EXT_STOP = "ext_stopwords";
    private static ESLogger logger = null;
	private Properties props;
    /*
	 * 是否使用smart方式分词
	 */
    private boolean useSmart=true;

	public  Configuration(Settings settings){

        logger = Loggers.getLogger("ik-analyzer");
		props = new Properties();
        Environment environment=new Environment(settings);
        File fileConfig= new File(environment.configFile(), FILE_NAME);

        InputStream input = null;
        try {
            input = new FileInputStream(fileConfig);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(input != null){
			try {
				props.loadFromXML(input);
                logger.info("[Dict Loading] {}",FILE_NAME);
			} catch (InvalidPropertiesFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

    /**
     * 返回useSmart标志位
     * useSmart =true ，分词器使用智能切分策略， =false则使用细粒度切分
     * @return useSmart
     */
    public boolean useSmart() {
        return useSmart;
    }

    /**
     * 设置useSmart标志位
     * useSmart =true ，分词器使用智能切分策略， =false则使用细粒度切分
     * @param useSmart
     */
    public void setUseSmart(boolean useSmart) {
        this.useSmart = useSmart;
    }



    public  List<String> getExtDictionarys(){
		List<String> extDictFiles = new ArrayList<String>(2);
		String extDictCfg = props.getProperty(EXT_DICT);
		if(extDictCfg != null){

			String[] filePaths = extDictCfg.split(";");
			if(filePaths != null){
				for(String filePath : filePaths){
					if(filePath != null && !"".equals(filePath.trim())){
                        File file=new File("ik",filePath.trim());
						extDictFiles.add(file.toString());

					}
				}
			}
		}		
		return extDictFiles;		
	}

	public List<String> getExtStopWordDictionarys(){
		List<String> extStopWordDictFiles = new ArrayList<String>(2);
		String extStopWordDictCfg = props.getProperty(EXT_STOP);
		if(extStopWordDictCfg != null){
			
			String[] filePaths = extStopWordDictCfg.split(";");
			if(filePaths != null){
				for(String filePath : filePaths){
					if(filePath != null && !"".equals(filePath.trim())){
                        File file=new File("ik",filePath.trim());
						extStopWordDictFiles.add(file.toString());

					}
				}
			}
		}		
		return extStopWordDictFiles;		
	}
}
