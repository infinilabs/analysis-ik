package org.wltea.analyzer.dictionary.remote;

import lombok.Getter;

import java.util.stream.Stream;

/**
 * RemoteDictionaryEtymology
 *
 * 远程词典词源
 *
 * @author Qicz
 * @since 2021/7/13 10:24
 */
@Getter
public enum RemoteDictionaryEtymology {

	HTTP("http"),
	REDIS("redis"),
	MYSQL("mysql"),
	DEFAULT(REDIS.etymology);

	String etymology;

	RemoteDictionaryEtymology(String etymology) {
		this.etymology = etymology;
	}

	public static RemoteDictionaryEtymology newEtymology(String etymology) {
		return Stream.of(values()).filter(e -> e.etymology.equals(etymology)).findFirst().orElse(null
		);
	}
}
