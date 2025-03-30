package com.wearezeta.auto.common.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class MinimalFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());

        return String.format(
                "%1$tT %2$.4s [%3$s] (%4$.9s) %5$s %n%6$s",
                dateTime,
                record.getLevel().getName(),
                record.getThreadID(),
                stripPackageNameAndTrim(record.getSourceClassName()),
                record.getMessage(),
                stackTraceToString(record)
        );
    }

    private static String stackTraceToString(LogRecord record) {
        final String throwableAsString;
        if (record.getThrown() != null) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            printWriter.println();
            record.getThrown().printStackTrace(printWriter);
            printWriter.close();
            throwableAsString = stringWriter.toString();
        } else {
            throwableAsString = "";
        }
        return throwableAsString;
    }

    private String stripPackageNameAndTrim(final String name) {
        int index = name.lastIndexOf(".");

        if (index != -1) {
            return name.substring(index + 1);
        } else {
            return name;
        }
    }


}
