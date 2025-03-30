package com.wearezeta.auto.androidreloaded.common;

import com.google.common.collect.ImmutableMap;
import com.wearezeta.auto.androidreloaded.pages.AndroidPage;
import com.wearezeta.auto.common.CommonUtils;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.misc.Timedelta;
import com.wire.qa.picklejar.engine.gherkin.model.Scenario;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class AndroidDriverBuilder {

    private static final Logger log = ZetaLogger.getLog(AndroidDriverBuilder.class.getSimpleName());

    private URL hubUrl = null;
    private Capabilities capabilities = null;
    private Scenario scenario;

    public AndroidDriverBuilder withCapabilities(Capabilities capabilities) {
        this.capabilities = capabilities;
        return this;
    }

    public AndroidDriverBuilder addCapabilities(Capabilities capabilities) {
        this.capabilities = this.capabilities.merge(capabilities);
        return this;
    }

    public AndroidDriverBuilder withHub(URL url) {
        this.hubUrl = url;
        return this;
    }

    public AndroidDriverBuilder withScenario(Scenario scenario) {
        //sets the description
        this.scenario = scenario;
        return this;
    }

    public WebDriver build() {
        log.info(String.format("Creating webdriver with capabilities: %s", capabilities));

        if (hubUrl == null) {
            throw new RuntimeException("No hub url specified.");
        }

        if (capabilities == null) {
            throw new RuntimeException("No capabilities specified.");
        }

        final AndroidDriver androidDriver = new AndroidDriver(hubUrl, capabilities);
        androidDriver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        closeSystemDialogs(androidDriver);
        runTestingGallery(androidDriver);

        if (scenario != null && scenario.getDescription().isEmpty()) {
            try {
                // Add device name and manufacturer into scenario description
                StringBuilder descriptionBuilder = new StringBuilder(scenario.getDescription());
                String manufacturer = (String) AndroidPage.executeShell(androidDriver, "getprop ro.product.manufacturer");
                String device = (String) AndroidPage.executeShell(androidDriver, "getprop ro.product.device");
                String model = (String) AndroidPage.executeShell(androidDriver, "getprop ro.product.model");
                String id = (String) AndroidPage.executeShell(androidDriver, "getprop ro.boot.serialno");
                String androidVersion = (String) AndroidPage.executeShell(androidDriver, "getprop ro.build.version.release");

                descriptionBuilder.append(manufacturer.replace("\n",""));
                descriptionBuilder.append(" ");
                descriptionBuilder.append(model.replace("\n",""));
                descriptionBuilder.append(" (");
                descriptionBuilder.append(device.replace("\n",""));
                descriptionBuilder.append(") ");
                descriptionBuilder.append(id.replace("\n",""));
                descriptionBuilder.append(" ");
                descriptionBuilder.append(String.format("[%s] ", androidVersion.replace("\n","")));

                // Add battery state into scenario description
                String dumpsysOutput = (String) AndroidPage.executeShell(androidDriver, "dumpsys battery");
                Scanner scanner = new Scanner(dumpsysOutput);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.contains("level")) {
                        int battery = Integer.parseInt(line.replaceAll("\\D", ""));
                        // write battery state into scenario description
                        descriptionBuilder.append(battery);
                        descriptionBuilder.append("% ");
                        break;
                    }
                }
                scanner.close();
                scenario.setDescription(descriptionBuilder.toString());

                scenario.setDescription(descriptionBuilder.toString());
            } catch (Exception e) {
                log.severe(e.getMessage());
            }
        }

        return androidDriver;
    }

    private String getPackageName() {
        return PackageNameHolder.getPackageName();
    }

    private void closeSystemDialogs(AndroidDriver driver) {
        log.info("Closing system dialogs");
        try {
            driver.executeScript("mobile: shell", ImmutableMap.of("command", "sh", "args", "-c 'am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS'"));
        } catch (WebDriverException wde) {
            log.severe(String.format("the system dialog closing broadcast exited unexpectly. Message: %s", wde.getMessage()));
        }
    }

    public static final String TESTING_GALLERY_APP_ID = "com.wire.testinggallery";

    private void runTestingGallery(AndroidDriver drv) {
        drv.activateApp(TESTING_GALLERY_APP_ID);
        if (CommonUtils.waitUntilTrue(Timedelta.ofSeconds(10), Timedelta.ofMillis(1000),
                () -> drv.getCurrentPackage().equals(TESTING_GALLERY_APP_ID))) {
            while (drv.getCurrentPackage().equals(TESTING_GALLERY_APP_ID)) {
                drv.activateApp(getPackageName());
                Timedelta.ofSeconds(1).sleep();
            }
        }
    }
}
