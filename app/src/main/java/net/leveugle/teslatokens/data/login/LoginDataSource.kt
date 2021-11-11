package net.leveugle.teslatokens.data.login

import android.content.Context
import net.leveugle.teslatokens.utils.MyLog
import com.android.volley.RequestQueue
import android.content.SharedPreferences
import org.json.JSONException
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject
import kotlin.Throws
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import net.leveugle.teslatokens.data.Result
import net.leveugle.teslatokens.utils.TaskRunner
import java.lang.Exception
import java.nio.charset.StandardCharsets

class LoginDataSource private constructor(context: Context) {
    private val queue: RequestQueue = Volley.newRequestQueue(context)
    var session: Session? = null
        private set
    private val sharedPref = context.getSharedPreferences("net.leveugle.teslatokens.LoginDataSource", Context.MODE_PRIVATE)

    fun setLoggedInUser(session: Session) {
        this.session = session
        try {
            sharedPref.edit().apply {
                putString(keySession, session.toJSON().toString())
                commit()
            }
        } catch (e: JSONException) {
            MyLog.e(TAG, "error while saving session", e)
        }
    }

    fun logout() {
        MyLog.i(TAG, "logout")
        sharedPref.edit().apply {
            remove(keySession)
            commit()
        }
        session = null
    }

    fun refreshSession(listener: LoginResponseListener) {
        MyLog.d(TAG, "refreshSession start")
        val session = session
        if (session?.refreshToken == null || session.issuer == null || "" == session.refreshToken || "" == session.issuer) {
            MyLog.d(TAG, "refreshSession end Cannot refresh session")
            listener.onError(Result.Error("Cannot refresh session"))
            return
        }
        queue.add(object : StringRequest(
            Method.POST,
            "https://${session.issuer}/oauth2/v3/token",
            Response.Listener { response ->
                MyLog.d(TAG, "session refreshed")
                try {
                    val newSession = Session(JSONObject(response))
                    newSession.issuer = session.issuer
                    val logic = TeslaLoginLogic()
                    TaskRunner().executeAsync(
                        { logic.obtainOwnerAPITokenFromSSOToken(newSession) },
                        object : TaskRunner.Callback<Session> {

                            override fun onComplete(result: Session) {
                                val user = logic.buildSessionFrom(result, result)
                                setLoggedInUser(user)
                                MyLog.i(TAG, "refresh OK")
                                listener.onResponse(Result.Success(user))
                            }

                            override fun onError(exc: Exception) {
                                MyLog.e(TAG, "error on refresh call", exc)
                                setLoggedInUser(session)
                            }
                        }
                    )
                } catch (e: JSONException) {
                    MyLog.e(TAG, "error while refreshing onResponse", e)
                    setLoggedInUser(session)
                    listener.onError(Result.Error(e))
                }
            },
            Response.ErrorListener { error ->
                MyLog.e(TAG, "session refresh error", error)
                setLoggedInUser(session)
                if (error == null) {
                    listener.onError(Result.Error("error while refreshing session"))
                    return@ErrorListener
                }
                if (error.networkResponse != null) {
                    MyLog.d(TAG, "session refresh error statusCode:" + error.networkResponse.statusCode)
                    if (error.networkResponse.allHeaders != null) {
                        MyLog.d(TAG, "session refresh error headers:" + error.networkResponse.allHeaders.toString())
                    }
                    if (error.networkResponse.data != null) {
                        MyLog.d(TAG,"session refresh error body:" + String(error.networkResponse.data, StandardCharsets.UTF_8))
                    } else {
                        MyLog.d(TAG, "session refresh error body is null")
                    }
                }
                listener.onError(Result.Error(error.toString()))
            }) {

            override fun getBodyContentType(): String {
                return "application/x-www-form-urlencoded"
            }

            @Throws(AuthFailureError::class)
            override fun getBody(): ByteArray {
                return ("client_id=ownerapi&scope=openid email offline_access&grant_type=refresh_token&refresh_token=${session.refreshToken}").toByteArray()
            }
        })
    }

    companion object {
        private const val TAG = "LoginDataSource"

        private var sInstance: LoginDataSource? = null
        private const val keySession = "keySession"

        @JvmStatic
        fun getInstance(context: Context): LoginDataSource {
            if (sInstance == null) {
                sInstance = LoginDataSource(context)
            }
            return sInstance!!
        }
    }

    init {
        val session = sharedPref.getString(keySession, null)
        if (session != null) {
            try {
                setLoggedInUser(Session(JSONObject(session)))
            } catch (e: JSONException) {
                MyLog.e(TAG, "error while loading saved session", e)
            }
        }
    }
}