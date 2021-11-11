package net.leveugle.teslatokens.data.login

import com.google.gson.JsonObject
import kotlin.Throws
import com.google.gson.Gson
import android.os.Build
import android.util.Base64
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.lang.RuntimeException
import java.lang.StringBuilder
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.*
import java.util.concurrent.TimeUnit

open class TeslaLoginLogic {
    private val jsonMediaType: MediaType = "application/json; charset=utf-8".toMediaTypeOrNull()!!
    private val gson = Gson()
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.MINUTES)
        .cookieJar(
            JavaNetCookieJar(
                CookieManager().apply { setCookiePolicy(CookiePolicy.ACCEPT_ALL) }
            )
        )
        .build()

    var teslaEnv = DEFAULT_TESLA_ENV
    val authorizeHttpUrl: String
        get() = HttpUrl.Builder()
            .scheme("https")
            .host(teslaEnv)
            .addPathSegment("oauth2")
            .addPathSegment(LOGIN_SSO_VERSION)
            .addPathSegment("authorize")
            .addQueryParameter("client_id", LOGIN_CLIENT_ID)
            .addQueryParameter("redirect_uri", LOGIN_REDIRECT_URI)
            .addQueryParameter("response_type", "code")
            .addQueryParameter("scope", LOGIN_SCOPES)
            .addQueryParameter("state", "TeslaTokens" + randomString())
            .build()
            .toString()

    fun buildSessionFrom(ssoSession: Session, ownApiSession: Session): Session {
        return Session(
            ownApiSession.accessToken,
            ssoSession.refreshToken,
            ownApiSession.createdAt,
            ownApiSession.expiresIn,
            ssoSession.issuer
        )
    }

    @Throws(IOException::class)
    private fun obtainSSOToken(code: String): Session {
        val verifier = generateCodeVerifier()
        val build: HttpUrl = HttpUrl.Builder()
            .scheme("https")
            .host(teslaEnv)
            .addPathSegment("oauth2")
            .addPathSegment(LOGIN_SSO_VERSION)
            .addPathSegment("token")
            .build()
        val parameters = JsonObject()
        parameters.addProperty("grant_type", "authorization_code")
        parameters.addProperty("client_id", LOGIN_CLIENT_ID)
        parameters.addProperty("code_verifier", verifier)
        parameters.addProperty("code", code)
        parameters.addProperty("redirect_uri", LOGIN_REDIRECT_URI)
        val request: Request = Request.Builder()
            .url(build)
            .post(gson.toJson(parameters).toRequestBody(jsonMediaType))
            .build()
        try {
            okHttpClient.newCall(request).execute().use { execute ->
                execute.body.use { body ->
                    failIfNotSuccessful(execute)
                    val jsonObject2 = gson.fromJson(body!!.string(), JsonObject::class.java)
                    failOnError(jsonObject2)
                    val session = Session(jsonObject2)
                    session.issuer = teslaEnv
                    return session
                }
            }
        } catch (th: Throwable) {
            th.addSuppressed(th)
            throw th
        }
    }

    @Throws(IOException::class)
    fun login(code: String): Session {
        val ssoToken = obtainSSOToken(code)
        return buildSessionFrom(ssoToken, obtainOwnerAPITokenFromSSOToken(ssoToken))
    }

    @Throws(IOException::class)
    fun obtainOwnerAPITokenFromSSOToken(session: Session): Session {
        val ownApiUrl = HttpUrl.Builder()
            .scheme("https")
            .host("owner-api.teslamotors.com")
            .addPathSegment("oauth")
            .addPathSegment("token")
            .build()

        val parameters = JsonObject().apply {
            addProperty("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
            addProperty("client_id", CLIENT_ID)
        }
        val request = Request.Builder()
            .url(ownApiUrl)
            .addHeader("Authorization", "Bearer " + session.accessToken)
            .post(gson.toJson(parameters).toRequestBody(jsonMediaType))
            .build()
        try {
            okHttpClient.newCall(request).execute().use { result ->
                failIfNotSuccessful(result)
                return Session(gson.fromJson(result.body!!.string(), JsonObject::class.java))
            }
        } catch (th: Throwable) {
            th.addSuppressed(th)
            throw th
        }
    }

    companion object {
        protected const val DEFAULT_TESLA_ENV = "auth.tesla.com"
        const val CLIENT_ID = "81527cff06843c8634fdc09e8ac0abefb46ac849f38fe1e431c2ef2106796384"
        const val LOGIN_CLIENT_ID = "ownerapi"
        const val LOGIN_REDIRECT_URI = "https://auth.tesla.com/void/callback"
        const val LOGIN_SCOPES = "openid email offline_access"
        const val LOGIN_SSO_VERSION = "v3"

        private fun generateCodeVerifier(): String {
            return encodeBase64(randomString())
        }

        private fun randomString(): String {
            val letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"

            val sb = StringBuilder()
            val random = Random()
            for (i in 0..85) {
                sb.append(letters[random.nextInt(letters.length)])
            }
            return sb.toString()
        }

        private fun encodeBase64(str: String): String {
            val encoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                java.util.Base64.getEncoder().encodeToString(str.toByteArray())
            } else {
                Base64.encodeToString(str.toByteArray(), Base64.DEFAULT)
            }
            return encoded.apply {
                replace("+", "-")
                replace("/", "_")
                replace("=", "")
                trim { it <= ' ' }
            }
        }

        private fun failIfNotSuccessful(response: Response) {
            if (!response.isSuccessful) {
                throw RuntimeException("Request not successful: $response")
            }
        }

        private fun failOnError(obj: JsonObject) {
            obj.getAsJsonObject("error")?.let { error ->
                val errMsg = error["message"].asString
                if (errMsg != null) {
                    throw RuntimeException(errMsg)
                }
            }
        }
    }

}