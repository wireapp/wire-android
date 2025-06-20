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
import backendConnections.Backend
import com.wire.android.testSupport.backendConnections.team.TeamRoles
import com.wire.android.testSupport.backendConnections.team.createTeamOwnerViaBackend
import com.wire.android.testSupport.backendConnections.team.createTeamUserViaBackend
import kotlinx.coroutines.runBlocking
import logger.WireTestLogger
import user.usermanager.exceptions.NoSuchUserException
import user.utils.ClientUser
import java.util.Collections
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import java.util.logging.Logger
import java.util.stream.Collectors

class ClientUsersManager {
    private val usersMap: MutableMap<UserState, MutableList<ClientUser>> = ConcurrentHashMap<UserState, MutableList<ClientUser>>()
    private var useSpecialEmail: Boolean = false
    private var selfUser: ClientUser? = null

    companion object {
        private val NAME_ALIAS_TEMPLATE: Function<Int, String> = Function { idx -> "user${idx}Name" }
        private val FIRSTNAME_ALIAS_TEMPLATE: Function<Int, String> = Function { idx -> "user${idx}FirstName" }
        private val PASSWORD_ALIAS_TEMPLATE: Function<Int, String> = Function { idx -> "user${idx}Password" }
        private val EMAIL_ALIAS_TEMPLATE: Function<Int, String> = Function { idx -> "user${idx}Email" }
        val STR_UNIQUE_USERNAME_ALIAS_TEMPLATE: Function<String, String> = Function { idx -> "user${idx}UniqueUsername" }
        private val UNIQUE_USERNAME_ALIAS_TEMPLATE: Function<Int, String> =
            Function { idx -> STR_UNIQUE_USERNAME_ALIAS_TEMPLATE.apply("$idx") }
        const val MAX_USERS = 101
        const val MAX_USERS_IN_TEAM = 4000
        const val ALIASES_SEPARATOR = ","
        private val log: Logger = WireTestLogger.getLog(ClientUsersManager::class.java.simpleName)
        private const val OTHER_USERS_ALIAS = "all other"
        private val SELF_USER_NAME_ALIASES = arrayOf("I", "Me", "Myself")
        private val SELF_USER_PASSWORD_ALIASES = arrayOf("myPassword")
        private val SELF_USER_EMAIL_ALIASES = arrayOf("myEmail")
        private val SELF_USER_UNIQUE_USERNAME_ALIASES = arrayOf("myUniqueUsername")

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
        this.useSpecialEmail = if (Backend.getDefault() == null || Backend.getDefault()?.hasInbucketSetup()==true) {
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
    private fun generateIndexedEmail(uniqueUserName:String,userNumber: Int): String {
        return "$uniqueUserName+$userNumber@wire.com"
    }



    private fun setUserDefaults(user: ClientUser, userIdx: Int) {
        if (useSpecialEmail) {
            user.email = generateIndexedEmail(user.uniqueUsername?:user.email.orEmpty(),userIdx+1)
            user.emailPassword = UUID.randomUUID().toString()
        }
        val nameAliases = arrayOf(NAME_ALIAS_TEMPLATE.apply(userIdx + 1))
        val firstNameAliases = arrayOf(FIRSTNAME_ALIAS_TEMPLATE.apply(userIdx + 1))
        val passwordAliases = arrayOf(PASSWORD_ALIAS_TEMPLATE.apply(userIdx + 1))
        val emailAliases = arrayOf(EMAIL_ALIAS_TEMPLATE.apply(userIdx + 1))
        val uniqueUsernameAliases = arrayOf(UNIQUE_USERNAME_ALIAS_TEMPLATE.apply(userIdx + 1))
        setClientUserAliases(user, nameAliases, firstNameAliases, passwordAliases, emailAliases, uniqueUsernameAliases)
    }


    private fun syncCreatedState(countOfUsersToBeAdded: Int): List<ClientUser> {
        if (countOfUsersToBeAdded <= 0) {
            return emptyList()
        }
        val createdUsers = usersMap[UserState.Created] ?: mutableListOf()
        val nonCreatedUsers = usersMap[UserState.NotCreated] ?: emptyList()
        val usersToBeAdded = nonCreatedUsers.subList(0, countOfUsersToBeAdded)
        createdUsers.addAll(usersToBeAdded)
        val restOfNonCreatedUsers = nonCreatedUsers.subList(countOfUsersToBeAdded, nonCreatedUsers.size)
        usersMap[UserState.NotCreated] = restOfNonCreatedUsers.toMutableList()
        return Collections.unmodifiableList(usersToBeAdded)
    }

    fun getAllUsers(): List<ClientUser> {
        val allUsers = ArrayList<ClientUser>()
        allUsers.addAll(usersMap[UserState.Created] ?: emptyList())
        allUsers.addAll(usersMap[UserState.NotCreated] ?: emptyList())
        return Collections.unmodifiableList(allUsers)
    }

    fun getAllTeamOwners(): Set<ClientUser> = getAllUsers().stream().filter { it.isTeamOwner }.collect(Collectors.toSet())

    @Throws(NoSuchUserException::class)
    fun findUserByEmailOrName(searchStr: String): ClientUser =
        findUserBy(searchStr, arrayOf(FindBy.EMAIL_ALIAS, FindBy.NAME_ALIAS))

    @Throws(NoSuchUserException::class)
    fun findUserByPasswordAlias(alias: String): ClientUser = findUserBy(alias, arrayOf(FindBy.PASSWORD_ALIAS))

    @Throws(NoSuchUserException::class)
    fun findUserByNameOrNameAlias(alias: String): ClientUser =
        findUserBy(alias, arrayOf(FindBy.NAME, FindBy.NAME_ALIAS))

    @Throws(NoSuchUserException::class)
    fun findUserByFirstNameOrFirstNameAlias(alias: String): ClientUser =
        findUserBy(alias, arrayOf(FindBy.FIRSTNAME, FindBy.FIRSTNAME_ALIAS))

    @Throws(NoSuchUserException::class)
    fun findUserByEmailOrEmailAlias(alias: String): ClientUser =
        findUserBy(alias, arrayOf(FindBy.EMAIL, FindBy.EMAIL_ALIAS))

    @Throws(NoSuchUserException::class)
    fun findUserByUniqueUsernameAlias(alias: String): ClientUser =
        findUserBy(alias, arrayOf(FindBy.UNIQUE_USERNAME, FindBy.UNIQUE_USERNAME_ALIAS))

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
        val trimmedSearchStr = searchStr.trim()
        for (user in getAllUsers()) {
            val aliases = when (findByCriteria) {
                FindBy.NAME_ALIAS -> user.nameAliases
                FindBy.EMAIL_ALIAS -> user.emailAliases
                FindBy.PASSWORD_ALIAS -> user.passwordAliases
                FindBy.UNIQUE_USERNAME_ALIAS -> user.uniqueUsernameAliases
                FindBy.NAME -> if (user.name.equals(trimmedSearchStr, ignoreCase = true)) return user else emptySet()
                FindBy.FIRSTNAME -> if (user.firstName.equals(trimmedSearchStr, ignoreCase = true)) return user else emptySet()
                FindBy.EMAIL -> if (user.email.equals(trimmedSearchStr, ignoreCase = true)) return user else emptySet()
                FindBy.PASSWORD -> if (user.password == searchStr) return user else emptySet()
                FindBy.UNIQUE_USERNAME -> if (user.uniqueUsername.equals(trimmedSearchStr, ignoreCase = true)) return user else emptySet()
                else -> throw RuntimeException("Unknown FindBy criteria $findByCriteria")
            }
            if (aliases.any { it.equals(trimmedSearchStr, ignoreCase = true) }) {
                return user
            }
        }
        throw NoSuchUserException("User '$searchStr' could not be found by '${findByCriteria}'")
    }

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

    fun getListByAliases(srcStr: String, vararg findByAliasTypes: FindBy): List<String> {
        require(findByAliasTypes.isNotEmpty()) { "At least one replacement type should be provided" }
        val result = ArrayList<String>()
        for (dstUser in getAllUsers()) {
            for (aliasType in findByAliasTypes) {
                val (aliases, replacement) = when (aliasType) {
                    FindBy.NAME_ALIAS -> dstUser.nameAliases to dstUser.name
                    FindBy.EMAIL_ALIAS -> dstUser.emailAliases to dstUser.email
                    FindBy.PASSWORD_ALIAS -> dstUser.passwordAliases to dstUser.password
                    FindBy.UNIQUE_USERNAME_ALIAS -> dstUser.uniqueUsernameAliases to dstUser.uniqueUsername
                    else -> throw IllegalArgumentException("Unsupported alias type '${aliasType.name}'")
                }
                val srcList = srcStr.split(", ")
                for (alias in srcList) {
                    if (aliases.contains(alias)) {
                        result.add(replacement.orEmpty())
                    }
                }
            }
        }
        return result
    }

    private fun performParallelUsersCreation(
        usersToCreate: List<ClientUser>,
        userCreationFunc: Function<ClientUser, ClientUser>
    ): List<ClientUser> {
        usersToCreate.parallelStream().forEach { userCreationFunc.apply(it) }
        return Collections.unmodifiableList(usersToCreate)
    }

    // ! Mutates the users in the list
    private fun generatePersonalUsers(usersToCreate: List<ClientUser>, backend: Backend): List<ClientUser> =
        performParallelUsersCreation(usersToCreate, Function { usr ->
            usr.backendName = backend.name
            backend.createPersonalUserViaBackend(usr)
        })

    // ! Mutates the users in the list
    private fun generateWirelessUsers(usersToCreate: List<ClientUser>, backend:Backend): List<ClientUser> =
        performParallelUsersCreation(usersToCreate, Function { usr ->
            usr.backendName = backend.name
            backend.createPersonalUserViaBackend(usr)
        })

    // ! Mutates the users in the list
    private fun generateTeamMembers(
        membersToAdd: List<ClientUser>,
        teamOwner: ClientUser,
        teamId: String,
        membersHaveHandles: Boolean,
        role: TeamRoles,
        backend: Backend,
        context: Context
    ): List<ClientUser> = performParallelUsersCreation(membersToAdd, Function { usr ->
        usr.backendName = backend.name
        backend.createTeamUserViaBackend(teamOwner, teamId, usr, true, membersHaveHandles, role,context)
    })

    fun getCreatedUserss():List<ClientUser> {
        return Collections.unmodifiableList(this.usersMap.get(UserState.Created));
    }
    val createdUsers:List<ClientUser>
        get() = getCreatedUserss()
    private fun verifyUsersCountSatisfiesConstraints(countOfUsersToBeCreated: Int) {
        if (countOfUsersToBeCreated + createdUsers.size > MAX_USERS_IN_TEAM) {
            throw TooManyUsersToCreateException(
                "Cannot create $countOfUsersToBeCreated more users, " +
                        "because the maximum allowed number of available users is $MAX_USERS_IN_TEAM"
            )
        }
    }

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

    fun createWirelessUsers(users: List<ClientUser>, backend: Backend): List<ClientUser> {
        verifyUsersCountSatisfiesConstraints(users.size)
        return generateWirelessUsers(users, backend)
    }

    fun createTeamMembers(
        teamOwner: ClientUser,
        teamId: String,
        members: List<ClientUser>,
        membersHaveHandles: Boolean,
        role: TeamRoles,
        backend: Backend,
        context:Context
    ): List<ClientUser> {
        verifyUsersCountSatisfiesConstraints(members.size)
        return generateTeamMembers(members, teamOwner, teamId, membersHaveHandles, role, backend,context)
    }

    fun createXPersonalUsers(count: Int, backend: Backend) {
        verifyUsersCountSatisfiesConstraints(count)
        generatePersonalUsers(syncCreatedState(count), backend)
    }

    fun createPersonalUsersByAliases(nameAliases: List<String>, backend: Backend): List<ClientUser> {
        val usersToBeCreated = nameAliases.stream()
            .map { findUserByNameOrNameAlias(it) }
            .collect(Collectors.toList())
        verifyUsersCountSatisfiesConstraints(usersToBeCreated.size)
        generatePersonalUsers(usersToBeCreated, backend)
        return Collections.unmodifiableList(usersToBeCreated)
    }

    fun createTeamOwnerByAlias(nameAlias: String, teamName: String, locale: String, updateHandle: Boolean, backend: Backend, context:Context) {
        verifyUsersCountSatisfiesConstraints(1)
        var owner = findUserByNameOrNameAlias(nameAlias)
        WireTestLogger.getLog("Hello").info(owner.toString()+"innnnn")
        owner = runBlocking {   backend.createTeamOwnerViaBackend(owner, teamName, locale, updateHandle, context)}
        owner.backendName = backend.name
        // remember all owners to later be able to delete all created teams
        owner.isTeamOwner = true
    }

    fun getSelfUserOrThrowError(): ClientUser = selfUser ?:
    throw SelfUserNotDefinedException("Self user should be defined in some previous step!")

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
        usersMap[UserState.Created]?.let { createdUsers ->
            if (createdUsers.contains(user)) {
                return createdUsers.indexOf(user)
            }
            createdUsers.add(user)
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
