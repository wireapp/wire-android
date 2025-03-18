package com.wearezeta.auto.common.usrmgmt;

import com.wearezeta.auto.common.Config;
import com.wearezeta.auto.common.backend.Backend;
import com.wearezeta.auto.common.backend.BackendConnections;
import com.wearezeta.auto.common.backend.models.AccentColor;
import com.wearezeta.auto.common.backend.models.AccessCredentials;
import com.wearezeta.auto.common.email.MessagingUtils;
import net.datafaker.Faker;
import net.datafaker.providers.base.Text;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;

import static com.wearezeta.auto.common.CommonUtils.generateRandomNumericString;
import static net.datafaker.providers.base.Text.*;

public class ClientUser {
    private AccessCredentials accessCredentials = null;
    private String id = null;
    private String name = null;
    private String firstName = null;
    private String lastName = null;
    private String password = null;
    private String email = null;
    private Set<String> emailAliases = new HashSet<>();
    private String emailPassword = null;
    private Set<String> nameAliases = new HashSet<>();
    private Set<String> firstNameAliases = new HashSet<>();
    private String uniqueUsername = null;
    private Set<String> uniqueUsernameAliases = new HashSet<>();
    private Set<String> passwordAliases = new HashSet<>();
    private AccentColor accentColor = AccentColor.Undefined;
    private String teamId = null;
    private Duration expiresIn = null;
    private String serviceProviderId = null;
    private boolean SSO = false;
    private boolean SCIM = false;
    private String backendName = null;
    private boolean hardcoded = false;
    public Callable<String> getUserIdThroughOwner;
    private boolean isTeamOwner = false;
    private boolean registeredLegalHoldServices = false;
    private String verificationCode = null;
    private String activationCode = null;
    private final String customSpecialSymbols = "!@#$";

    public ClientUser() {
        Faker faker = new Faker();
        this.firstName = faker.name().firstName();
        this.lastName = faker.name().lastName();
        // Reroll last name if it contains a quote to not break locator checks later
        while (this.lastName.contains("'")) {
            this.lastName = faker.name().lastName();
        }
        this.name = String.format("%s %s", firstName, lastName);
        this.password = faker.text().text(Text.TextSymbolsBuilder.builder()
                .len(8)
                .with(EN_LOWERCASE, 3)
                .with(EN_UPPERCASE, 1)
                .with(customSpecialSymbols, 1)
                .with(DIGITS, 1).build());
        this.uniqueUsername = sanitizedRandomizedHandle(lastName);
        if (BackendConnections.getDefault() != null && BackendConnections.getDefault().hasInbucketSetup()) {
            this.email = MessagingUtils.generateEmail(null, uniqueUsername);
        } else {
            this.email = MessagingUtils.generateEmail(MessagingUtils.getDefaultAccountName(), uniqueUsername);
        }
        this.emailPassword = Config.current().getDefaultEmailPassword(ClientUser.class);
    }

    public ClientUser(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.name = String.format("%s %s", firstName, lastName);
        this.email = email;
        this.password = password;
    }

    public void setAccessCredentials(AccessCredentials accessCredentials) {
        this.accessCredentials = accessCredentials;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getNameAliases() {
        return new HashSet<>(this.nameAliases);
    }

    public Set<String> getFirstNameAliases() {
        return new HashSet<>(this.firstNameAliases);
    }

    public void addNameAlias(String alias) {
        this.nameAliases.add(alias);
    }

    public void removeNameAlias(String alias) {
        this.nameAliases.remove(alias);
    }

    public void clearNameAliases() {
        this.nameAliases.clear();
    }

    public void addFirstNameAlias(String alias) {
        this.firstNameAliases.add(alias);
    }

    public void removeFirstNameAlias(String alias) {
        this.firstNameAliases.remove(alias);
    }

    public void clearFirstNameAliases() {
        this.firstNameAliases.clear();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<String> getPasswordAliases() {
        return new HashSet<>(this.passwordAliases);
    }

    public void addPasswordAlias(String alias) {
        this.passwordAliases.add(alias);
    }

    public void removePasswordAlias(String alias) {
        this.passwordAliases.remove(alias);
    }

    public void clearPasswordAliases() {
        this.passwordAliases.clear();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<String> getEmailAliases() {
        return new HashSet<>(this.emailAliases);
    }

    public void addEmailAlias(String alias) {
        this.emailAliases.add(alias);
    }

    public void removeEmailAlias(String alias) {
        this.emailAliases.remove(alias);
    }

    public void clearEmailAliases() {
        this.emailAliases.clear();
    }

    public void setEmailPassword(String emailPassword) {
        this.emailPassword = emailPassword;
    }

    public String getEmailPassword() {
        return emailPassword;
    }

    public AccessCredentials getAccessCredentialsWithoutRefresh() {
        return accessCredentials;
    }

    public String getId() {
        if (this.id == null) {
            if (SSO) {
                try {
                    this.id = getUserIdThroughOwner.call();
                } catch (Exception e) {
                    throw new IllegalStateException ("No owner id; " + e);
                }
            } else {
                Backend backend = BackendConnections.get(backendName);
                this.id = backend.getUserId(this);
            }
        }
        return id;
    }

    public void setAccentColor(AccentColor newColor) {
        this.accentColor = newColor;
    }

    public AccentColor getAccentColor() {
        return this.accentColor;
    }

    public String getUniqueUsername() {
        return uniqueUsername;
    }

    public void setUniqueUsername(String uniqueUsername) {
        this.uniqueUsername = uniqueUsername;
    }

    public void addUniqueUsernameAlias(String alias) {
        this.uniqueUsernameAliases.add(alias);
    }

    public void removeUniqueUsernameAlias(String alias) {
        this.uniqueUsernameAliases.remove(alias);
    }

    public void clearUniqueUsernameAliases() {
        this.uniqueUsernameAliases.clear();
    }

    public Set<String> getUniqueUsernameAliases() {
        return new HashSet<>(this.uniqueUsernameAliases);
    }

    public static String sanitizedRandomizedHandle(String derivative) {
        return String.format("%s%s", derivative, generateRandomNumericString(8))
                .replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase();
    }

    public void setManagedBySCIM() {
        this.SCIM = true;
    }

    public boolean isManagedBySCIM() {
        return SCIM;
    }

    public void forceTokenExpiration() {
        this.accessCredentials = null;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof ClientUser) && ((ClientUser) other).getEmail().equals(getEmail());
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public void setExpiresIn(Duration expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Duration getExpiresIn() {
        return expiresIn;
    }

    public boolean isSSOUser() {
        return SSO;
    }

    public void setUserIsSSOUser() {
        this.SSO = true;
    }

    public String getBackendName() {
        return backendName;
    }

    public void setBackendName(String backendName) {
        this.backendName = backendName;
    }

    public boolean isHarcoded() {
        return hardcoded;
    }

    public void setHardcoded(boolean hardcoded) {
        this.hardcoded = hardcoded;
    }

    public void setTeamOwner(boolean isTeamOwner) {
        this.isTeamOwner = isTeamOwner;
    }

    public boolean isTeamOwner() {
        return isTeamOwner;
    }

    public boolean hasRegisteredLegalHoldService() {
        return registeredLegalHoldServices;
    }

    public void setRegisteredLegalHoldService(boolean value) {
        this.registeredLegalHoldServices = value;
    }

    public boolean hasServiceProvider() {
        return this.serviceProviderId != null;
    }

    public void setServiceProviderId(String id) {
        this.serviceProviderId = id;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public String getVerificationCode() {
        return this.verificationCode;
    }

    public void setActivationCode(String activationCode) {
        this.activationCode = activationCode;
    }

    public String getActivationCode() {
        return this.activationCode;
    }
}