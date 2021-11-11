package net.leveugle.teslatokens.data.login

import org.json.JSONObject
import org.json.JSONException
import com.google.gson.JsonObject
import net.leveugle.teslatokens.utils.MyLog
import kotlin.Throws

class Session {
    @JvmField var accessToken: String = ""
    @JvmField var refreshToken: String? = null
    @JvmField var createdAt: Int = 0
    @JvmField var expiresIn: Int = 0
    @JvmField var issuer: String? = null

    internal constructor(obj: JSONObject) {
        try {
            accessToken = obj.getString("access_token")
            if (obj.has("refresh_token")) {
                refreshToken = obj.getString("refresh_token")
            }
            if (obj.has("created_at")) {
                createdAt = obj.getInt("created_at")
            }
            if (obj.has("expires_in")) {
                expiresIn = obj.getInt("expires_in")
            }
            if (obj.has("issuer")) {
                issuer = obj.getString("issuer")
            }
        } catch (e: JSONException) {
            MyLog.e("Session", "error while loading saved session", e)
        }
    }

    internal constructor(obj: JsonObject) {
        accessToken = obj["access_token"].asString
        if (obj.has("refresh_token")) {
            refreshToken = obj["refresh_token"].asString
        }
        if (obj.has("created_at")) {
            createdAt = obj["created_at"].asInt
        }
        if (obj.has("expires_in")) {
            expiresIn = obj["expires_in"].asInt
        }
        if (obj.has("issuer")) {
            issuer = obj["issuer"].asString
        }
    }

    constructor(
        accessToken: String,
        refreshToken: String?,
        createdAt: Int,
        expiresIn: Int,
        issuer: String?
    ) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.createdAt = createdAt
        this.expiresIn = expiresIn
        this.issuer = issuer
    }

    @Throws(JSONException::class)
    fun toJSON(): JSONObject {
        val obj = JSONObject()
        obj.put("access_token", accessToken)
        obj.put("refresh_token", refreshToken)
        obj.put("created_at", createdAt)
        obj.put("expires_in", expiresIn)
        obj.put("issuer", issuer)
        return obj
    }
}