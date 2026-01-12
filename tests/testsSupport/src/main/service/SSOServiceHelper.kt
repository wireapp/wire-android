import android.content.Context
import backendUtils.BackendClient.Companion.AUTHORIZATION
import backendUtils.BackendClient.Companion.accept
import backendUtils.BackendClient.Companion.applicationJson
import backendUtils.BackendClient.Companion.contentType
import backendUtils.BackendClient.Companion.loadBackend
import backendUtils.team.getTeamByName
import kotlinx.coroutines.runBlocking
import network.NetworkBackendClient
import network.NumberSequence
import network.RequestOptions
import network.WireTestLogger
import okta.OktaApiClient
import org.json.JSONObject
import service.TestServiceHelper
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

object SSOServiceHelper {

    var identityProviderId = ""
    lateinit var usersManager: ClientUserManager

    fun initialize(manager: ClientUserManager) {
        this.usersManager = manager
    }

    suspend fun TestServiceHelper.thereIsASSOTeamOwnerForOkta(
        context: Context,
        ownerNameAlias: String,
        teamName: String,
        client: OktaApiClient
    ) {
        val owner = toClientUser(ownerNameAlias)
        thereIsATeamOwner(context, ownerNameAlias, teamName, true, backend = loadBackend(owner.backendName ?: "STAGING"))
        enableSSOFeature(owner, teamName)
        val backend = loadBackend(owner.backendName.orEmpty())
        val finalizeUrl = OktaApiClient.getFinalizeUrlDependingOnBackend(backend.backendUrl)
        val client = OktaApiClient()
        client.createApplication(owner.name + " " + teamName + UUID.randomUUID().toString(), finalizeUrl, context)

        val metadata = client.getApplicationMetadata()
        identityProviderId =
            backend.createIdentityProvider(
                owner,
                metadata
            )
    }

    @Suppress("TooGenericExceptionThrown", "MagicNumber")
    suspend fun TestServiceHelper.userAddsOktaUser(ownerNameAlias: String, userNameAliases: String, oktaApiClient: OktaApiClient) {
        val aliases = usersManager.splitAliases(userNameAliases)
        for (userNameAlias in aliases) {
            val user = toClientUser(userNameAlias)

            if (usersManager.isUserCreated(user)) {
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

            val ownerBackendName = toClientUser(ownerNameAlias).backendName
            user.backendName = ownerBackendName
            user.setUserIsSSOUser()

            syncUserIdsForUsersCreatedThroughIdP(ownerNameAlias, user)

            // set backend for added okta users
            user.backendName = ownerBackendName

            oktaApiClient.createUser(user.name.orEmpty(), user.email.orEmpty(), user.password.orEmpty())
        }
    }

    fun TestServiceHelper.userXIsMe(nameAlias: String) {
        usersManager.setSelfUser(toClientUser(nameAlias))
    }

    fun getSSOCode(): String = "wire-$identityProviderId".also {
        WireTestLogger.getLog("Test Log").info("The sso code is $it")
    }

    fun clearSSOCode() {
        identityProviderId = ""
    }

    @Suppress("MagicNumber")
    fun enableSSOFeature(clientUser: ClientUser, teamName: String) {
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
