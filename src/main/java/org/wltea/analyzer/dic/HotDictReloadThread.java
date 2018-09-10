package org.wltea.analyzer.dic;

/**
 * @author TSN
 * @date 2018/9/7
 * @Description
 */
public class HotDictReloadThread implements Runnable {
    @Override
    public void run() {
        while (true){
            Dictionary.getSingleton().reLoadMainDict();
        }
    }
}
