/**
 * Copyright (c) 2013 ifeng Inc.
 * 
 * @author 		Xu Wei <xuwei@ifeng.com>
 * 
 * @date 2013-4-18
 */
package com.ifeng.util.logging;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;

import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

/**
 * Log Utils方法
 */
public final class Utils {
    /**
     * 私有构造函数
     */
    private Utils() {

    }
    /**
     * Handy function to get a loggable stack trace from a Throwable
     * 
     * @param tr
     *            An exception to log
     * 
     * @return tr StackTraceString.
     */
    public static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        return sw.toString();
    }
    /**
     * 获取当前进程名。
     * 
     * @return 当前进程名。
     */
    public static String getLogFileName() {
        int pid = Process.myPid();
        String name = getProcessNameForPid(pid);
        if (TextUtils.isEmpty(name)) {
            name = "ifengFileLog";
        }
        name = name.replace(':', '_');

        return name;
    }

    /**
     * 根据 进程 pid 获取进程名。
     * 
     * @param pid
     *            process id
     * @return 进程名
     */
    private static String getProcessNameForPid(int pid) {

        String cmdlinePath = "/proc/" + pid + "/cmdline"; // cmdline file path
        String statusPath = "/proc/" + pid + "/status"; // proc status file path

        String name = ""; // 进程名

        try {
            // 首先根据 cmdline 获取进程名
            File file = new File(cmdlinePath);
            BufferedReader bf = new BufferedReader(new FileReader(file));
            String line = null;
            line = bf.readLine();

            if (!TextUtils.isEmpty(line)) {
                // 从 cmdline中获取进程名
                int index = line.indexOf(0); // cmdline 为 c语言格式字符串，后边为 0
                name = line.substring(0, index);
            } else {
                // 从proc status获取进程名
                file = new File(statusPath);
                bf = new BufferedReader(new FileReader(file));
                line = bf.readLine();

                while (line != null) {
                    if (line.startsWith("Name:")) {
                        int index = line.indexOf("\t"); // 比如
                                                        // "Name:\tcom.baidu.appsearch"
                        if (index >= 0) {
                            name = line.substring(index + 1);
                        }

                        break;
                    }

                    line = bf.readLine();
                }
            }
            bf.close();
        } catch (Exception e) {
            Log.e(Utils.class.getSimpleName(), "error:" + e.getMessage());
        }

        return name;
    }
}
