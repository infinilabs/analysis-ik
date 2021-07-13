package org.wltea.analyzer.dictionary;

import lombok.Getter;

/**
 * DictionaryType
 *
 * @author Qicz
 * @since 2021/7/12 15:36
 */
@Getter
public enum DictionaryType {
	MAIN_WORDS(1, "main-words"),
	STOP_WORDS(2, "stop-words");

	Integer type;
	String dictName;

	DictionaryType(Integer type, String dictName) {
		this.type = type;
		this.dictName = dictName;
	}
}
