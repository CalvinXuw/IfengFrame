package com.ifeng.util.logging;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * 修改自 java.util.logging.SimpleFormatter
 */
public class SimpleFormatter extends Formatter {

    /** date object. */
    Date dat = new Date();
    /** date format */
    private static String format = "{0,date} {0,time}";
    /** message format */
    private MessageFormat formatter;

    /**MessageFormat  args */
    private Object[] args = new Object[1];


    /**
     * Format the given LogRecord.
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    public synchronized String format(LogRecord record) {
        StringBuffer sb = new StringBuffer();
        // Minimize memory allocations here.
        dat.setTime(record.getMillis());
        args[0] = dat;
        StringBuffer text = new StringBuffer();
        if (formatter == null) {
            formatter = new MessageFormat(format);
        }
        formatter.format(args, text, null);
        sb.append(text);
        /*    sb.append(" ");
        if (record.getSourceClassName() != null) {  
            sb.append(record.getSourceClassName());
        } else {
            sb.append(record.getLoggerName());
        }
        if (record.getSourceMethodName() != null) { 
            sb.append(" ");
            sb.append(record.getSourceMethodName());
        }*/
        sb.append(" "); // sb.append(lineSeparator);
        String message = formatMessage(record);
        sb.append(record.getLevel().getLocalizedName());
        sb.append(": ");
        sb.append(message);
        sb.append("\n");
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return sb.toString();
    }
}