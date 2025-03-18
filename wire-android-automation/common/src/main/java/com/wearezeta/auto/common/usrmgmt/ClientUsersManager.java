package com.wearezeta.auto.common.usrmgmt;

import com.wearezeta.auto.common.backend.Backend;
import com.wearezeta.auto.common.backend.BackendConnections;
import com.wearezeta.auto.common.email.MessagingUtils;
import com.wearezeta.auto.common.log.ZetaLogger;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ClientUsersManager {
    private static final Function<Integer, String> NAME_ALIAS_TEMPLATE = idx -> String
            .format("user%dName", idx);
    private static final Function<Integer, String> FIRSTNAME_ALIAS_TEMPLATE = idx -> String
            .format("user%dFirstName", idx);
    private static final Function<Integer, String> PASSWORD_ALIAS_TEMPLATE = idx -> String
            .format("user%dPassword", idx);
    private static final Function<Integer, String> EMAIL_ALIAS_TEMPLATE = idx -> String
            .format("user%dEmail", idx);
    public static final Function<String, String> STR_UNIQUE_USERNAME_ALIAS_TEMPLATE = idx -> String
            .format("user%sUniqueUsername", idx);

    private static final Function<Integer, String> UNIQUE_USERNAME_ALIAS_TEMPLATE = idx ->
            STR_UNIQUE_USERNAME_ALIAS_TEMPLATE.apply("" + idx);
    public static final int MAX_USERS = 101;
    public static final int MAX_USERS_IN_TEAM = 4000;
    public static final String ALIASES_SEPARATOR = ",";
    private static final Logger log = ZetaLogger.getLog(ClientUsersManager.class.getSimpleName());
    private static final String OTHER_USERS_ALIAS = "all other";
    private static String[] SELF_USER_NAME_ALIASES = new String[]{"I", "Me", "Myself"};
    private static String[] SELF_USER_PASSWORD_ALIASES = new String[]{"myPassword"};
    private static String[] SELF_USER_EMAIL_ALIASES = new String[]{"myEmail"};
    private static String[] SELF_USER_UNIQUE_USERNAME_ALIASES = new String[]{"myUniqueUsername"};
    private final Map<UserState, List<ClientUser>> usersMap = new ConcurrentHashMap<>();
    private boolean useSpecialEmail;
    private ClientUser selfUser;

    // Creates an empty manager (for maintenance jobs)
    public ClientUsersManager() {
        usersMap.put(UserState.Created, new ArrayList<>());
        usersMap.put(UserState.NotCreated, new ArrayList<>());
    }

    public ClientUsersManager(boolean useSpecialEmail) {
        usersMap.put(UserState.Created, new ArrayList<>());
        usersMap.put(UserState.NotCreated, new ArrayList<>());
        // Workaround for federation tests (can be deleted when inbucket is rolled out completely)
        if (BackendConnections.getDefault() == null || BackendConnections.getDefault().hasInbucketSetup()) {
            this.useSpecialEmail = false;
        } else {
            this.useSpecialEmail = useSpecialEmail;
        }
        for (int userIdx = 0; userIdx < MAX_USERS; userIdx++) {
            ClientUser pendingUser = new ClientUser();
            setUserDefaults(pendingUser, userIdx);
            usersMap.get(UserState.NotCreated).add(pendingUser);
        }
    }

    private static void setClientUserAliases(ClientUser user,
                                             String[] nameAliases,
                                             String[] firstNameAliases,
                                             String[] passwordAliases,
                                             String[] emailAliases,
                                             String[] uniqueUsernameAliases) {
        if (nameAliases != null && nameAliases.length > 0) {
            user.clearNameAliases();
            for (String nameAlias : nameAliases) {
                user.addNameAlias(nameAlias);
            }
        }
        if (firstNameAliases != null && firstNameAliases.length > 0) {
            user.clearFirstNameAliases();
            for (String firstNameAlias : firstNameAliases) {
                user.addFirstNameAlias(firstNameAlias);
            }
        }
        if (passwordAliases != null && passwordAliases.length > 0) {
            user.clearPasswordAliases();
            for (String passwordAlias : passwordAliases) {
                user.addPasswordAlias(passwordAlias);
            }
        }
        if (emailAliases != null && emailAliases.length > 0) {
            user.clearEmailAliases();
            for (String emailAlias : emailAliases) {
                user.addEmailAlias(emailAlias);
            }
        }
        if (uniqueUsernameAliases != null && uniqueUsernameAliases.length > 0) {
            user.clearUniqueUsernameAliases();
            for (String alias : uniqueUsernameAliases) {
                user.addUniqueUsernameAlias(alias);
            }
        }
    }

    private void setUserDefaults(ClientUser user, int userIdx) {
        if (useSpecialEmail) {
            user.setEmail(MessagingUtils.generateEmail(MessagingUtils.getSpecialAccountName(), user.getUniqueUsername()));
            user.setEmailPassword(MessagingUtils.getSpecialAccountPassword());
        }
        final String[] nameAliases = new String[]{NAME_ALIAS_TEMPLATE.apply(userIdx + 1)};
        final String[] firstNameAliases = new String[]{FIRSTNAME_ALIAS_TEMPLATE.apply(userIdx + 1)};
        final String[] passwordAliases = new String[]{PASSWORD_ALIAS_TEMPLATE.apply(userIdx + 1)};
        final String[] emailAliases = new String[]{EMAIL_ALIAS_TEMPLATE.apply(userIdx + 1)};
        final String[] uniqueUsernameAliases = new String[]{UNIQUE_USERNAME_ALIAS_TEMPLATE.apply(userIdx + 1)};
        setClientUserAliases(user, nameAliases, firstNameAliases, passwordAliases, emailAliases, uniqueUsernameAliases);
    }

    public List<ClientUser> getCreatedUsers() {
        return Collections.unmodifiableList(this.usersMap.get(UserState.Created));
    }

    private List<ClientUser> syncCreatedState(int countOfUsersToBeAdded) {
        if (countOfUsersToBeAdded <= 0) {
            return Collections.emptyList();
        }
        final List<ClientUser> createdUsers = this.usersMap.get(UserState.Created);
        final List<ClientUser> nonCreatedUsers = this.usersMap.get(UserState.NotCreated);
        final List<ClientUser> usersToBeAdded = nonCreatedUsers.subList(0, countOfUsersToBeAdded);
        createdUsers.addAll(usersToBeAdded);
        final List<ClientUser> restOfNonCreatedUsers = nonCreatedUsers
                .subList(countOfUsersToBeAdded, nonCreatedUsers.size());
        this.usersMap.put(UserState.NotCreated, restOfNonCreatedUsers);
        return Collections.unmodifiableList(usersToBeAdded);
    }

    public List<ClientUser> getAllUsers() {
        final List<ClientUser> allUsers = new ArrayList<>();
        allUsers.addAll(this.usersMap.get(UserState.Created));
        allUsers.addAll(this.usersMap.get(UserState.NotCreated));
        return Collections.unmodifiableList(allUsers);
    }

    public Set<ClientUser> getAllTeamOwners() {
        return getAllUsers().stream().filter(ClientUser::isTeamOwner).collect(Collectors.toSet());
    }

    public ClientUser findUserByEmailOrName(String searchStr) throws NoSuchUserException {
        return findUserBy(searchStr, new FindBy[]{FindBy.EMAIL_ALIAS, FindBy.NAME_ALIAS});
    }

    public ClientUser findUserByPasswordAlias(String alias) throws NoSuchUserException {
        return findUserBy(alias, new FindBy[]{FindBy.PASSWORD_ALIAS});
    }

    public ClientUser findUserByNameOrNameAlias(String alias) throws NoSuchUserException {
        return findUserBy(alias,
                new FindBy[]{FindBy.NAME, FindBy.NAME_ALIAS});
    }

    public ClientUser findUserByFirstNameOrFirstNameAlias(String alias) throws NoSuchUserException {
        return findUserBy(alias,
                new FindBy[]{FindBy.FIRSTNAME, FindBy.FIRSTNAME_ALIAS});
    }

    public ClientUser findUserByEmailOrEmailAlias(String alias) throws NoSuchUserException {
        return findUserBy(alias, new FindBy[]{FindBy.EMAIL,
                FindBy.EMAIL_ALIAS});
    }

    public ClientUser findUserByUniqueUsernameAlias(String alias) throws NoSuchUserException {
        return findUserBy(alias,
                new FindBy[]{FindBy.UNIQUE_USERNAME, FindBy.UNIQUE_USERNAME_ALIAS});
    }

    private ClientUser findUserBy(String searchStr, FindBy[] findByCriterias) throws NoSuchUserException {
        for (FindBy findBy : findByCriterias) {
            try {
                return findUserBy(searchStr, findBy);
            } catch (NoSuchUserException e) {
                log.info("Cannot find user by: " + searchStr);
            }
        }
        throw new NoSuchUserException(String.format(
                "User '%s' could not be found by '%s'", searchStr,
                Arrays.stream(findByCriterias).map(f -> f.name).collect(Collectors.joining(", "))
                ));
    }

    public ClientUser findUserBy(String searchStr, FindBy findByCriteria) throws NoSuchUserException {
        searchStr = searchStr.trim();
        for (ClientUser user : getAllUsers()) {
            Set<String> aliases = new HashSet<>();
            if (findByCriteria == FindBy.NAME_ALIAS) {
                aliases = user.getNameAliases();
            } else if (findByCriteria == FindBy.EMAIL_ALIAS) {
                aliases = user.getEmailAliases();
            } else if (findByCriteria == FindBy.PASSWORD_ALIAS) {
                aliases = user.getPasswordAliases();
            } else if (findByCriteria == FindBy.UNIQUE_USERNAME_ALIAS) {
                aliases = user.getUniqueUsernameAliases();
            } else if (findByCriteria == FindBy.NAME) {
                if (user.getName().equalsIgnoreCase(searchStr)) {
                    return user;
                }
            } else if (findByCriteria == FindBy.FIRSTNAME) {
                if (user.getFirstName().equalsIgnoreCase(searchStr)) {
                    return user;
                }
            } else if (findByCriteria == FindBy.EMAIL) {
                if (user.getEmail().equalsIgnoreCase(searchStr)) {
                    return user;
                }
            } else if (findByCriteria == FindBy.PASSWORD) {
                if (user.getPassword().equals(searchStr)) {
                    return user;
                }
            } else if (findByCriteria == FindBy.UNIQUE_USERNAME) {
                if (user.getUniqueUsername().equalsIgnoreCase(searchStr)) {
                    return user;
                }
            } else {
                throw new RuntimeException(String.format("Unknown FindBy criteria %s", findByCriteria));
            }
            for (String currentAlias : aliases) {
                if (currentAlias.equalsIgnoreCase(searchStr)) {
                    return user;
                }
            }
        }
        throw new NoSuchUserException(String.format("User '%s' could not be found by '%s'", searchStr, findByCriteria));
    }

    public String replaceAliasesOccurrences(String srcStr, FindBy... findByAliasTypes) {
        // At least one replacement type should be provided
        assert(findByAliasTypes.length > 0);
        String result = srcStr;
        for (ClientUser dstUser : getAllUsers()) {
            for (FindBy aliasType : findByAliasTypes) {
                Set<String> aliases;
                String replacement;
                switch (aliasType) {
                    case NAME_ALIAS:
                        aliases = dstUser.getNameAliases();
                        replacement = dstUser.getName();
                        break;
                    case FIRSTNAME_ALIAS:
                        aliases = dstUser.getFirstNameAliases();
                        replacement = dstUser.getFirstName();
                        break;
                    case EMAIL_ALIAS:
                        aliases = dstUser.getEmailAliases();
                        replacement = dstUser.getEmail();
                        break;
                    case PASSWORD_ALIAS:
                        aliases = dstUser.getPasswordAliases();
                        replacement = dstUser.getPassword();
                        break;
                    case UNIQUE_USERNAME_ALIAS:
                        aliases = dstUser.getUniqueUsernameAliases();
                        replacement = dstUser.getUniqueUsername();
                        break;
                    default:
                        throw new IllegalArgumentException(String.format("Unsupported alias type '%s'", aliasType.name()));
                }
                for (String alias : aliases) {
                    result = result.replaceAll("(?i)\\b(" + alias + ")\\b", replacement);
                }
            }
        }
        return result;
    }

    /**
     * This method is taking a string list of aliases and returns a List of strings of the found AliasTypes
     * @param srcStr string list of aliases
     * @param findByAliasTypes alias type to look for
     * @return List of names mapped onto the aliases
     */
    public List<String> getListByAliases(String srcStr, FindBy... findByAliasTypes) {
        // At least one replacement type should be provided
        assert(findByAliasTypes.length > 0);
        List<String> result = new ArrayList<>();
        for (ClientUser dstUser : getAllUsers()) {
            for (FindBy aliasType : findByAliasTypes) {
                Set<String> aliases;
                String replacement;
                switch (aliasType) {
                    case NAME_ALIAS:
                        aliases = dstUser.getNameAliases();
                        replacement = dstUser.getName();
                        break;
                    case EMAIL_ALIAS:
                        aliases = dstUser.getEmailAliases();
                        replacement = dstUser.getEmail();
                        break;
                    case PASSWORD_ALIAS:
                        aliases = dstUser.getPasswordAliases();
                        replacement = dstUser.getPassword();
                        break;
                    case UNIQUE_USERNAME_ALIAS:
                        aliases = dstUser.getUniqueUsernameAliases();
                        replacement = dstUser.getUniqueUsername();
                        break;
                    default:
                        throw new IllegalArgumentException(String.format("Unsupported alias type '%s'", aliasType.name()));
                }

                String[] srcList = srcStr.split(", ");
                for(int i = 0; i < srcList.length; i++) {
                    if(aliases.contains(srcList[i])) {
                        result.add(replacement);
                    }
                }
            }
        }
        return result;
    }

    private static List<ClientUser> performParallelUsersCreation(List<ClientUser> usersToCreate,
                                                                 Function<ClientUser, ClientUser> userCreationFunc) {
        usersToCreate.stream().parallel().forEach(userCreationFunc::apply);
        return Collections.unmodifiableList(usersToCreate);
    }

    // ! Mutates the users in the list
    private List<ClientUser> generatePersonalUsers(List<ClientUser> usersToCreate, Backend backend) {
        return performParallelUsersCreation(usersToCreate,
                (usr) -> {
                    usr.setBackendName(backend.getBackendName());
                    return backend.createPersonalUserViaBackdoor(usr);
                });
    }

    // ! Mutates the users in the list
    private List<ClientUser> generateWirelessUsers(List<ClientUser> usersToCreate, Backend backend) {
        return performParallelUsersCreation(usersToCreate,
                (usr) -> {
                    usr.setBackendName(backend.getBackendName());
                    return backend.createWirelessUserViaBackdoor(usr);
                });
    }

    // ! Mutates the users in the list
    private List<ClientUser> generateTeamMembers(List<ClientUser> membersToAdd, final ClientUser teamOwner,
                                                 final String teamId, final boolean membersHaveHandles, String role,
                                                 Backend backend) {
        return performParallelUsersCreation(membersToAdd,
                (usr) -> {
                    usr.setBackendName(backend.getBackendName());
                    return backend.createTeamUserViaBackdoor(teamOwner, teamId, usr, true, membersHaveHandles, role);
                });
    }

    private void verifyUsersCountSatisfiesConstraints(int countOfUsersToBeCreated) {
        if (countOfUsersToBeCreated + this.getCreatedUsers().size() > MAX_USERS_IN_TEAM) {
            throw new TooManyUsersToCreateException(String.format(
                    "Cannot create %d more users, because the maximum allowed number of available users is %d",
                    countOfUsersToBeCreated, MAX_USERS_IN_TEAM));
        }
    }

    public List<String> generateUnactivatedMails(int amountToCreate) {
        List<String> unactivatedMails = new ArrayList<>();
        for (int i = 0; i < amountToCreate; i++) {
            ClientUser user = new ClientUser();
            setUserDefaults(user, getCreatedUsers().size() + i);
            log.info("Add new unactivated mail: " + user.getEmail());
            unactivatedMails.add(user.getEmail());
        }
        return unactivatedMails;
    }

    public List<ClientUser> createWirelessUsers(List<ClientUser> users, Backend backend) {
        verifyUsersCountSatisfiesConstraints(users.size());
        return generateWirelessUsers(users, backend);
    }

    public List<ClientUser> createTeamMembers(ClientUser teamOwner, String teamId, List<ClientUser> members,
                                              final boolean membersHaveHandles, String role, Backend backend) {
        verifyUsersCountSatisfiesConstraints(members.size());
        return generateTeamMembers(members, teamOwner, teamId, membersHaveHandles, role, backend);
    }

    public void createXPersonalUsers(int count, Backend backend) {
        verifyUsersCountSatisfiesConstraints(count);
        generatePersonalUsers(syncCreatedState(count), backend);
    }

    public List<ClientUser> createPersonalUsersByAliases(List<String> nameAliases, Backend backend) {
        final List<ClientUser> usersToBeCreated = nameAliases.stream()
                .map(this::findUserByNameOrNameAlias)
                .collect(Collectors.toList());
        verifyUsersCountSatisfiesConstraints(usersToBeCreated.size());
        generatePersonalUsers(usersToBeCreated, backend);
        return Collections.unmodifiableList(usersToBeCreated);
    }

    public void createTeamOwnerByAlias(String nameAlias, String teamName, String locale, boolean updateHandle,
                                       Backend backend) {
        verifyUsersCountSatisfiesConstraints(1);
        ClientUser owner = this.findUserByNameOrNameAlias(nameAlias);
        owner = backend.createTeamOwnerViaBackdoor(owner, teamName, locale, updateHandle);
        owner.setBackendName(backend.getBackendName());
        // remember all owners to later be able to delete all created teams
        owner.setTeamOwner(true);
    }

    public ClientUser getSelfUserOrThrowError() {
        return Optional.ofNullable(selfUser).orElseThrow(
                () -> new SelfUserNotDefinedException("Self user should be defined in some previous step!")
        );
    }

    public Optional<ClientUser> getSelfUser() {
        return Optional.ofNullable(selfUser);
    }

    @SuppressWarnings("ConstantConditions")
    public void setSelfUser(ClientUser usr) {
        if (!this.getAllUsers().contains(usr)) {
            throw new IllegalArgumentException(String.format(
                    "User %s should be one of precreated users!", usr.toString()));
        }
        // this is to make sure that the user is in the list of created users
        appendCustomUser(usr);

        if (this.selfUser != null) {
            for (String nameAlias : SELF_USER_NAME_ALIASES) {
                if (this.selfUser.getNameAliases().contains(nameAlias)) {
                    this.selfUser.removeNameAlias(nameAlias);
                }
            }
            for (String passwordAlias : SELF_USER_PASSWORD_ALIASES) {
                if (this.selfUser.getPasswordAliases().contains(passwordAlias)) {
                    this.selfUser.removePasswordAlias(passwordAlias);
                }
            }
            for (String emailAlias : SELF_USER_EMAIL_ALIASES) {
                if (this.selfUser.getEmailAliases().contains(emailAlias)) {
                    this.selfUser.removeEmailAlias(emailAlias);
                }
            }
            for (String alias : SELF_USER_UNIQUE_USERNAME_ALIASES) {
                if (this.selfUser.getUniqueUsernameAliases().contains(alias)) {
                    this.selfUser.removeUniqueUsernameAlias(alias);
                }
            }
        }
        assert(usr != null);
        this.selfUser = usr;
        for (String nameAlias : SELF_USER_NAME_ALIASES) {
            this.selfUser.addNameAlias(nameAlias);
        }
        for (String passwordAlias : SELF_USER_PASSWORD_ALIASES) {
            this.selfUser.addPasswordAlias(passwordAlias);
        }
        for (String emailAlias : SELF_USER_EMAIL_ALIASES) {
            this.selfUser.addEmailAlias(emailAlias);
        }
        for (String alias : SELF_USER_UNIQUE_USERNAME_ALIASES) {
            this.selfUser.addUniqueUsernameAlias(alias);
        }
    }

    public boolean isSelfUserSet() {
        return this.selfUser != null;
    }

    /**
     * Check if a user is already in the user map for created users
     *
     * @param user prepared ClientUser instance
     * @return true if user is in map of created users
     */
    public boolean isUserCreated(ClientUser user) {
        return this.usersMap.get(UserState.Created).contains(user);
    }

    /**
     * Add custom user to the end of internal list of users, so then it is
     * possible to use it in "standard" steps
     * <p>
     * Be careful when use this method. Make sure, that this user has been
     * already created and has all the necessary aliases already set
     * The method will return current user index in the list if it has been already added
     *
     * @param user prepared ClientUser instance
     * @return index in users list
     */
    public int appendCustomUser(ClientUser user) {
        if (this.usersMap.get(UserState.Created).contains(user)) {
            return this.usersMap.get(UserState.Created).indexOf(user);
        }
        this.usersMap.get(UserState.Created).add(user);
        this.usersMap.get(UserState.NotCreated).remove(user);
        return getCreatedUsers().size() - 1;
    }

    public List<String> splitAliases(String aliases) {
        if (aliases.toLowerCase().startsWith(OTHER_USERS_ALIAS)) {
            final List<ClientUser> otherUsers = new ArrayList<>(getCreatedUsers());
            otherUsers.remove(getSelfUserOrThrowError());
            return otherUsers.stream().map(ClientUser::getName).collect(Collectors.toList());
        }
        return Arrays.stream(aliases.split(ALIASES_SEPARATOR))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    public enum FindBy {
        NAME("Name"),
        NAME_ALIAS("Name Alias(es)"),
        PASSWORD("Password"),
        PASSWORD_ALIAS("Password Alias(es)"),
        EMAIL("Email"),
        EMAIL_ALIAS("Email Alias(es)"),
        FIRSTNAME("First Name"),
        FIRSTNAME_ALIAS("First Name Alias(es)"),
        UNIQUE_USERNAME("Unique Username"),
        UNIQUE_USERNAME_ALIAS("Unique Username Alias(es)");

        private final String name;

        FindBy(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    public static class TooManyUsersToCreateException extends RuntimeException {
        private static final long serialVersionUID = -7730445785978830114L;

        TooManyUsersToCreateException(String msg) {
            super(msg);
        }
    }

    public static class SelfUserNotDefinedException extends RuntimeException {
        private static final long serialVersionUID = 5586439025162442603L;

        SelfUserNotDefinedException(String msg) {
            super(msg);
        }
    }
}
