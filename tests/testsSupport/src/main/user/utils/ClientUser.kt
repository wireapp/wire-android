
package user.utils

import net.datafaker.Faker
import net.datafaker.providers.base.Text
import net.datafaker.providers.base.Text.DIGITS
import net.datafaker.providers.base.Text.EN_LOWERCASE
import net.datafaker.providers.base.Text.EN_UPPERCASE
import user.UserClient
import java.time.Duration
import java.util.Objects
import java.util.concurrent.Callable

class ClientUser {

    var accessCredentials: AccessCredentials? = null

    var id: String? = null
    var name: String? = null
    var firstName: String? = null
    var lastName: String? = null
    var password: String? = null
    var email: String? = null
    val emailAliases: MutableSet<String> = HashSet()
    var emailPassword: String? = null
    val nameAliases: MutableSet<String> = HashSet()
    val firstNameAliases: MutableSet<String> = HashSet()
    var uniqueUsername: String? = null
    val uniqueUsernameAliases: MutableSet<String> = HashSet()
    val passwordAliases: MutableSet<String> = HashSet()

    //    var accentColor: AccentColor = AccentColor.Undefined
    var teamId: String? = null
    var expiresIn: Duration? = null
    var serviceProviderId: String? = null
    var sso: Boolean = false
    var scim: Boolean = false
    var backendName: String? = null
    var hardcoded: Boolean = false
    var getUserIdThroughOwner: Callable<String>? = null
    var isTeamOwner: Boolean = false
    var registeredLegalHoldServices: Boolean = false
    var verificationCode: String? = null
    var activationCode: String? = null
    val customSpecialSymbols: String = "!@#$"

    @Suppress("MagicNumber")
    constructor() {
        val faker = Faker()
        firstName = faker.name().firstName()
        lastName = faker.name().lastName()
        // Reroll last name if it contains a quote to not break locator checks later
        while (lastName?.contains("'") == true) {
            lastName = faker.name().lastName()
        }
        name = "$firstName $lastName"
        password = faker.text().text(
            Text.TextSymbolsBuilder.builder()
                .len(8)
                .with(EN_LOWERCASE, 3)
                .with(EN_UPPERCASE, 1)
                .with(customSpecialSymbols, 1)
                .with(DIGITS, 1).build()
        )
        uniqueUsername = sanitizedRandomizedHandle(lastName)
        val domain = "wire.engineering"
        email = "$uniqueUsername@$domain"
        emailPassword = UserClient.RandomStringGenerator.randomWithSpecialChars(12)
    }

    constructor(firstName: String, lastName: String, email: String, password: String) {
        this.firstName = firstName
        this.lastName = lastName
        this.name = "$firstName $lastName"
        this.email = email
        this.password = password
    }

    override fun toString(): String =
        "Hello, my name is $name and my email is $email and password is $password and emailPassword is $emailPassword"

    override fun equals(other: Any?): Boolean =
        (other is ClientUser) && other.email == email

    fun isSSOUser(): Boolean = sso
    fun setUserIsSSOUser() {
        sso = true
    }

    fun isManagedBySCIM(): Boolean = scim
    fun isHardcoded(): Boolean = hardcoded
    fun hasRegisteredLegalHoldService(): Boolean = registeredLegalHoldServices
    fun setRegisteredLegalHoldService(value: Boolean) {
        registeredLegalHoldServices = value
    }

    fun hasServiceProvider(): Boolean = serviceProviderId != null

    companion object {
        @Suppress("MagicNumber")
        fun sanitizedRandomizedHandle(derivative: String?): String =
            "${derivative}${UserClient.RandomStringGenerator.randomAlphabetic(10)}"
                .lowercase()
    }

    override fun hashCode(): Int = Objects.hash(
        sso, isTeamOwner, accessCredentials, id, name,
        firstName, lastName, password, email, emailAliases,
        emailPassword, nameAliases, firstNameAliases, uniqueUsername,
        uniqueUsernameAliases, passwordAliases, teamId, expiresIn,
        backendName, verificationCode, activationCode
    )
}
