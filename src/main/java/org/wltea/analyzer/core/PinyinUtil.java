package org.wltea.analyzer.core;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;

import java.util.*;

/**
 * Created by 刘一波 on 16/1/20.
 * E-Mail:yibo.liu@tqmall.com
 */
public class PinyinUtil {

    //匹配一个中文和非中文字符，可用于替换，替换后只留下多个中文词组
    public static String regexpSingleWord = "[^\\u4e00-\\u9fa5a]|(?<![\\u4e00-\\u9fa5a])[\\u4e00-\\u9fa5a](?![\\u4e00-\\u9fa5a])";

    private static HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();

    static {
        defaultFormat.setCaseType(HanyuPinyinCaseType.UPPERCASE);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        defaultFormat.setVCharType(HanyuPinyinVCharType.WITH_V);
    }

    /**
     * 只转换单词的，也就是两个汉语词以上，其它的过滤掉
     *
     * @param chines
     * @return
     */
    public static String convertToFirstSpellOnlyWord(String chines) {
        chines = chines.replaceAll(regexpSingleWord, " ").replace("\\s+", " ");
        return toString(convertToSpellPolyphonic(chines, true));
    }

    /**
     * 汉字转换位汉语拼音，英文字符不变
     * 返回多音拼音数组
     *
     * @param chines 汉字
     * @return 拼音
     */
    public static String[] convertToSpellPolyphonic(String chines, boolean isFirstSpell) {
        if (chines == null || chines.length() == 0) return null;
        String[] postArr = convertToSpellPolyphonic(chines.substring(1), isFirstSpell);
        List<String> pList = new LinkedList<String>();
        char nameChar = chines.charAt(0);
        if (nameChar > 128) {
            try {
                String[] pinyinArr = PinyinHelper.toHanyuPinyinStringArray(nameChar, defaultFormat);
                if (pinyinArr != null && pinyinArr.length > 0) {
                    if (isFirstSpell) {
                        for (String pinyin : pinyinArr) {
                            if (pinyin.length() > 0) {
                                pList.add(String.valueOf(pinyin.charAt(0)));
                            }
                        }
                    } else {
                        Collections.addAll(pList, pinyinArr);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            pList.add(String.valueOf(nameChar).toUpperCase());
        }
        if (postArr == null && pList.isEmpty()) {
            return null;
        }
        if (postArr != null && !pList.isEmpty()) {
            Set<String> pinyinSet = new HashSet<String>();
            for (String py : pList) {
                for (String post : postArr) {
                    pinyinSet.add(py + post);
                }
            }
            postArr = new String[pinyinSet.size()];
            pinyinSet.toArray(postArr);
        } else if (postArr == null) {
            postArr = new String[pList.size()];
            pList.toArray(postArr);
        }
        return postArr;
    }

    public static String toString(Object[] a) {
        if (a == null)
            return "null";

        int iMax = a.length - 1;
        if (iMax == -1)
            return "";

        StringBuilder b = new StringBuilder();
        for (int i = 0; ; i++) {
            b.append(String.valueOf(a[i]));
            if (i == iMax)
                return b.toString();
            b.append(" ");
        }
    }

    public static void main(String[] args) {
        System.out.println(toString(convertToSpellPolyphonic("刘一波最", false)));
        System.out.println(toString(convertToSpellPolyphonic("刘一波最", true)));
    }
}
