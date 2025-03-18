package com.wearezeta.auto.androidreloaded.common;

import com.wearezeta.auto.androidreloaded.pages.AndroidPage;
import com.wearezeta.auto.common.log.ZetaLogger;

import java.util.concurrent.*;
import java.util.logging.Logger;

public class Pinger {

    public static final Logger log = ZetaLogger.getLog(Pinger.class.getSimpleName());

    // Should not be higher than session timeout on selenium grid
    private static final int PINGER_POLLING_PERIOD = 30;

    private final ScheduledThreadPoolExecutor PING_EXECUTOR;
    private ScheduledFuture<?> RUNNING_PINGER;
    private AndroidTestContext context;
    private final Runnable PINGER = new Runnable() {
        @Override
        public void run() {
            log.info("Pinging driver");
            wakeUpScreen();
        }
    };

    public Pinger(AndroidTestContext context) {
        PING_EXECUTOR = new ScheduledThreadPoolExecutor(1);
        PING_EXECUTOR.setRemoveOnCancelPolicy(true);
        this.context = context;
    }

    public void startPinging() {
        if (RUNNING_PINGER == null) {
            log.info("Scheduling pinger task");
            RUNNING_PINGER = PING_EXECUTOR.scheduleAtFixedRate(PINGER, 0, PINGER_POLLING_PERIOD, TimeUnit.SECONDS);
        } else {
            log.warning("Driver pinger is already running - Please stop the driver pinger before starting it again");
        }
    }

    public void stopPinging() {
        if (RUNNING_PINGER != null) {
            if (!RUNNING_PINGER.cancel(true)) {
                log.warning("Could not stop driver pinger");
            } else {
                log.warning("Stopped driver pinger");
            }
            RUNNING_PINGER = null;
        } else {
            log.warning("No pinger to stop");
        }
    }

    public void wakeUpScreen() {
        AndroidPage.executeShell(context.getDriver(), "input keyevent KEYCODE_WAKEUP");
    }
}
