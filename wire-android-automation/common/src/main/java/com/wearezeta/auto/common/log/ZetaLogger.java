package com.wearezeta.auto.common.log;

import java.util.logging.Logger;

public class ZetaLogger {

    public static synchronized Logger getLog(String className) {
        Logger.getLogger("").getHandlers()[0].setFormatter(new MinimalFormatter());
        return Logger.getLogger(className);
    }
}
