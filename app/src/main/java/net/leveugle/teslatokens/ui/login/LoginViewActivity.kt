package net.leveugle.teslatokens.ui.login

import net.leveugle.teslatokens.utils.MyLog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import net.leveugle.teslatokens.R
import net.leveugle.teslatokens.data.login.TeslaLoginLogic
import net.leveugle.teslatokens.data.login.LoginDataSource
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.view.View
import net.leveugle.teslatokens.data.login.Session
import net.leveugle.teslatokens.utils.TaskRunner
import java.lang.Exception

class LoginViewActivity : AppCompatActivity() {
    private val logic: TeslaLoginLogic by lazy { TeslaLoginLogic() }
    private val loginDataSource: LoginDataSource by lazy { LoginDataSource.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_view)

        findViewById<WebView>(R.id.login_webview).apply {
            settings.javaScriptEnabled = true
            webViewClient = MyWebViewClient()
            loadUrl(logic.authorizeHttpUrl)
        }
    }

    private fun doLogin(code: String) {
        findViewById<View>(R.id.login_loading).visibility = View.VISIBLE
        findViewById<View>(R.id.login_webview).visibility = View.INVISIBLE
        TaskRunner().executeAsync(
            { logic.login(code).also { loginDataSource.setLoggedInUser(it) } },
            object : TaskRunner.Callback<Session?> {

                override fun onComplete(result: Session?) {
                    if (result == null) {
                        setResult(RESULT_CANCELED)
                    } else {
                        setResult(RESULT_OK)
                    }
                    finish()
                }

                override fun onError(exc: Exception) {
                    MyLog.e(TAG, "error on login call", exc)
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
        )
    }

    companion object {
        private const val TAG = "LoginViewActivity"
    }

    private inner class MyWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val loginHint = request.url.getQueryParameter("login_hint")
            if (!loginHint.isNullOrEmpty()) {
                logic.teslaEnv = request.url.host!!
            }
            val code = request.url.getQueryParameter("code")
            if (!code.isNullOrEmpty()) {
                doLogin(code)
            }
            return false
        }
    }
}