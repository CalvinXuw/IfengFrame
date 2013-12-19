/**
 * Copyright (c) 2013 ifeng Inc.
 * 
 * @author 		Xu Wei <xuwei@ifeng.com>
 * 
 * @date 2013-4-18
 */
package com.ifeng.util.logging;

import java.util.HashMap;
import java.util.Map;

/**
 * log相关常量。
 */
public final class LogConstants {
    /**
     * Log info level
     */
    public static final String LOGLEVEL_INFO = "info";

    /**
     * Log info int level
     */
    public static final int LOGLEVEL_INFO_INT = 0;
    /**
     * Log debug level
     */
    public static final String LOGLEVEL_DEBUG = "debug";
    /**
     * Log debug int level
     */
    public static final int LOGLEVEL_DEBUG_INT = 1;
    /**
     * Log warning level
     */
    public static final String LOGLEVEL_WARNING = "warning";
    /**
     * Log warning int level
     */
    public static final int LOGLEVEL_WARNING_INT = 2;
    /**
     * Log error level
     */
    public static final String LOGLEVEL_ERROR = "error";
    /**
     * Log error int level
     */
    public static final int LOGLEVEL_ERROR_INT = 3;
    /**
     * Log secure level
     */
    public static final String LOGLEVEL_SECURE = "secure";
    /**
     * Log secure int level
     */
    public static final int LOGLEVEL_SECURE_INT = 4;
    /**
     * Log all level
     */
    public static final String LOGLEVEL_ALL = "all";
    /**
     * Log all int level
     */
    public static final int LOGLEVEL_ALL_INT = 5;
    /**
     * 所有的log等级，map存储，key:value. key是loglevel,value是对应的int值等级
     */
    public static final Map<String, Integer> LOG_LEVELS = new HashMap<String, Integer>();

    static {
        LOG_LEVELS.put(LOGLEVEL_INFO, LOGLEVEL_INFO_INT);
        LOG_LEVELS.put(LOGLEVEL_DEBUG, LOGLEVEL_DEBUG_INT);
        LOG_LEVELS.put(LOGLEVEL_WARNING, LOGLEVEL_WARNING_INT);
        LOG_LEVELS.put(LOGLEVEL_ERROR, LOGLEVEL_ERROR_INT);
        LOG_LEVELS.put(LOGLEVEL_SECURE, LOGLEVEL_SECURE_INT);
        LOG_LEVELS.put(LOGLEVEL_ALL, LOGLEVEL_ALL_INT);
    }

    /**
     * 私有构造函数
     */
    private LogConstants() {

    }
}
