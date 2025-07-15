/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package user.usermanager

import android.content.Context
import com.wire.android.testSupport.backendConnections.BackendClient
import com.wire.android.testSupport.backendConnections.team.TeamRoles
import com.wire.android.testSupport.backendConnections.team.createTeamOwnerViaBackend
import com.wire.android.testSupport.backendConnections.team.createTeamUserViaBackend
import kotlinx.coroutines.runBlocking
import logger.WireTestLogger
import user.usermanager.exceptions.NoSuchUserException
import user.utils.ClientUser
import user.utils.component.addEmailAlias
import user.utils.component.addFirstNameAlias
import user.utils.component.addNameAlias
import user.utils.component.addPasswordAlias
import user.utils.component.addUniqueUsernameAlias
import user.utils.component.clearEmailAliases
import user.utils.component.clearFirstNameAliases
import user.utils.component.clearNameAliases
import user.utils.component.clearPasswordAliases
import user.utils.component.clearUniqueUsernameAliases
import user.utils.component.removeEmailAlias
import user.utils.component.removeNameAlias
import user.utils.component.removePasswordAlias
import user.utils.component.removeUniqueUsernameAlias
import java.util.Collections
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import java.util.logging.Logger
import java.util.stream.Collectors

@Suppress("TooManyFunctions")
class ClientUserManager {

    private val usersMap: MutableMap<UserState, MutableList<ClientUser>> =
        ConcurrentHashMap<UserState, MutableList<ClientUser>>()
    private var useSpecialEmail: Boolean = false
    private var selfUser: ClientUser? = null

    companion object {
        private val NAME_ALIAS_TEMPLATE: Function<Int, String> = Function { idx -> "user${idx}Name" }
        private val FIRSTNAME_ALIAS_TEMPLATE: Function<Int, String> = Function { idx -> "user${idx}FirstName" }
        private val PASSWORD_ALIAS_TEMPLATE: Function<Int, String> = Function { idx -> "user${idx}Password" }
        private val EMAIL_ALIAS_TEMPLATE: Function<Int, String> =
            Function { idx -> "user${idx}Email" }
        val STR_UNIQUE_USERNAME_ALIAS_TEMPLATE: Function<String, String> =
            Function { idx -> "user${idx}UniqueUsername" }
        private val UNIQUE_USERNAME_ALIAS_TEMPLATE: Function<Int, String> =
            Function { idx -> STR_UNIQUE_USERNAME_ALIAS_TEMPLATE.apply("$idx") }
        const val MAX_USERS = 101
        const val MAX_USERS_IN_TEAM = 4000
        const val ALIASES_SEPARATOR = ","
        private val log: Logger = WireTestLogger.getLog(ClientUserManager::class.java.simpleName)
        private const val OTHER_USERS_ALIAS = "all other"
        private val SELF_USER_NAME_ALIASES = arrayOf("I", "Me", "Myself")
        private val SELF_USER_PASSWORD_ALIASES = arrayOf("myPassword")
        private val SELF_USER_EMAIL_ALIASES = arrayOf("myEmail")
        private val SELF_USER_UNIQUE_USERNAME_ALIASES = arrayOf("myUniqueUsername")

        @Suppress("LongParameterList")
        private fun setClientUserAliases(
            user: ClientUser,
            nameAliases: Array<String>?,
            firstNameAliases: Array<String>?,
            passwordAliases: Array<String>?,
            emailAliases: Array<String>?,
            uniqueUsernameAliases: Array<String>?
        ) {
            nameAliases?.takeIf { it.isNotEmpty() }?.let {
                user.clearNameAliases()
                it.forEach { alias -> user.addNameAlias(alias) }
            }
            firstNameAliases?.takeIf { it.isNotEmpty() }?.let {
                user.clearFirstNameAliases()
                it.forEach { alias -> user.addFirstNameAlias(alias) }
            }
            passwordAliases?.takeIf { it.isNotEmpty() }?.let {
                user.clearPasswordAliases()
                it.forEach { alias -> user.addPasswordAlias(alias) }
            }
            emailAliases?.takeIf { it.isNotEmpty() }?.let {
                user.clearEmailAliases()
                it.forEach { alias -> user.addEmailAlias(alias) }
            }
            uniqueUsernameAliases?.takeIf { it.isNotEmpty() }?.let {
                user.clearUniqueUsernameAliases()
                it.forEach { alias -> user.addUniqueUsernameAlias(alias) }
            }
        }
    }

    // Creates an empty manager (for maintenance jobs)
    constructor() {
        usersMap[UserState.Created] = ArrayList()
        usersMap[UserState.NotCreated] = ArrayList()
    }

    constructor(useSpecialEmail: Boolean) {
        usersMap[UserState.Created] = ArrayList()
        usersMap[UserState.NotCreated] = ArrayList()
        // Workaround for federation tests (can be deleted when inbucket is rolled out completely)
        this.useSpecialEmail = if (BackendClient.getDefault() == null || BackendClient.getDefault()?.hasInbucketSetup() == true) {
            false
        } else {
            useSpecialEmail
        }
        for (userIdx in 0 until MAX_USERS) {
            val pendingUser = ClientUser()
            setUserDefaults(pendingUser, userIdx)
            usersMap[UserState.NotCreated]?.add(pendingUser)
        }
    }

    /**
     * Generates an email in the format username+{index}@wire.com
     * @param userNumber The 1-based index of the user
     * @return The generated email address
     */
    private fun generateIndexedEmail(uniqueUserName: String, userNumber: Int): String {
        return "$uniqueUserName+$userNumber@wire.com"
    }

    /**
     * Sets default values and aliases for a user based on their index.
     * @param user The ClientUser object to modify
     * @param userIdx The index of the user (used for generating unique values)
     */
    private fun setUserDefaults(user: ClientUser, userIdx: Int) {
        // If special email flag is set, generate a unique email and password
        if (useSpecialEmail) {
            user.email = generateIndexedEmail(user.uniqueUsername ?: user.email.orEmpty(), userIdx + 1)
            user.emailPassword = UUID.randomUUID().toString()
        }

        // Create aliases for various user properties using templates
        val nameAliases = arrayOf(NAME_ALIAS_TEMPLATE.apply(userIdx + 1))
        val firstNameAliases = arrayOf(FIRSTNAME_ALIAS_TEMPLATE.apply(userIdx + 1))
        val passwordAliases = arrayOf(PASSWORD_ALIAS_TEMPLATE.apply(userIdx + 1))
        val emailAliases = arrayOf(EMAIL_ALIAS_TEMPLATE.apply(userIdx + 1))
        val uniqueUsernameAliases = arrayOf(UNIQUE_USERNAME_ALIAS_TEMPLATE.apply(userIdx + 1))

        // Set all the generated aliases for the user
        setClientUserAliases(user, nameAliases, firstNameAliases, passwordAliases, emailAliases, uniqueUsernameAliases)
    }

    /**
     * Moves users from NotCreated state to Created state and returns the moved users.
     * @param countOfUsersToBeAdded Number of users to move to Created state
     * @return Unmodifiable list of users that were moved to Created state
     */
    private fun syncCreatedState(countOfUsersToBeAdded: Int): List<ClientUser> {
        // Return empty list if count is invalid
        if (countOfUsersToBeAdded <= 0) {
            return emptyList()
        }

        // Get current lists of created and non-created users
        val createdUsers = usersMap[UserState.Created] ?: mutableListOf()
        val nonCreatedUsers = usersMap[UserState.NotCreated] ?: emptyList()

        // Get subset of users to move to created state
        val usersToBeAdded = nonCreatedUsers.subList(0, countOfUsersToBeAdded)

        // Add users to created list and update the map
        createdUsers.addAll(usersToBeAdded)
        val restOfNonCreatedUsers = nonCreatedUsers.subList(countOfUsersToBeAdded, nonCreatedUsers.size)
        usersMap[UserState.NotCreated] = restOfNonCreatedUsers.toMutableList()

        // Return immutable view of the added users
        return Collections.unmodifiableList(usersToBeAdded)
    }

    /**
     * Gets all users regardless of their creation state.
     * @return Unmodifiable list containing all users (both created and not created)
     */
    fun getAllUsers(): List<ClientUser> {
        val allUsers = ArrayList<ClientUser>()

        // Add both created and non-created users to the combined list
        allUsers.addAll(usersMap[UserState.Created] ?: emptyList())
        allUsers.addAll(usersMap[UserState.NotCreated] ?: emptyList())

        // Return immutable view of all users
        return Collections.unmodifiableList(allUsers)
    }

    /**
     * Retrieves all users who are team owners.
     * @return Set of ClientUser objects where isTeamOwner is true
     */
    fun getAllTeamOwners(): Set<ClientUser> =
        getAllUsers().stream()
            .filter { it.isTeamOwner }
            .collect(Collectors.toSet())

    /**
     * Finds a user by either email or name alias.
     * @param searchStr The string to search for (either email or name)
     * @return The matching ClientUser
     * @throws NoSuchUserException if no user matches the search criteria
     */
    @Throws(NoSuchUserException::class)
    fun findUserByEmailOrName(searchStr: String): ClientUser =
        findUserBy(searchStr, arrayOf(FindBy.EMAIL_ALIAS, FindBy.NAME_ALIAS))

    /**
     * Finds a user by password alias.
     * @param alias The password alias to search for
     * @return The matching ClientUser
     * @throws NoSuchUserException if no user matches the password alias
     */
    @Throws(NoSuchUserException::class)
    fun findUserByPasswordAlias(alias: String): ClientUser =
        findUserBy(alias, arrayOf(FindBy.PASSWORD_ALIAS))

    /**
     * Finds a user by either name or name alias.
     * @param alias The name or name alias to search for
     * @return The matching ClientUser
     * @throws NoSuchUserException if no user matches the name criteria
     */
    @Throws(NoSuchUserException::class)
    fun findUserByNameOrNameAlias(alias: String): ClientUser =
        findUserBy(alias, arrayOf(FindBy.NAME, FindBy.NAME_ALIAS))

    /**
     * Finds a user by either first name or first name alias.
     * @param alias The first name or first name alias to search for
     * @return The matching ClientUser
     * @throws NoSuchUserException if no user matches the first name criteria
     */
    @Throws(NoSuchUserException::class)
    fun findUserByFirstNameOrFirstNameAlias(alias: String): ClientUser =
        findUserBy(alias, arrayOf(FindBy.FIRSTNAME, FindBy.FIRSTNAME_ALIAS))

    /**
     * Finds a user by either email or email alias.
     * @param alias The email or email alias to search for
     * @return The matching ClientUser
     * @throws NoSuchUserException if no user matches the email criteria
     */
    @Throws(NoSuchUserException::class)
    fun findUserByEmailOrEmailAlias(alias: String): ClientUser =
        findUserBy(alias, arrayOf(FindBy.EMAIL, FindBy.EMAIL_ALIAS))

    /**
     * Finds a user by either unique username or unique username alias.
     * @param alias The unique username or its alias to search for
     * @return The matching ClientUser
     * @throws NoSuchUserException if no user matches the unique username criteria
     */
    @Throws(NoSuchUserException::class)
    fun findUserByUniqueUsernameAlias(alias: String): ClientUser =
        findUserBy(alias, arrayOf(FindBy.UNIQUE_USERNAME, FindBy.UNIQUE_USERNAME_ALIAS))

    /**
     * Searches for a user by different criteria in sequence until a user is found.
     *
     * @param searchStr The string to search for (could be username, email, etc.)
     * @param findByCriterias Array of search criteria to try in order
     * @return The found ClientUser
     * @throws NoSuchUserException if no user is found using any of the provided criteria
     */
    @Throws(NoSuchUserException::class)
    private fun findUserBy(searchStr: String, findByCriterias: Array<FindBy>): ClientUser {
        val trimmedSearchStr = searchStr.trim()
        for (findBy in findByCriterias) {
            try {
                return findUserBy(trimmedSearchStr, findBy)
            } catch (e: NoSuchUserException) {
                log.info("Cannot find user by: $searchStr")
            }
        }
        throw NoSuchUserException(
            "User '$searchStr' could not be found by '${
                findByCriterias.joinToString(", ") { it.name }
            }'"
        )
    }

    @Throws(NoSuchUserException::class)
    fun findUserBy(searchStr: String, findByCriteria: FindBy): ClientUser {
        return getAllUsers().firstOrNull { user ->
            matchesUser(user, searchStr, findByCriteria)
        } ?: throw NoSuchUserException("User '$searchStr' could not be found by '$findByCriteria'")
    }

    @Suppress("TooGenericExceptionThrown")
    private fun matchesUser(user: ClientUser, searchStr: String, findBy: FindBy): Boolean {
        val trimmed = searchStr.trim()
        return when (findBy) {
            FindBy.NAME_ALIAS -> user.nameAliases.any { it.equals(trimmed, ignoreCase = true) }
            FindBy.EMAIL_ALIAS -> user.emailAliases.any { it.equals(trimmed, ignoreCase = true) }
            FindBy.PASSWORD_ALIAS -> user.passwordAliases.any { it.equals(trimmed, ignoreCase = true) }
            FindBy.UNIQUE_USERNAME_ALIAS -> user.uniqueUsernameAliases.any { it.equals(trimmed, ignoreCase = true) }

            FindBy.NAME -> user.name.equals(trimmed, ignoreCase = true)
            FindBy.FIRSTNAME -> user.firstName.equals(trimmed, ignoreCase = true)
            FindBy.EMAIL -> user.email.equals(trimmed, ignoreCase = true)
            FindBy.PASSWORD -> user.password == searchStr
            FindBy.UNIQUE_USERNAME -> user.uniqueUsername.equals(trimmed, ignoreCase = true)

            else -> throw RuntimeException("Unknown FindBy criteria $findBy")
        }
    }

    /**
     * Finds all replacement values for aliases found in the source string.
     *
     * @param srcStr Comma-separated string potentially containing aliases
     * @param findByAliasTypes One or more alias types to search for
     * @return List of replacement values for any found aliases
     * @throws IllegalArgumentException if no alias types are provided or if an unsupported type is used
     */
    fun replaceAliasesOccurrences(srcStr: String, vararg findByAliasTypes: FindBy): String {
        require(findByAliasTypes.isNotEmpty()) { "At least one replacement type should be provided" }
        var result = srcStr
        for (dstUser in getAllUsers()) {
            for (aliasType in findByAliasTypes) {
                val (aliases, replacement) = when (aliasType) {
                    FindBy.NAME_ALIAS -> dstUser.nameAliases to dstUser.name
                    FindBy.FIRSTNAME_ALIAS -> dstUser.firstNameAliases to dstUser.firstName
                    FindBy.EMAIL_ALIAS -> dstUser.emailAliases to dstUser.email
                    FindBy.PASSWORD_ALIAS -> dstUser.passwordAliases to dstUser.password
                    FindBy.UNIQUE_USERNAME_ALIAS -> dstUser.uniqueUsernameAliases to dstUser.uniqueUsername
                    else -> throw IllegalArgumentException("Unsupported alias type '${aliasType.name}'")
                }
                for (alias in aliases) {
                    result = result.replace("(?i)\\b($alias)\\b".toRegex(), replacement.orEmpty())
                }
            }
        }
        return result
    }

    /**
     * Retrieves a list of replacement values for any matching aliases found in the source string.
     *
     * Processes a comma-separated input string against specified alias types for all users,
     * returning the corresponding actual values for any matches found.
     *
     * @param srcStr Comma-separated string containing potential aliases to match
     * @param findByAliasTypes Vararg of alias types to check against (NAME_ALIAS, EMAIL_ALIAS, etc.)
     * @return List of replacement values for any matched aliases (may contain empty strings)
     * @throws IllegalArgumentException if no alias types are provided or if an unsupported type is specified
     */
    fun getListByAliases(srcStr: String, vararg findByAliasTypes: FindBy): List<String> {
        require(findByAliasTypes.isNotEmpty()) { "At least one replacement type should be provided" }

        val srcList = srcStr.split(", ")
        return getAllUsers().flatMap { user ->
            findByAliasTypes.flatMap { aliasType ->
                val (aliases, replacement) = when (aliasType) {
                    FindBy.NAME_ALIAS -> user.nameAliases to user.name
                    FindBy.EMAIL_ALIAS -> user.emailAliases to user.email
                    FindBy.PASSWORD_ALIAS -> user.passwordAliases to user.password
                    FindBy.UNIQUE_USERNAME_ALIAS -> user.uniqueUsernameAliases to user.uniqueUsername
                    else -> throw IllegalArgumentException("Unsupported alias type '${aliasType.name}'")
                }

                srcList.filter { it in aliases }
                    .map { replacement.orEmpty() }
            }
        }
    }

    /**
     * Executes user creation operations in parallel for better performance with large datasets.
     *
     * Note: The parallel processing doesn't return the results of the creation operations,
     * but rather the original input list in unmodifiable form. Ensure the creation function
     * has proper side effects if results are needed.
     *
     * @param usersToCreate List of ClientUser objects to process
     * @param userCreationFunc Function that performs the actual user creation operation
     * @return Unmodifiable list of the original input users
     */
    private fun performParallelUsersCreation(
        usersToCreate: List<ClientUser>,
        userCreationFunc: Function<ClientUser, ClientUser>
    ): List<ClientUser> {
        usersToCreate.parallelStream().forEach { userCreationFunc.apply(it) }
        return Collections.unmodifiableList(usersToCreate)
    }

    /**
     * Generates personal users in parallel by creating them in the specified BackendClient system.
     *
     * This function mutates the input users by setting their backendName property before creation.
     * Uses parallel processing for better performance with large user lists.
     *
     * @param usersToCreate List of ClientUser objects to be created as personal users
     * @param BackendClient The BackendClient system where users will be created
     * @return Unmodifiable list of the created users with updated BackendClient information
     */
    private fun generatePersonalUsers(usersToCreate: List<ClientUser>, backend: BackendClient): List<ClientUser> =
        performParallelUsersCreation(usersToCreate) { usr ->
            usr.backendName = backend.name
            backend.createPersonalUserViaBackend(usr)
        }

    /**
     * Generates team members in parallel by creating them in the specified BackendClient system.
     *
     * This function mutates the input users by setting their backendName property before creation.
     * Uses parallel processing for efficient bulk creation of team members.
     *
     * @param membersToAdd List of ClientUser objects to be added as team members
     * @param teamOwner The owner/administrator of the team
     * @param teamId Unique identifier of the team
     * @param membersHaveHandles Flag indicating if members should have unique handles/identifiers
     * @param role Team role to assign to all new members (e.g., ADMIN, MEMBER)
     * @param BackendClient The BackendClient system where team members will be created
     * @param context Additional context information for the creation process
     * @return Unmodifiable list of the created team members with updated BackendClient information
     */
    @Suppress("LongParameterList")
    private fun generateTeamMembers(
        membersToAdd: List<ClientUser>,
        teamOwner: ClientUser,
        teamId: String,
        membersHaveHandles: Boolean,
        role: TeamRoles,
        backend: BackendClient,
        context: Context
    ): List<ClientUser> = performParallelUsersCreation(
        membersToAdd
    ) { usr ->
        usr.backendName = backend.name
        backend.createTeamUserViaBackend(
            teamOwner,
            teamId,
            usr,
            true,
            membersHaveHandles,
            role,
            context
        )
    }

    private fun fetchCreatedUsers(): List<ClientUser> {
        return Collections.unmodifiableList(this.usersMap[UserState.Created])
    }

    val createdUsers: List<ClientUser>
        get() = fetchCreatedUsers()

    private fun verifyUsersCountSatisfiesConstraints(countOfUsersToBeCreated: Int) {
        if (countOfUsersToBeCreated + createdUsers.size > MAX_USERS_IN_TEAM) {
            throw TooManyUsersToCreateException(
                "Cannot create $countOfUsersToBeCreated more users, " +
                        "because the maximum allowed number of available users is $MAX_USERS_IN_TEAM"
            )
        }
    }

    /**
     * Generates a list of email addresses for unactivated user accounts.
     *
     * Creates dummy user objects with default values and collects their email addresses.
     * The users are not persisted or activated in the system.
     *
     * @param amountToCreate Number of email addresses to generate
     * @return List of generated email strings (empty strings if email is null)
     */
    fun generateUnactivatedMails(amountToCreate: Int): List<String> {
        val unactivatedMails = ArrayList<String>()
        for (i in 0 until amountToCreate) {
            val user = ClientUser()
            setUserDefaults(user, createdUsers.size + i)
            log.info("Add new unactivated mail: ${user.email}")
            unactivatedMails.add(user.email.orEmpty())
        }
        return unactivatedMails
    }

    /**
     * Creates team members in bulk after verifying count constraints.
     *
     * @param teamOwner The team owner user object
     * @param teamId Unique identifier for the team
     * @param members List of users to add as team members
     * @param membersHaveHandles Whether members should have unique handles
     * @param role Team role to assign to all members
     * @param BackendClient Target BackendClient system
     * @param context Additional operation context
     * @return List of created team members (unmodifiable)
     * @throws ConstraintViolationException if member count exceeds system limits
     */
    @Suppress("LongParameterList")
    fun createTeamMembers(
        teamOwner: ClientUser,
        teamId: String,
        members: List<ClientUser>,
        membersHaveHandles: Boolean,
        role: TeamRoles,
        backend: BackendClient,
        context: Context
    ): List<ClientUser> {
        verifyUsersCountSatisfiesConstraints(members.size)
        return generateTeamMembers(members, teamOwner, teamId, membersHaveHandles, role, backend, context)
    }

    fun createXPersonalUsers(count: Int, backend: BackendClient) {
        verifyUsersCountSatisfiesConstraints(count)
        generatePersonalUsers(syncCreatedState(count), backend)
    }

    fun createPersonalUsersByAliases(nameAliases: List<String>, backend: BackendClient): List<ClientUser> {
        val usersToBeCreated = nameAliases.stream()
            .map { findUserByNameOrNameAlias(it) }
            .collect(Collectors.toList())
        verifyUsersCountSatisfiesConstraints(usersToBeCreated.size)
        generatePersonalUsers(usersToBeCreated, backend)
        return Collections.unmodifiableList(usersToBeCreated)
    }

    @Suppress("LongParameterList")
    fun createTeamOwnerByAlias(
        nameAlias: String,
        teamName: String,
        locale: String,
        updateHandle: Boolean,
        backend: BackendClient,
        context: Context
    ) {
        verifyUsersCountSatisfiesConstraints(1)
        var owner = findUserByNameOrNameAlias(nameAlias)
        WireTestLogger.getLog("Hello").info(owner.toString() + "innnnn")
        owner = runBlocking { backend.createTeamOwnerViaBackend(owner, teamName, locale, updateHandle, context) }
        owner.backendName = backend.name
        // remember all owners to later be able to delete all created teams
        owner.isTeamOwner = true
    }

    fun getSelfUserOrThrowError(): ClientUser =
        selfUser ?: throw SelfUserNotDefinedException("Self user should be defined in some previous step!")

    fun getSelfUser(): Optional<ClientUser> = Optional.ofNullable(selfUser)

    fun setSelfUser(usr: ClientUser) {
        require(getAllUsers().contains(usr)) {
            "User $usr should be one of precreated users!"
        }
        // this is to make sure that the user is in the list of created users
        appendCustomUser(usr)

        selfUser?.let { currentSelfUser ->
            SELF_USER_NAME_ALIASES.forEach { alias ->
                if (currentSelfUser.nameAliases.contains(alias)) {
                    currentSelfUser.removeNameAlias(alias)
                }
            }
            SELF_USER_PASSWORD_ALIASES.forEach { alias ->
                if (currentSelfUser.passwordAliases.contains(alias)) {
                    currentSelfUser.removePasswordAlias(alias)
                }
            }
            SELF_USER_EMAIL_ALIASES.forEach { alias ->
                if (currentSelfUser.emailAliases.contains(alias)) {
                    currentSelfUser.removeEmailAlias(alias)
                }
            }
            SELF_USER_UNIQUE_USERNAME_ALIASES.forEach { alias ->
                if (currentSelfUser.uniqueUsernameAliases.contains(alias)) {
                    currentSelfUser.removeUniqueUsernameAlias(alias)
                }
            }
        }

        this.selfUser = usr
        SELF_USER_NAME_ALIASES.forEach { selfUser?.addNameAlias(it) }
        SELF_USER_PASSWORD_ALIASES.forEach { selfUser?.addPasswordAlias(it) }
        SELF_USER_EMAIL_ALIASES.forEach { selfUser?.addEmailAlias(it) }
        SELF_USER_UNIQUE_USERNAME_ALIASES.forEach { selfUser?.addUniqueUsernameAlias(it) }
    }

    fun isSelfUserSet(): Boolean = selfUser != null

    fun isUserCreated(user: ClientUser): Boolean = usersMap[UserState.Created]?.contains(user) ?: false

    fun appendCustomUser(user: ClientUser): Int {
        usersMap[UserState.Created]?.let { safeCreatedUsers ->
            if (safeCreatedUsers.contains(user)) {
                return safeCreatedUsers.indexOf(user)
            }
            safeCreatedUsers.add(user)
        }
        usersMap[UserState.NotCreated]?.remove(user)
        return createdUsers.size - 1
    }

    fun splitAliases(aliases: String): List<String> {
        return if (aliases.lowercase().startsWith(OTHER_USERS_ALIAS)) {
            val otherUsers = ArrayList(createdUsers)
            otherUsers.remove(getSelfUserOrThrowError())
            otherUsers.map { it.name.orEmpty() }
        } else {
            aliases.split(ALIASES_SEPARATOR).map { it.trim() }
        }
    }

    enum class FindBy(val nameValue: String) {
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

        override fun toString(): String = name
    }

    class TooManyUsersToCreateException(msg: String) : RuntimeException(msg)

    class SelfUserNotDefinedException(msg: String) : RuntimeException(msg)
}
