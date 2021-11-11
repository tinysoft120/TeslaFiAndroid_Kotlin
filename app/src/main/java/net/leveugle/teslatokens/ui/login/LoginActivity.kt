package net.leveugle.teslatokens.ui.login

import androidx.appcompat.app.AppCompatActivity
import android.widget.ProgressBar
import android.os.Bundle
import net.leveugle.teslatokens.R
import android.content.Intent
import android.widget.Toast
import net.leveugle.teslatokens.ui.mainactivity.MainActivity
import android.view.View
import androidx.annotation.StringRes
import net.leveugle.teslatokens.utils.MyLog

class LoginActivity : AppCompatActivity() {
    private lateinit var loadingProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)
        MyLog.i(TAG, "onCreate")
        loadingProgressBar = findViewById(R.id.loading)
        findViewById<View>(R.id.login_using_tesla_account).setOnClickListener {
            loadingProgressBar.visibility = View.VISIBLE
            startActivityForResult(Intent(this@LoginActivity, LoginViewActivity::class.java), 0)
        }
    }

    override fun onResume() {
        super.onResume()
        MyLog.i(TAG, "onResume")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (resultCode) {
            RESULT_OK       -> showLoginOkAndFinish()
            RESULT_CANCELED -> showLoginFailed(R.string.login_failed)
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun showLoginOkAndFinish() {
        Toast.makeText(applicationContext, getString(R.string.succeed), Toast.LENGTH_LONG).show()
        startActivity(Intent(this, MainActivity::class.java))
        setResult(RESULT_OK)
        finish()
    }

    private fun showLoginFailed(@StringRes errMsg: Int) {
        loadingProgressBar.visibility = View.GONE
        Toast.makeText(applicationContext, errMsg, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}