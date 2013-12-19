package com.ifeng.util;

/**
 * 有些特殊类，proguard被优化后会出现错误。
 * 但是又广泛使用了。
 * 配置 proguard.cfg 比较麻烦。
 * 则继承此类即可。
 * proguard.cfg 配置如下
 *  
    -keep public class com.ifeng.util.NoProGuard
    -keep class * implements com.ifeng.util.NoProGuard {
         *;
    }
 *   
 */
public interface NoProGuard {

}
