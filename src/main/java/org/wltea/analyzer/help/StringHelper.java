package org.wltea.analyzer.help;

import java.util.Objects;

/**
 * StringHelper
 *
 * @author Qicz
 * @since 2021/7/9 14:39
 */
public final class StringHelper {

	public static boolean nonBlank(String string) {
		return Objects.nonNull(string) && !"".equals(string.trim());
	}
}
