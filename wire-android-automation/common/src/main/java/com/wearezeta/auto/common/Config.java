package com.wearezeta.auto.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static com.wearezeta.auto.common.CommonUtils.isDesktop;
import static com.wearezeta.auto.common.CommonUtils.isRunningOnJenkinsNode;

public class Config {
    private static Config currentInstance = null;
    private static Config commonInstance = null;

    private final static Map<String, Properties> cache = new ConcurrentHashMap<>();

    private static final String CURRENT_CONFIG = "Configuration.properties";
    private static final String COMMON_CONFIG = "CommonConfiguration.properties";

    private final String configName;

    private Config(String configName) {
        this.configName = configName;
    }

    public static synchronized Config current() {
        if (currentInstance == null) {
            currentInstance = new Config(CURRENT_CONFIG);
        }
        return currentInstance;
    }

    public static synchronized Config common() {
        if (commonInstance == null) {
            commonInstance = new Config(COMMON_CONFIG);
        }
        return commonInstance;
    }

    private String getValue(Class<?> c, String key, String resourcePath) {
        return getOptionalValue(c, key, resourcePath)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("There is no '%s' key in '%s' config", key, configName)
                ));
    }

    private Optional<String> getOptionalValue(Class<?> c, String key, String resourcePath) {
        final Properties cachedProps = cache.computeIfAbsent(resourcePath, (k) -> {
            try (InputStream configFileStream = c.getClassLoader().getResourceAsStream(resourcePath)) {
                final Properties p = new Properties();
                p.load(configFileStream);
                return p;
            } catch (IOException e) {
                throw new IllegalArgumentException(String.format("Configuration file '%s' cannot be loaded",
                        resourcePath), e);
            }
        });
        return cachedProps.containsKey(key) ? Optional.of((String) cachedProps.get(key)) : Optional.empty();
    }

    private Optional<Integer> getOptionalInteger(Class<?> c, String key, String resourcePath) {
        final Properties cachedProps = cache.computeIfAbsent(resourcePath, (k) -> {
            try (InputStream configFileStream = c.getClassLoader().getResourceAsStream(resourcePath)) {
                final Properties p = new Properties();
                p.load(configFileStream);
                return p;
            } catch (IOException e) {
                throw new IllegalArgumentException(String.format("Configuration file '%s' cannot be loaded",
                        resourcePath), e);
            }
        });
        return cachedProps.containsKey(key) ? Optional.of((Integer) cachedProps.get(key)) : Optional.empty();
    }

    private String getValue(Class<?> c, String key) {
        return getValue(c, key, configName);
    }

    private Optional<String> getOptionalValue(Class<?> c, String key) {
        return getOptionalValue(c, key, configName);
    }

    public String getBackendType(Class<?> c) {
        return getValue(c, "backendType");
    }

    public String getBackendConnections(Class<?> c) {
        return getValue(c, "backendConnections");
    }

    public String getCustomBackendUrl(Class<?> c) {
        return getValue(c, "customBackendUrl");
    }

    public String getCustomBackendDomain(Class<?> c) {
        return getValue(c, "customBackendDomain");
    }

    public String getCustomBackendWebsocket(Class<?> c) {
        return getValue(c, "customBackendWebsocket");
    }

    public String getCustomBackendBasicAuth(Class<?> c) {
        return getValue(c, "customBackendBasicAuth");
    }

    public boolean isFeatureCheckConsentEnabled(Class<?> c, String backendType) {
        switch (backendType) {
            case "qa-column-1":
            case "qa-column-3":
            case "bund-qa-column-1":
            case "bund-qa-column-3":
            case "bund-next-column-1":
            case "bund-next-column-3":
            case "kube":
            case "calling-dev":
            case "jct-66-a":
            case "mobtown-test":
            case "mobtown-red":
            case "mobtown-ernie":
                return false;
            default:
                return true;
        }
    }

    public boolean isFeaturePaymentsEnabled(Class<?> c) {
        final String teamAdminDomain = getTeamAdminDomain(c);
        switch (teamAdminDomain.toLowerCase()) {
            case "teams.wire.com":
            case "wire-teams-staging.zinfra.io":
            case "wire-teams-dev.zinfra.io":
                return true;
            default:
                throw new IllegalArgumentException(String.format("Not recognized team admin domain '%s'", teamAdminDomain));
        }
    }

    public boolean isFeatureServicesEnabled(Class<?> c) {
        final String teamAdminDomain = getTeamAdminDomain(c);
        switch (teamAdminDomain.toLowerCase()) {
            case "teams.wire.com":
            case "wire-teams-staging.zinfra.io":
            case "wire-teams-dev.zinfra.io":
                return true;
            default:
                throw new IllegalArgumentException(String.format("Not recognized team admin domain '%s'", teamAdminDomain));
        }
    }

    public boolean isFeatureLegalHoldEnabled(Class<?> c) {
        final String teamAdminDomain = getTeamAdminDomain(c);
        switch (teamAdminDomain.toLowerCase()) {
            case "wire-teams-dev.zinfra.io":
            case "wire-teams-staging.zinfra.io":
                return true;
            case "teams.wire.com":
                return false;
            default:
                throw new IllegalArgumentException(String.format("Not recognized team admin domain '%s'", teamAdminDomain));
        }
    }

    public boolean isFeatureSSOConfigurationEnabled(Class<?> c) {
        final String teamAdminDomain = getTeamAdminDomain(c);
        switch (teamAdminDomain.toLowerCase()) {
            case "wire-teams-dev.zinfra.io":
            case "teams.wire.com":
            case "wire-teams-staging.zinfra.io":
                return true;
            default:
                throw new IllegalArgumentException(String.format("Not recognized team admin domain '%s'", teamAdminDomain));
        }
    }

    public boolean isDigitalSignatureEnabled(Class<?> c) {
        final String teamAdminDomain = getTeamAdminDomain(c);
        switch (teamAdminDomain.toLowerCase()) {
            case "wire-teams-dev.zinfra.io":
            case "teams.wire.com":
            case "wire-teams-staging.zinfra.io":
                return false;
            default:
                throw new IllegalArgumentException(String.format("Not recognized team admin domain '%s'", teamAdminDomain));
        }
    }

    public boolean isDelegatedAdminEnabled(Class<?> c) {
        final String teamAdminDomain = getTeamAdminDomain(c);
        switch (teamAdminDomain.toLowerCase()) {
            case "wire-teams-dev.zinfra.io":
            case "teams.wire.com":
                return false;
            case "wire-teams-staging.zinfra.io":
                return true;
            default:
                throw new IllegalArgumentException(String.format("Not recognized team admin domain '%s'", teamAdminDomain));
        }
    }

    public boolean isOpenCVEnabled(Class<?> c) {
        return !Boolean.parseBoolean(getValue(c, "disableOpenCV"));
    }

    public String getDeviceName(Class<?> c) {
        return getValue(c, "deviceName");
    }

    public String getImagesPath(Class<?> c) {
        return getValue(c, "defaultImagesPath");
    }

    public String getAudioPath(Class<?> c) {
        return getValue(c, "defaultAudioPath");
    }

    public String getVideoPath(Class<?> c) {
        return getValue(c, "defaultVideoPath");
    }

    public String getMiscResourcesPath(Class<?> c) {
        return getValue(c, "defaultMiscResourcesPath");
    }

    public String getDefaultEmail(Class<?> c) {
        return getValue(c, "defaultEmail");
    }

    public String getDefaultEmailPassword(Class<?> c) {
        return getValue(c, "defaultEmailPassword");
    }

    public String getSpecialEmail(Class<?> c) {
        return getValue(c, "specialEmail");
    }

    public String getSpecialEmailPassword(Class<?> c) {
        return getValue(c, "specialPassword");
    }

    public String getDefaultEmailServer(Class<?> c) {
        return getValue(c, "defaultEmailServer");
    }

    public String getDriverTimeout(Class<?> c) {
        return getValue(c, "driverTimeoutSeconds");
    }

    public String getAppiumUrl(Class<?> c) {
        return getValue(c, "appiumUrl");
    }

    public boolean enableAppiumOutput(Class<?> c) {
        return (getValue(c, "enableAppiumOutput").equals("true"));
    }

    public boolean isSimulator(Class<?> c) {
        return (getValue(c, "isSimulator").equals("true"));
    }

    public String getIOSToolsRoot(Class<?> c) {
        return getValue(c, "iOSToolsRoot");
    }

    public String getWebAppApplicationPath(Class<?> c) {
        String path = getValue(c, "webappApplicationPath");
        if (!path.isEmpty()) {
            return path;
        } else {
            throw new RuntimeException("Could not find property: webappApplicationPath");
        }
    }

    public String getOverrrideWebappDockerImage(Class<?> c) {
        return getValue(c, "overrrideWebappDockerImage");
    }

    public String getWebsitePath(Class<?> c) {
        String path = getValue(c, "websitePath");
        if (!path.isEmpty()) {
            return path;
        } else {
            throw new RuntimeException("Could not find property: websitePath");
        }
    }

    public String getAccountPages(Class<?> c) {
        return getValue(c, "accountPagesPath");
    }

    public String getTeamAdminPath(Class<?> c) {
        return getValue(c, "teamAdminPath");
    }

    public String getTeamAdminDomain(Class<?> c) {
        String domain = "";
        try {
            domain = new URI(getTeamAdminPath(c)).getHost();
        } catch (Exception e) {
        }
        return domain;
    }

    public String getBrowserDownloadPath(Class<?> c) {
        return getValue(c, "browserDownloadPath");
    }

    public String getAndroidApplicationPath(Class<?> c) {
        return getValue(c, "androidApplicationPath");
    }

    public String getIosApplicationPath(Class<?> c) {
        return getValue(c, "iosApplicationPath");
    }

    public String getAndroidMainActivity(Class<?> c) {
        return getValue(c, "mainActivity");
    }

    public boolean getAndroidShowLogcat(Class<?> c) {
        return Boolean.parseBoolean(getOptionalValue(c, "showLogcat").orElse("true"));
    }

    public String getAndroidLaunchActivity(Class<?> c) {
        return getValue(c, "launchActivity");
    }

    public String getOldAppPath(Class<?> c) {
        return getValue(c, "oldAppPath");
    }

    public String isCountlyAvailable(Class<?> c) {
        return getValue(c, "isCountlyAvailable");
    }

    public String getCurrentApkVersion(Class<?> c) {
        return getValue(c, "currentApkVersion");
    }

    public String getKubeConfigPath(Class<?> c) {
        return getValue(c, "kubeConfigPath");
    }

    public Optional<String> getRealBuildNumber(Class<?> c) {
        return getOptionalValue(c, "realBuildNumber", configName);
    }

    public String getBundleId(Class<?> c) {
        return getValue(c, "bundleId");
    }

    public String getAndroidPackage(Class<?> c) {
        return getValue(c, "package");
    }

    public String getMailboxHandlerType(Class<?> c) {
        return getValue(c, "mailboxHandlerType");
    }

    public boolean isTablet(Class<?> c) {
        final Optional<String> value = getOptionalValue(c, "isTablet");
        return value.map(Boolean::valueOf).orElse(false);
    }

    public String getCallingServiceUrl(Class<?> c) {
        String callingServiceUrl = getValue(c, "callingServiceUrl");
        if (callingServiceUrl.equals("loadbalanced")) {
            return "https://qa-callingservice-wire.runs.onstackit.cloud";
        }
        if (callingServiceUrl.startsWith("http")) {
            return callingServiceUrl;
        } else {
            throw new RuntimeException("Missing or wrong callingServiceUrl: " + callingServiceUrl);
        }
    }

    public String getCallingServiceEnvironment(Class<?> c) {
        return getValue(c, "com.wire.calling.env").toUpperCase();
    }

    public String getDefaultEmailListenerUrl(Class<?> c) {
        if (isRunningOnJenkinsNode() && !isDesktop()) {
            return getValue(c, "defaultInternalEmailListenerUrl");
        }
        return getValue(c, "defaultEmailListenerUrl");
    }

    public boolean isScreenshootingEnabled(Class<?> c) {
        return Boolean.valueOf(getValue(c, "makeScreenshots"));
    }

    public String getTestrailServerUrl(Class<?> c) {
        if (isRunningOnJenkinsNode()) {
            return getValue(c, "internalTestrailServerUrl");
        }
        return getValue(c, "testrailServerUrl");
    }

    public String getTestrailUsername(Class<?> c) {
        return getValue(c, "testrailUser");
    }

    public String getTestrailToken(Class<?> c) {
        return getValue(c, "testrailToken");
    }

    public Optional<String> getTestrailProjectName(Class<?> c) {
        return getOptionalValue(c, "testrailProjectName");
    }

    public Optional<String> getTestrailPlanName(Class<?> c) {
        return getOptionalValue(c, "testrailPlanName");
    }

    public Optional<String> getTestrailRunName(Class<?> c) {
        return getOptionalValue(c, "testrailRunName");
    }

    public Optional<String> getTestrailRunConfigName(Class<?> c) {
        return getOptionalValue(c, "testrailRunConfigName");
    }

    public Optional<String> getTestinyProjectName(Class<?> c) {
        return getOptionalValue(c, "testinyProjectName");
    }

    public Optional<String> getTestinyRunName(Class<?> c) {
        return getOptionalValue(c, "testinyRunName");
    }

    public Optional<String> getJiraUrl(Class<?> c) {
        return getOptionalValue(c, "jiraUrl");
    }

    public boolean getSyncIsAutomated(Class<?> c) {
        return getValue(c, "syncIsAutomated").toLowerCase().equals("true");
    }

    public String getBuildPath(Class<?> c) {
        return getValue(c, "projectBuildPath");
    }

    public String getPlatformVersion(Class<?> cls) {
        return getValue(cls, "platformVersion");
    }

    public String getIOSAppName(Class<?> cls) {
        return getValue(cls, "appName");
    }

    public String getTestServiceUrl(Class<?> cls) {
        return getValue(cls, "testServiceUrl");
    }

    public String getOldTestServiceUrl(Class<?> cls) {
        return getValue(cls, "oldTestserviceUrl");
    }

    public Optional<String> getRcTestsCommentPath(Class<?> c) {
        return getOptionalValue(c, "rcTestsCommentPath");
    }

    public Optional<String> getCucumberReportUrl(Class<?> c) {
        return getOptionalValue(c, "cucumberReportUrl");
    }

    public String getAndroidToolsPath(Class<?> c) {
        return getValue(c, "androidToolsPath");
    }

    public String getPackageAutoDetection(Class<?> c) {
        return getValue(c, "packageAutoDetection");
    }

    public boolean isOnGrid(Class<?> c) {
        return Boolean.parseBoolean(getValue(c, "isOnGrid"));
    }

    public String getBrowserName(Class<?> c) {
        return getValue(c, "browserName");
    }

    public boolean getEnforceAppInstall(Class<?> c) {
        return Boolean.parseBoolean(getValue(c, "enforceAppInstall"));
    }

    public Optional<String> getUDID(Class<?> c) {
        return getOptionalValue(c, "UDID");
    }

    public String getTestingGalleryPath(Class<?> c) {
        return getValue(c, "testingGalleryPath");
    }
}
