/**
 * 
 */
package org.wltea.analyzer.cfg;

import java.io.*;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Properties;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.env.Environment;

public class Configuration {

	private static String FILE_NAME = "ik/IKAnalyzer.cfg.xml";
	private static final String EXT_DICT = "ext_dict";
	private static final String REMOTE_EXT_DICT = "remote_ext_dict";
	private static final String EXT_STOP = "ext_stopwords";
	private static final String REMOTE_EXT_STOP = "remote_ext_stopwords";
    private static ESLogger logger = null;
	private Properties props;
    private Environment environment;

    public  Configuration(Environment env){
        logger = Loggers.getLogger("ik-analyzer");
		props = new Properties();
        environment = env;

        File fileConfig = new File(environment.configFile().resolve(FILE_NAME).toString());

        InputStream input = null;
        try {
            input = new FileInputStream(fileConfig);
        } catch (FileNotFoundException e) {
            logger.error("ik-analyzer",e);
        }
        if(input != null){
			try {
				props.loadFromXML(input);
			} catch (InvalidPropertiesFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
    
    public  List<String> getRemoteExtDictionarys(){
		List<String> remoteExtDictFiles = new ArrayList<String>(2);
		String remoteExtDictCfg = props.getProperty(REMOTE_EXT_DICT);
		if(remoteExtDictCfg != null){

			String[] filePaths = remoteExtDictCfg.split(";");
			if(filePaths != null){
				for(String filePath : filePaths){
					if(filePath != null && !"".equals(filePath.trim())){
						remoteExtDictFiles.add(filePath);

					}
				}
			}
		}		
		return remoteExtDictFiles;		
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
	
	public  List<String> getRemoteExtStopWordDictionarys(){
		List<String> remoteExtStopWordDictFiles = new ArrayList<String>(2);
		String remoteExtStopWordDictCfg = props.getProperty(REMOTE_EXT_STOP);
		if(remoteExtStopWordDictCfg != null){

			String[] filePaths = remoteExtStopWordDictCfg.split(";");
			if(filePaths != null){
				for(String filePath : filePaths){
					if(filePath != null && !"".equals(filePath.trim())){
						remoteExtStopWordDictFiles.add(filePath);

					}
				}
			}
		}		
		return remoteExtStopWordDictFiles;		
	}

    public File getDictRoot() {
        return environment.configFile().toFile();
    }
}
