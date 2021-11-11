package net.leveugle.teslatokens.data.login

import net.leveugle.teslatokens.data.Result

interface LoginResponseListener {
    fun onError(error: Result.Error)
    fun onResponse(success: Result.Success<Session>)
}