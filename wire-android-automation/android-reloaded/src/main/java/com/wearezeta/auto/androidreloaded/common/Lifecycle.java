package com.wearezeta.auto.androidreloaded.common;

import com.wearezeta.auto.androidreloaded.pages.AndroidPage;
import com.wearezeta.auto.androidreloaded.pages.CommonAppPage;
import com.wearezeta.auto.common.Config;
import com.wearezeta.auto.common.TestScreenshotHelper;
import com.wearezeta.auto.common.driver.AppiumLocalServer;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.testiny.ScenarioResultToTestinyTransformer;
import com.wearezeta.auto.common.testiny.TestinySync;
import com.wire.qa.picklejar.engine.TestContext;
import com.wire.qa.picklejar.engine.annotations.AfterEachScenario;
import com.wire.qa.picklejar.engine.annotations.AfterEachStep;
import com.wire.qa.picklejar.engine.annotations.BeforeEachScenario;
import com.wire.qa.picklejar.engine.annotations.BeforeEachStep;
import com.wire.qa.picklejar.engine.gherkin.model.Embeddings;
import com.wire.qa.picklejar.engine.gherkin.model.Scenario;
import com.wire.qa.picklejar.engine.gherkin.model.Step;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.OutputType;

import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Lifecycle {

    private static final Logger log = ZetaLogger.getLog(Lifecycle.class.getSimpleName());
    private static boolean isScreenshotingEnabled = Config.common().isScreenshootingEnabled(Lifecycle.class);
    private static final int MAX_SCREENSHOT_WIDTH = 800;
    private static final int MAX_SCREENSHOT_HEIGHT = 400;

    private final TestScreenshotHelper screenshotHelper = new TestScreenshotHelper();

    private boolean isOnGrid() {
        return Config.current().isOnGrid(this.getClass());
    }

    private static String getUrl() {
        return Config.current().getAppiumUrl(Lifecycle.class);
    }

    private static String getPath() {
        return Config.current().getAndroidApplicationPath(Lifecycle.class);
    }

    private static String getTestingGalleryPath() {
        return String.format("%stesting-gallery-qa-release.apk", Config.current().getTestingGalleryPath(Lifecycle.class));
    }

    @BeforeEachScenario
    public TestContext setUp(Scenario scenario) {
        boolean useSpecialEmail = false;
        if (scenario.hasTag("useSpecialEmail")) {
            useSpecialEmail = true;
        }
        AndroidDriverBuilder driverBuilder = new AndroidDriverBuilder();

        String url = getUrl();
        log.info("URL: " + url);

        final URL serverAddress;
        try {
            serverAddress = new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }

        driverBuilder.withHub(serverAddress);

        final UiAutomator2Options capabilities = new UiAutomator2Options();
        String appPath = getPath();

        // The capability browserName can be used to run a test on a specific node
        String browserName = Config.current().getBrowserName(Lifecycle.class);
        if (!browserName.isEmpty()) {
            log.info(String.format("Only use node '%s'", browserName));
            capabilities.setCapability("browserName", browserName);
        }

        String isCountlyAvailable = Config.current().isCountlyAvailable(Lifecycle.class);
        if (isCountlyAvailable.equals("false")) {
            log.info("Countly is not enabled on this build");
            capabilities.setCapability("isCountlyAvailable", "false");
        } else {
            log.info("Countly is enabled on this build");
            capabilities.setCapability("isCountlyAvailable", "true");
        }

        capabilities.setCapability("platformName", "android");
        capabilities.setCapability("app", appPath);
        capabilities.setCapability("automationName", "uiautomator2");
        capabilities.setCapability("appium:uiautomator2ServerInstallTimeout", 30000);
        capabilities.setCapability("appium:skipUnlock", false);
        capabilities.setCapability("appium:autoGrantPermissions", "true");
        capabilities.setCapability("appium:otherApps", getTestingGalleryPath());
        capabilities.setCapability("appium:settings[enableMultiWindows]", true);

        //TODO: Check if capability below makes it possible to use 'id = okta-sign-in' instead of //*[@resource-id='okta-sign-in']
        //capabilities.setCapability("settings[disableIdLocatorAutocompletion]", true);

        driverBuilder.withCapabilities(capabilities);

        driverBuilder.withScenario(scenario);

        return new AndroidTestContext(scenario, driverBuilder, useSpecialEmail);
    }

    @BeforeEachStep
    public void beforeEachStep(AndroidTestContext context, Scenario scenario, Step step) {

    }

    @AfterEachStep
    public void afterEachStep(AndroidTestContext context, Scenario scenario, Step step) {
        // Make screenshot if test not skipped
        try {
            if (context.isDriverCreated()
                    && !"SKIPPED".equalsIgnoreCase(step.getResult().getStatus())
                    && isScreenshotingEnabled) {
                byte[] screenshot = context.getDriver().getScreenshotAs(OutputType.BYTES);
                // Jenkins 1: screenshotHelper.saveScreenshot(step, scenario, scenario.getCurrentFeatureName(), screenshot);
                Embeddings embedding = new Embeddings(screenshotHelper.encodeToBase64(screenshot, MAX_SCREENSHOT_WIDTH, MAX_SCREENSHOT_HEIGHT), "image/jpeg");
                step.addEmbedding(embedding);
            }
            if (context.hasAdditionalScreenshots()) {
                for (BufferedImage image : context.getAdditionalScreenshots()) {
                    Embeddings embedding = new Embeddings(screenshotHelper.encodeToBase64(image), "image/jpeg");
                    step.addEmbedding(embedding);
                }
                context.clearAdditionalScreenshots();
            }
        } catch (Exception e) {
            log.warning("Could not make a sceenshot: " + e.getMessage());
        }
    }

    @AfterEachScenario
    public void tearDown(AndroidTestContext context, Scenario scenario) {

        try {
            TestinySync.syncExecutedScenarioWithTestiny(scenario.getName(),
                    new ScenarioResultToTestinyTransformer(scenario).transform(),
                    scenario.getTags());
        } catch (Exception e) {
            log.warning(e.getMessage());
        }

        if (scenario.hasTag("sso")) {
            try {
                if (context != null && context.isDriverCreated()) {
                    context.getPage(AndroidPage.class).setClipboard("");
                }
            } catch (Exception e) {
                log.warning(e.getMessage());
            }
        }

        try {
            log.info(String.format("Delete testservice instances for %s", scenario.getName()));
            context.getCommonSteps().cleanUpTestServiceInstances();
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
        try {
            if (!scenario.hasTag("maintenance")) {
                log.info("Delete application in okta");
                if (context != null) {
                    context.getCommonSteps().cleanUpOkta();
                }
            }
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
        try {
            if (context != null
                && context.isWiFiStateResetNeeded()
                && context.isDriverCreated()) {
                    context.getPage(CommonAppPage.class).enableWifi();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Driver
        try {
            log.fine("Closing webdriver");
            if (context != null && context.isDriverCreated()) {
                context.getDriver().quit();
            }
        } catch (Exception e) {
            log.warning("Closing webdriver failed: " + e.getMessage());
            e.printStackTrace();
        }
        // Federation
        try {
            for (String fromBackend: context.getCommonSteps().defederatedBackends.keySet()) {
                log.info("Repair defederation");
                context.getCommonSteps().federateBackends(fromBackend,
                        context.getCommonSteps().defederatedBackends.get(fromBackend));
            }
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
        try {
            for (String backendName: context.getCommonSteps().touchedFederator) {
                log.info("Turn federator back on");
                context.getCommonSteps().turnFederatorInBackendOn(backendName);
            }
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
        try {
            for (String backendName: context.getCommonSteps().touchedBrig) {
                log.info("Turn brig back on");
                context.getCommonSteps().turnBrigInBackendOn(backendName);
            }
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
        try {
            for (String backendName: context.getCommonSteps().touchedGalley) {
                log.info("Turn galley back on");
                context.getCommonSteps().turnGalleyInBackendOn(backendName);
            }
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
        try {
            for (String backendName: context.getCommonSteps().touchedIngress) {
                log.info("Turn ingress back on");
                context.getCommonSteps().turnIngressInBackendOn(backendName);
            }
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
        try {
            for (String backendName: context.getCommonSteps().touchedSFT) {
                log.info("Turn SFT back on");
                context.getCommonSteps().turnSFTInBackendOn(backendName);
            }
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
        // Local appium
        try {
            if (!isOnGrid()) {
                log.info("Stopping appium");
                AppiumLocalServer.stop();
            }
        } catch (Exception e) {
            log.warning("Stopping appium failed: " + e.getMessage());
        }
        try {
            log.fine("Cleaning up calling instances");
            context.getCallingManager().cleanup();
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
        try {
            log.info("Cleanup backends");
            if (!scenario.hasTag("maintenance")) {
                if (context != null) {
                    context.getCommonSteps().cleanUpBackends();
                }
            }
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
    }

}
