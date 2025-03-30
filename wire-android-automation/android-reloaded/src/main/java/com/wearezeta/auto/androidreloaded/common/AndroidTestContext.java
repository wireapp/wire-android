package com.wearezeta.auto.androidreloaded.common;

import com.wearezeta.auto.common.CommonTestContext;
import com.wearezeta.auto.common.backend.BackendConnections;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.misc.Timedelta;
import com.wearezeta.auto.common.usrmgmt.ClientUser;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import com.wire.qa.picklejar.engine.gherkin.model.Scenario;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import org.json.JSONObject;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import static com.wearezeta.auto.common.CommonSteps.NO_EXPIRATION;

public class AndroidTestContext extends CommonTestContext {

    private static final Logger log = ZetaLogger.getLog(AndroidTestContext.class.getSimpleName());

    public boolean isWiFiStateResetNeeded = false;

    private ClientUser userToRegister = null;
    private final Scenario scenario;
    private final AndroidDriverBuilder driverBuilder;
    private WebDriver driver;
    private final Pinger pinger;
    private final String testname;
    private String currentDeviceId = null;
    private String messageToRemember = null;
    private Future<String> rememberedMail;
    private String rememberedCertificate = null;
    private String rememberedInviteLink = null;
    private Future<String> activationMessage = null;

    private final Map<ClientUser, Map<String, Timedelta>> SELF_DELETING_MESSAGE_TIMEOUTS_MAP = new HashMap<>();

    public AndroidTestContext(Scenario scenario, AndroidDriverBuilder androidDriverBuilder, boolean useSpecialEmail) {
        super(useSpecialEmail);
        this.scenario = scenario;
        this.driverBuilder = androidDriverBuilder;
        this.pinger = new Pinger(this);
        this.testname = scenario.getName();
    }

    public String getTestname() {
        return testname;
    }

    public ClientUser getUserToRegister() {
        return userToRegister;
    }

    public void setUserToRegister(ClientUser userToRegister) {
        this.userToRegister = userToRegister;
    }

    public String getCurrentDeviceId() {
        return this.currentDeviceId;
    }

    public void setCurrentDeviceId(String deviceId) {
        this.currentDeviceId = deviceId;
    }

    public String getLongRandomGeneratedText() {
        return this.messageToRemember;
    }

    public void setLongRandomGeneratedText(String messageToRemember) {
        this.messageToRemember = messageToRemember;
    }

    public Future<String> getRememberedMail() {
        return rememberedMail;
    }

    public void setRememberedMail(Future<String> verificationMessage) {
        this.rememberedMail = verificationMessage;
    }

    public String getRememberedCertificate() {
        return rememberedCertificate;
    }

    public void setRememberedCertificate(String remembered) {
        this.rememberedCertificate = remembered;
    }

    public String getRememberedInviteLink() {
        return rememberedInviteLink;
    }

    public void setRememberedInviteLink(String rememberedInviteLink) {
        this.rememberedInviteLink = rememberedInviteLink;
    }

    public void setActivationMessage(Future<String> message) {
        this.activationMessage = message;
    }

    public Future<String> getActivationMessage() {
        return activationMessage;
    }

    public boolean isWiFiStateResetNeeded() {
        return this.isWiFiStateResetNeeded;
    }

    public AndroidDriver getDriver() {
        if (!isDriverCreated()) {
            log.info("Driver is not created yet. Using driver builder...");
            this.driver = driverBuilder.build();
        }
        return (AndroidDriver) this.driver;
    }

    public boolean isDriverCreated() {
        return driver != null;
    }

    public void addCapabilities(Capabilities capabilities) {
        if (!isDriverCreated()) {
            driverBuilder.addCapabilities(capabilities);
        } else {
            throw new RuntimeException("Driver was already created. Cannot add capabilities anymore.");
        }
    }

    public Scenario getScenario() {
        return scenario;
    }

    public <T> T getPage(Class<T> pageClass) {
        log.info("Page: " + pageClass.getSimpleName());
        try {
            final Constructor<?> constructor = pageClass.getConstructor(WebDriver.class);
            T instance = pageClass.cast(constructor.newInstance(getDriver()));
            PageFactory.initElements(new AppiumFieldDecorator(this.driver), instance);
            return instance;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Could not instantiate page " + pageClass, e);
        }
    }

    public void startPinging() {
        if (isDriverCreated()) {
            pinger.startPinging();
        }
    }

    public void stopPinging() {
        if (isDriverCreated()) {
            pinger.stopPinging();
        }
    }

    public Timedelta getSelfDeletingMessageTimeout(String userAlias, String conversationName) {
        final ClientUser user = getUsersManager().findUserByNameOrNameAlias(userAlias);

        // only team users support enforced self-deleting messages
        if (user.getTeamId() != null) {
            JSONObject selfDeletingMessagesSettings = BackendConnections.get(user).getSelfDeletingMessagesSettings(user);

            if (selfDeletingMessagesSettings.getString("status").equals("enabled")) {
                int timeoutInSeconds = selfDeletingMessagesSettings.getJSONObject("config").getInt("enforcedTimeoutSeconds");
                if (timeoutInSeconds != 0) {
                    // timeout value is enforced in team settings
                    return Timedelta.ofSeconds(timeoutInSeconds);
                }
            } else {
                // timeout is disabled
                return NO_EXPIRATION;
            }
        }

        // Personal user or team user without set enforced self-deleting message setting

        // follow conversation settings if there is any
        conversationName = getUsersManager().replaceAliasesOccurrences(conversationName,
                ClientUsersManager.FindBy.NAME_ALIAS);
        int conversationMessageTimer = getCommonSteps().getConversationMessageTimer(user, conversationName);
        if (conversationMessageTimer > 0) {
            return Timedelta.ofMillis(conversationMessageTimer);
        }

        // otherwise check for local/client-side self-deleting message timeout
        return getLocalSelfDeletingMessageTimeout(userAlias, conversationName);
    }

    public void setLocalSelfDeletingMessageTimeout(String userAlias, String conversationName, Timedelta timeout) {
        final ClientUser user = getUsersManager().findUserByNameOrNameAlias(userAlias);
        conversationName = getUsersManager().replaceAliasesOccurrences(conversationName,
                ClientUsersManager.FindBy.NAME_ALIAS);
        final String conversationId = BackendConnections.get(user).getConversationByName(user, conversationName).getId();
        if (SELF_DELETING_MESSAGE_TIMEOUTS_MAP.containsKey(user)) {
            final Map<String, Timedelta> dstMap = SELF_DELETING_MESSAGE_TIMEOUTS_MAP.get(user);
            dstMap.put(conversationId, timeout);
        } else {
            final Map<String, Timedelta> dstMap = new HashMap<>();
            dstMap.put(conversationId, timeout);
            SELF_DELETING_MESSAGE_TIMEOUTS_MAP.put(user, dstMap);
        }
    }

    private Timedelta getLocalSelfDeletingMessageTimeout(String userAlias, String conversationName) {
        final ClientUser user = getUsersManager().findUserByNameOrNameAlias(userAlias);
        conversationName = getUsersManager().replaceAliasesOccurrences(conversationName,
                ClientUsersManager.FindBy.NAME_ALIAS);
        final String conversationId = BackendConnections.get(user).getConversationByName(user, conversationName).getId();
        if (SELF_DELETING_MESSAGE_TIMEOUTS_MAP.containsKey(user) && SELF_DELETING_MESSAGE_TIMEOUTS_MAP.get(user).containsKey(conversationId)) {
            return SELF_DELETING_MESSAGE_TIMEOUTS_MAP.get(user).get(conversationId);
        }
        return NO_EXPIRATION;
    }
}
