import android.content.Context
import backendUtils.BackendSetupHelper
import backendUtils.BackendClient.Companion.AUTHORIZATION
import backendUtils.BackendClient.Companion.accept
import backendUtils.BackendClient.Companion.applicationJson
import backendUtils.BackendClient.Companion.contentType
import backendUtils.BackendClient.Companion.loadBackend
import backendUtils.sso.createIdentityProvider
import backendUtils.sso.createIdentityProviderV2
import backendUtils.team.getTeamByName
import backendUtils.team.getTeamMembers
import backendUtils.user.getUserNameByID
import keycloak.KeycloakApiClient
import kotlinx.coroutines.runBlocking
import network.NetworkBackendClient
import network.NumberSequence
import network.RequestOptions
import okta.OktaApiClient
import org.json.JSONObject
import backendUtils.scim.ScimClient
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import user.utils.component.setManagedBySCIM
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Callable
import java.util.UUID

object SSOServiceHelper {

    private var currentIdentityProviderId = ""
    private lateinit var clientUserManager: ClientUserManager

    // Registers the shared test user manager used by the SSO setup helpers.
    fun initialize(manager: ClientUserManager) {
        this.clientUserManager = manager
    }

    // Creates a backend team owner and wires the team to an Okta identity provider.
    suspend fun createOktaSsoTeamOwner(
        context: Context,
        ownerNameAlias: String,
        teamName: String,
        client: OktaApiClient
    ) {
        val owner = clientUserManager.findUserByNameOrNameAlias(ownerNameAlias)
        val backend = loadBackend(owner.backendName ?: "STAGING")
        BackendSetupHelper(clientUserManager).createTeamOwnerByAlias(
            ownerNameAlias,
            teamName,
            "en_US",
            updateHandle = true,
            backend,
            context
        )
        enableSsoFeature(owner, teamName)
        val finalizeUrl = OktaApiClient.getFinalizeUrlDependingOnBackend(backend.backendUrl)
        client.createApplication(owner.name + " " + teamName + UUID.randomUUID().toString(), finalizeUrl, context)

        val metadata = client.getApplicationMetadata()
        currentIdentityProviderId =
            backend.createIdentityProvider(
                owner,
                metadata
            )
    }

    // Creates a backend team owner and wires the team to a Keycloak identity provider.
    suspend fun createKeycloakSsoTeamOwner(
        context: Context,
        ownerNameAlias: String,
        teamName: String,
        client: KeycloakApiClient
    ) {
        val owner = clientUserManager.findUserByNameOrNameAlias(ownerNameAlias)
        val backend = loadBackend(owner.backendName ?: "mobtown-lemon")
        BackendSetupHelper(clientUserManager).createTeamOwnerByAlias(
            ownerNameAlias,
            teamName,
            "en_US",
            updateHandle = true,
            backend,
            context
        )
        enableSsoFeature(owner, teamName)
        val dstTeam = backend.getTeamByName(owner, teamName)
        currentIdentityProviderId = backend.createIdentityProviderV2(owner, client.getMetadata())
        client.createSamlClient(dstTeam.id)
    }

    @Suppress("TooGenericExceptionThrown", "MagicNumber")
    // Creates SSO users in Okta and prepares their matching test users for Wire login.
    suspend fun addOktaSsoUsers(
        ownerNameAlias: String,
        userNameAliases: String,
        oktaApiClient: OktaApiClient
    ) {
        val aliases = clientUserManager.splitAliases(userNameAliases)
        for (userNameAlias in aliases) {
            val user = clientUserManager.findUserByNameOrNameAlias(userNameAlias)

            if (clientUserManager.isUserCreated(user)) {
                throw Exception(
                    "Cannot add user with alias $userNameAlias to SSO team because user is already created"
                )
            }

            user.password = "SSO${user.password}"
            user.name = user.email

            // Backend generates the unique username through the email. We try to predict this here:
            var uniqueUsername = user.email?.replace(Regex("[^A-Za-z0-9]"), "")
            if ((uniqueUsername?.length ?: 0) > 21) {
                uniqueUsername = uniqueUsername?.substring(0, 21)
            }
            user.uniqueUsername = uniqueUsername

            val ownerBackendName = clientUserManager.findUserByNameOrNameAlias(ownerNameAlias).backendName
            user.backendName = ownerBackendName
            user.setUserIsSSOUser()

            syncUserIdsForUsersCreatedThroughIdP(ownerNameAlias, user)

            // set backend for added okta users
            user.backendName = ownerBackendName

            val userId = oktaApiClient.createUser(user.name.orEmpty(), user.email.orEmpty(), user.password.orEmpty())
            oktaApiClient.assignUserToApplication(userId)
        }
    }

    @Suppress("TooGenericExceptionThrown", "MagicNumber")
    // Creates Keycloak-backed SSO users without provisioning them as SCIM-managed users in Wire.
    suspend fun addKeycloakSsoUsers(
        ownerNameAlias: String,
        userNameAliases: String,
        keycloakApiClient: KeycloakApiClient
    ) {
        val aliases = clientUserManager.splitAliases(userNameAliases)
        for (userNameAlias in aliases) {
            val user = prepareKeycloakSsoUser(ownerNameAlias, userNameAlias)
            keycloakApiClient.createUser(
                username = user.name.orEmpty(),
                firstName = user.firstName.orEmpty(),
                lastName = user.lastName.orEmpty(),
                email = user.email.orEmpty(),
                password = user.password.orEmpty()
            )
        }
    }

    // Creates Keycloak-backed SSO users and also provisions them as SCIM-managed users in Wire.
    @Suppress("TooGenericExceptionThrown")
    suspend fun addKeycloakSsoUsersWithScim(
        ownerNameAlias: String,
        userNameAliases: String,
        keycloakApiClient: KeycloakApiClient
    ) {
        val owner = clientUserManager.findUserByNameOrNameAlias(ownerNameAlias)
        val backend = loadBackend(owner.backendName.orEmpty())
        val scimClient = ScimClient(backend)
        val aliases = clientUserManager.splitAliases(userNameAliases)

        for (userNameAlias in aliases) {
            val user = prepareKeycloakSsoUser(ownerNameAlias, userNameAlias)

            keycloakApiClient.createUser(
                username = user.email.orEmpty(),
                firstName = user.firstName.orEmpty(),
                lastName = user.lastName.orEmpty(),
                email = user.email.orEmpty(),
                password = user.password.orEmpty()
            )

            user.id = scimClient.insert(owner, user)
            user.setUserIsSSOUser()
            user.setManagedBySCIM()
        }
    }

    @Suppress("TooGenericExceptionThrown", "MagicNumber")
    // Applies the shared SSO user shape expected before the Keycloak user is created or SCIM-provisioned.
    private fun prepareKeycloakSsoUser(
        ownerNameAlias: String,
        userNameAlias: String
    ): ClientUser {
        val user = clientUserManager.findUserByNameOrNameAlias(userNameAlias)

        if (clientUserManager.isUserCreated(user)) {
            throw Exception(
                "Cannot add user with alias $userNameAlias to SSO team because user is already created"
            )
        }

        user.password = "SSO${user.password}"
        user.name = user.email

        var uniqueUsername = user.email?.replace(Regex("[^A-Za-z0-9]"), "")
        if ((uniqueUsername?.length ?: 0) > 21) {
            uniqueUsername = uniqueUsername?.substring(0, 21)
        }
        user.uniqueUsername = uniqueUsername

        user.backendName = clientUserManager.findUserByNameOrNameAlias(ownerNameAlias).backendName
        user.setUserIsSSOUser()

        syncUserIdsForUsersCreatedThroughIdP(ownerNameAlias, user)
        return user
    }

    // Marks which prepared SSO user should be treated as the currently logged-in test user.
    fun setCurrentSsoUser(nameAlias: String) {
        clientUserManager.setSelfUser(clientUserManager.findUserByNameOrNameAlias(nameAlias))
    }

    // Defers Wire user-id lookup until the IdP-created user becomes visible through the team owner account.
    private fun syncUserIdsForUsersCreatedThroughIdP(ownerNameAlias: String, user: ClientUser) {
        user.getUserIdThroughOwner = Callable {
            val asUser = clientUserManager.findUserByNameOrNameAlias(ownerNameAlias)
            val backend = loadBackend(asUser.backendName.orEmpty())
            val teamMembers = backend.getTeamMembers(asUser)

            for (member in teamMembers) {
                val memberId = member.userId
                val memberName = backend.getUserNameByID(backend.domain, memberId, asUser)
                if (user.name == memberName) {
                    return@Callable memberId
                }
            }

            throw IOException(
                "No user ID found for user ${user.email}. Please verify you are using the right Team Owner account"
            )
        }
    }

    // Returns the Wire SSO code that the UI test enters on the login screen.
    fun getSsoCode(): String = "wire-$currentIdentityProviderId"

    @Suppress("MagicNumber")
    // Enables the backend team SSO feature before attaching the external identity provider configuration.
    private fun enableSsoFeature(clientUser: ClientUser, teamName: String) {
        val backend = loadBackend(clientUser.backendName.orEmpty())
        val dstTeam = runBlocking {
            backend.getTeamByName(clientUser, teamName)
        }
        val url = URL("${backend.backendUrl}i/teams/${URLEncoder.encode(dstTeam.id, "UTF-8")}/features/sso")
        val headers = mapOf(
            AUTHORIZATION to backend.basicAuth.getEncoded(),
            accept to applicationJson,
            contentType to applicationJson
        )

        val requestBody = JSONObject().apply {
            put("status", "enabled")
        }

        NetworkBackendClient.sendJsonRequest(
            url = url,
            method = "PUT",
            body = requestBody.toString(),
            headers = headers,
            options = RequestOptions(expectedResponseCodes = NumberSequence.Array(intArrayOf(200))),
        )
    }
}
