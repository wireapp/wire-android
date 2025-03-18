package com.wearezeta.auto.androidreloaded.pages;

import com.google.common.collect.ImmutableMap;
import com.wearezeta.auto.androidreloaded.common.PackageNameHolder;
import com.wearezeta.auto.common.Config;
import com.wearezeta.auto.common.imagecomparator.QRCode;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.misc.Timedelta;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.appmanagement.AndroidInstallApplicationOptions;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.touch.offset.ElementOption;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebDriver;
import io.appium.java_client.android.AndroidTouchAction;
import org.openqa.selenium.support.ui.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.wearezeta.auto.common.CommonUtils.waitUntilTrue;
import static io.appium.java_client.touch.TapOptions.tapOptions;
import static io.appium.java_client.touch.WaitOptions.waitOptions;
import static io.appium.java_client.touch.offset.PointOption.point;

public class AndroidPage {

    private final WebDriver driver;

    public AndroidPage(WebDriver driver) {
        this.driver = driver;
    }

    protected AndroidDriver getDriver() {
        return (AndroidDriver) this.driver;
    }

    private DefaultArtifactVersion androidOSVersion;

    public DefaultArtifactVersion getOSVersion() {
        if (this.androidOSVersion == null) {
            this.androidOSVersion = new DefaultArtifactVersion((String) executeShell(getDriver(), "getprop ro.build.version.release"));
        }
        return this.androidOSVersion;
    }

    public static final DefaultArtifactVersion VERSION_10_0 = new DefaultArtifactVersion("10.0");
    public static final DefaultArtifactVersion VERSION_11_0 = new DefaultArtifactVersion("11.0");

    protected static final Logger log = ZetaLogger.getLog(AndroidPage.class.getSimpleName());

    public static int getDefaultLookupTimeoutSeconds() {
        return Integer.parseInt(Config.current().getDriverTimeout(AndroidPage.class));
    }

    public String getPhoneModelType() {
        return (String) executeShell(getDriver(), "getprop ro.product.manufacturer");
    }

    private final Timedelta ELEMENT_VERIFICATION_DELAY = Timedelta.ofMillis(500);

    public void minimizeApp() {
        getDriver().pressKey(new KeyEvent(AndroidKey.HOME));
    }

    public void terminateApp(String packageId) {
        getDriver().terminateApp(packageId);
    }

    public void activateApp(String packageId) {
        getDriver().activateApp(packageId);
    }

    public void installApp(String path, boolean grantPermissions, boolean force) {
        AndroidInstallApplicationOptions applicationOptions = new AndroidInstallApplicationOptions();
        if (grantPermissions) {
            applicationOptions.withGrantPermissionsEnabled();
        }
        if (force) {
            applicationOptions.withReplaceEnabled();
        }
        getDriver().installApp(path, applicationOptions);
    }

    public void uninstallApp(String packageId) {
        getDriver().removeApp(packageId);
    }

    public void openURL(String url) {
        getDriver().get(url);
    }

    public void navigateBack() {
        getDriver().navigate().back();
    }

    public void hideKeyboard() {
        this.getDriver().hideKeyboard();
    }

    public String getClipboard() {
        return getDriver().getClipboardText();
    }

    public void scroll(int widthEndPercent, int heightEndPercent) {
        int startX = getDriver().manage().window().getSize().getWidth() / 2;
        int startY = getDriver().manage().window().getSize().getHeight() / 2;
        int endX = getDriver().manage().window().getSize().getHeight() * widthEndPercent;
        int endY = getDriver().manage().window().getSize().getHeight() * heightEndPercent;
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence scroll = new Sequence(finger, 0);
        scroll.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
        scroll.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        scroll.addAction(finger.createPointerMove(Duration.ofMillis(600), PointerInput.Origin.viewport(), endX, endY));
        scroll.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        getDriver().perform(List.of(scroll));
    }

    public void longTap(WebElement el) {
        Point sourceLocation = el.getLocation();
        Dimension sourceSize = el.getSize();
        int centerX = sourceLocation.getX() + sourceSize.getWidth() / 2;
        int centerY = sourceLocation.getY() + sourceSize.getHeight() / 2;
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), centerX, centerY));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(finger.createPointerMove(Duration.ofMillis(1000), PointerInput.Origin.viewport(), centerX, centerY));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        getDriver().perform(List.of(tap));
    }

    public void setClipboard(String text) {
        getDriver().setClipboardText(text);
    }

    public static Object executeShell(RemoteWebDriver driver, String command) {
        log.info("Executing mobile:shell command: " + command);
        String[] words = command.split(" ");
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList(words).subList(1, words.length));
        Map<String, Object> commandMap = ImmutableMap.of(
                "command", words[0],
                "args", args
        );
        return driver.executeScript("mobile:shell", commandMap);
    }

    protected void longTapWithActionsAPI(WebElement el, Duration duration) throws InterruptedException {
        Actions actions = new Actions(getDriver());
        actions.moveToElement(el).clickAndHold().pause(duration).release().perform();
    }

    protected void longTapWithActionsAPI(WebElement el) throws InterruptedException {
        longTapWithActionsAPI(el, Duration.ofSeconds(2));
    }

    protected static Timedelta getDefaultLookupTimeout() {
        return Timedelta.ofSeconds(getDefaultLookupTimeoutSeconds());
    }

    public static String executeShell(RemoteWebDriver driver, String command, List<String> arguments) {
        log.info("Executing mobile:shell command: " + command);
        return (String) driver.executeScript("mobile: shell", ImmutableMap.of(
                "command", command,
                "args", arguments
        ));
    }

    public static boolean isElementPresentAndDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean waitUntilElementVisible(WebElement element, long timeOut) {
        new WebDriverWait(getDriver(), Duration.ofSeconds(timeOut))
                .ignoring(StaleElementReferenceException.class)
                .ignoring(NoSuchElementException.class)
                .until(ExpectedConditions.visibilityOf(element));
        return element.isDisplayed();
    }

    public boolean waitUntilElementVisible(WebElement element) {
        return waitUntilElementVisible(element, getDefaultLookupTimeoutSeconds());
    }

    public boolean waitUntilElementInvisible(WebElement element) {
        return waitUntilElementInvisible(element, Duration.ofSeconds(getDefaultLookupTimeoutSeconds()));
    }

    protected boolean isElementInvisible(WebElement webElement, Timedelta timeout) {
        try {
            // getDriver() is to make sure the driver has been created
            // For checking invisibility the implicit wait needs to be deactivated
            getDriver().manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
            return waitUntilTrue(timeout, ELEMENT_VERIFICATION_DELAY, () -> {
                try {
                    if (webElement == null || !isElementPresentAndDisplayed(webElement)) {
                        return true;
                    }
                } catch (WebDriverException e) {
                    return true;
                }
                log.warning(String.format("The element '%s' is still visible", webElement));
                return false;
            });
        } finally {
            int wait = Integer.parseInt(Config.current().getDriverTimeout(AndroidPage.class));
            driver.manage().timeouts().implicitlyWait(wait, TimeUnit.SECONDS);
        }
    }

    public boolean waitUntilElementInvisible(WebElement element, Duration timeout) {
        try {
            // For checking invisibility the implicit wait needs to be deactivated
            driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
            Wait<? extends RemoteWebDriver> wait = new FluentWait<>(getDriver())
                    .withTimeout(timeout)
                    .pollingEvery(Duration.ofMillis(500))
                    .ignoring(StaleElementReferenceException.class);
            // Unfortunately ExpectedConditions.invisibilityOf() cannot be used here, because selenium does not return
            // true on a NoSuchElementException when using an element (for invisibilityOfElementLocated it works)
            return wait.until((drv) -> {
                try {
                    return !element.isDisplayed();
                } catch (NoSuchElementException e) {
                    return true;
                }
            });
        } catch (TimeoutException e) {
            log.warning(String.format("The element '%s' is still visible", element));
            return false;
        } finally {
            int wait = Integer.parseInt(Config.current().getDriverTimeout(AndroidPage.class));
            getDriver().manage().timeouts().implicitlyWait(wait, TimeUnit.SECONDS);
        }
    }

    public boolean waitUntilElementClickable(WebElement element) {
        try {
            new WebDriverWait(getDriver(), Duration.ofSeconds(getDefaultLookupTimeoutSeconds()))
                    .ignoring(StaleElementReferenceException.class)
                    .until(ExpectedConditions.elementToBeClickable(element));
            Wait<? extends RemoteWebDriver> waitStopped = new FluentWait<>(getDriver())
                    .withTimeout(Duration.ofSeconds(getDefaultLookupTimeoutSeconds()))
                    .pollingEvery(Duration.ofMillis(100))
                    .ignoring(NoSuchElementException.class)
                    .ignoring(StaleElementReferenceException.class);
            waitStopped.until(elementStoppedMoving(element));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private ExpectedCondition<WebElement> elementStoppedMoving(final WebElement element) {
        return new ExpectedCondition<WebElement>() {

            private Point location = null;

            public WebElement apply(WebDriver driver) {
                if (element.isDisplayed()) {
                    Point currentLocation = element.getLocation();
                    if (currentLocation.equals(location)) {
                        return element;
                    }
                    location = currentLocation;
                }
                return null;
            }
            public String toString() {
                return "steadiness of element " + element;
            }
        };
    }

    public boolean waitUntilLocatorIsDisplayed(final By by, Duration timeout) {
        Wait<? extends RemoteWebDriver> wait = new FluentWait<>(getDriver())
                .withTimeout(timeout)
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class)
                .ignoring(InvalidElementStateException.class);
        try {
            return wait.until(drv -> {
                final List<WebElement> foundElements = drv.findElements(by);
                if (foundElements.size() > 0) {
                    for (WebElement element : foundElements) {
                        if (isElementPresentAndDisplayed(element)) {
                            return true;
                        } else {
                            log.info("Element is not in viewport: " + element);
                        }
                    }
                }
                return false;
            });
        } catch (TimeoutException e) {
            return false;
        }
    }

    protected boolean isLocatorInvisible(By locator) {
        return this.isLocatorInvisible(locator, getDefaultLookupTimeout());
    }

    protected boolean isLocatorInvisible(By locator, Timedelta timeout) {
        try {
            // getDriver() is to make sure the driver has been created
            // For checking invisibility the implicit wait needs to be deactivated
            getDriver().manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
            return waitUntilTrue(timeout, ELEMENT_VERIFICATION_DELAY, () -> {
                try {
                    final WebElement el = getDriver().findElement(locator);
                    if (el == null || !isElementPresentAndDisplayed(el)) {
                        return true;
                    }
                } catch (WebDriverException e) {
                    return true;
                }
                log.warning(String.format("The element '%s' is still visible", locator));
                return false;
            });
        } finally {
            int wait = Integer.parseInt(Config.current().getDriverTimeout(AndroidPage.class));
            getDriver().manage().timeouts().implicitlyWait(wait, TimeUnit.SECONDS);
        }
    }

    public Optional<WebElement> scrollUntilElementVisible(By locator, int widthEndPercent, int heightEndPercent) {
        hideKeyboard();
        int nScrolls = 0;
        while (nScrolls < 3) {
            if (waitUntilLocatorIsDisplayed(locator, Duration.ofSeconds(1))) {
                WebElement menuElement = getDriver().findElement(locator);
                if (isElementPresentAndDisplayed(menuElement)) {
                    return Optional.of(menuElement);
                }
            }
            scroll(widthEndPercent, heightEndPercent);
            nScrolls++;
        }
        return Optional.empty();
    }

    public BufferedImage getElementScreenshot(WebElement element) {
        try {
            final byte[] srcImage = element.getScreenshotAs(OutputType.BYTES);
            return ImageIO.read(new ByteArrayInputStream(srcImage));
        } catch (Exception e) {
            throw new RuntimeException("Could not take screenshot of element: " + e.getMessage());
        }
    }

    private java.awt.Rectangle seleniumRectToAWTRect(org.openqa.selenium.Rectangle seleiumRect) {
        return new java.awt.Rectangle(seleiumRect.x, seleiumRect.y, seleiumRect.width, seleiumRect.height);
    }

    public List<String> waitUntilElementContainsQRCode(final WebElement element) {
        try {
            Wait<? extends RemoteWebDriver> wait = new FluentWait<>(getDriver())
                    .withTimeout(Duration.ofSeconds(getDefaultLookupTimeoutSeconds()))
                    .pollingEvery(Duration.ofSeconds(1))
                    .ignoring(NoSuchElementException.class)
                    .ignoring(StaleElementReferenceException.class);
            return wait.until(elementContainsQRCode(element));
        } catch (TimeoutException e) {
            return Collections.emptyList();
        }
    }

    private ExpectedCondition<List<String>> elementContainsQRCode(final WebElement element) {
        return driver -> {
            BufferedImage actualImage;
            try {
                actualImage = getElementScreenshot(element);
            } catch (Exception e) {
                log.info(e.getMessage());
                return null;
            }
            List<String> codes;
            try {
                codes = QRCode.readMultipleCodes(actualImage);
            } catch (com.google.zxing.NotFoundException e) {
                log.info("Element contains no QR code");
                return null;
            }
            if (codes.isEmpty()) {
                return null;
            } else {
                return codes;
            }
        };
    }

    public void pushFile(File file) {
        String remotePath = "/sdcard/Download/" + file.getName();
        try {
            getDriver().pushFile(remotePath, file);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not push file %s to %s: %s",
                    file.getName(),
                    remotePath,
                    e.getMessage()));
        }
    }
}