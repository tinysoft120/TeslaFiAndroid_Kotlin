package net.leveugle.teslatokens.ui.mainactivity

import net.leveugle.teslatokens.utils.MyLog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import net.leveugle.teslatokens.R
import net.leveugle.teslatokens.ui.login.LoginActivity
import android.content.Intent
import net.leveugle.teslatokens.data.login.LoginDataSource
import com.google.android.material.textfield.TextInputLayout
import android.text.format.DateUtils
import android.annotation.SuppressLint
import android.text.TextUtils
import android.content.ClipData
import android.content.ClipboardManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import net.leveugle.teslatokens.BuildConfig
import net.leveugle.teslatokens.data.Result
import net.leveugle.teslatokens.data.login.LoginResponseListener
import net.leveugle.teslatokens.data.login.Session
import java.util.*

class MainActivity : AppCompatActivity() {
    private val loadingProgressBar: ProgressBar by lazy { findViewById(R.id.loading) }
    private val refreshButton: Button by lazy { findViewById(R.id.main_refresh_button) }

    private val loginDataSource: LoginDataSource by lazy { LoginDataSource.getInstance(this) }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        MyLog.d(TAG, "onCreate")
        setContentView(R.layout.main_activity)
        refreshButton.setOnClickListener {
            MyLog.d(TAG, "refreshButton onClick")
            if (Date().time / 1000 < loginDataSource.session!!.createdAt.toLong()) {
                MyLog.d(TAG, "refreshButton onClick too early")
                Toast.makeText(this@MainActivity, R.string.main_refresh_too_early, Toast.LENGTH_LONG).show()
            }
        }
        refreshButton.isEnabled = false
        findViewById<TextView>(R.id.version).text =
            getString(R.string.version, BuildConfig.VERSION_NAME)
    }

    override fun onResume() {
        super.onResume()
        MyLog.d(TAG, "onResume appCode:16 appVersion:1.2.1")
        if (loginDataSource.session == null) {
            MyLog.d(TAG, "onResume session null")
            startActivity(Intent(this, LoginActivity::class.java))
            setResult(RESULT_OK)
            finish()
            return
        }
        populateFields()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_logged_in_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.main_menu_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun populateFields() {
        MyLog.i(TAG, "populateFields")
        val session = loginDataSource.session
        initEditTextWithToken(findViewById(R.id.copy_tokens_owner_api_token), session!!.accessToken)
        findViewById<TextInputLayout>(R.id.layout_copy_tokens_owner_api_token).helperText =
            getString(
                R.string.copy_tokens_dialog_owner_at_expire,
                DateUtils.formatDateTime(this,
                    "${session.createdAt + session.expiresIn}000".toLong(),
                    DateUtils.FORMAT_SHOW_DATE and DateUtils.FORMAT_SHOW_YEAR and DateUtils.FORMAT_SHOW_TIME
                )
            )
        initEditTextWithToken(
            findViewById(R.id.copy_tokens_sso_refresh_token),
            session.refreshToken
        )
    }

    private fun logout() {
        MyLog.i(TAG, "logout")
        loginDataSource.logout()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initEditTextWithToken(editText: EditText, token: String?) {
        editText.setText(token)
        editText.inputType = EditorInfo.TYPE_NULL
        editText.setOnTouchListener { _, event ->
            MyLog.d(TAG, "onTouch")
            if (event.action == 1) {
                if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_LTR) {
                    if (event.rawX >= (editText.right - editText.compoundDrawables[2].bounds.width()).toFloat()) {
                        val manager: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        manager.setPrimaryClip(ClipData.newPlainText("net.leveugle.teslatokens.tokens", token))
                        Toast.makeText(applicationContext, "Copied into clipboard", Toast.LENGTH_SHORT).show()
                        return@setOnTouchListener true
                    }
                } else if (event.rawX <= (editText.left + editText.compoundDrawables[0].bounds.width()).toFloat()) {
                    val manager: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    manager.setPrimaryClip(ClipData.newPlainText("net.leveugle.teslatokens.tokens", token))
                    Toast.makeText(applicationContext, "Copied into clipboard", Toast.LENGTH_SHORT).show()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun refreshToken() {
        MyLog.d(TAG, "refreshToken")
        if (Date().time / 1000 < loginDataSource.session!!.createdAt) {
            MyLog.d(TAG, "refreshToken too early")
            Toast.makeText(this, R.string.main_refresh_too_early, Toast.LENGTH_LONG).show()
            return
        }
        refreshButton.isEnabled = false
        loadingProgressBar.visibility = View.VISIBLE
        loginDataSource.refreshSession(object : LoginResponseListener {
            override fun onError(error: Result.Error) {
                MyLog.e(TAG, "refreshToken onError", error.error)
                Toast.makeText(this@MainActivity, R.string.main_refresh_error, Toast.LENGTH_LONG)
                    .show()
                refreshButton.isEnabled = true
                loadingProgressBar.visibility = View.INVISIBLE
            }

            override fun onResponse(success: Result.Success<Session>) {
                MyLog.d(TAG, "refreshToken onResponse")
                populateFields()
                loadingProgressBar.visibility = View.INVISIBLE
            }
        })

    }

    companion object {
        private const val TAG = "MainActivity"
    }
}