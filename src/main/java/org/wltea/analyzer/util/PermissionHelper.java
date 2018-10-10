package org.wltea.analyzer.util;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.elasticsearch.SpecialPermission;

/**
 * @author hanqing.zhq@alibaba-inc.com
 * @date 2018/4/18
 */
public class PermissionHelper {

    /**
     * Executes a {@link PrivilegedExceptionAction} with privileges enabled.
     */

    public static  <T> T doPrivileged(PrivilegedExceptionAction<T> operation) throws IOException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SpecialPermission());
        }
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<T>) operation::run);
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
    }
}
