package edu.cit.abelgas.localloop

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import edu.cit.abelgas.localloop.ui.LoginActivity
import edu.cit.abelgas.localloop.util.SharedPreferencesHelper

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MainActivity is just a router.
        // It checks if the user is already logged in and
        // redirects them to the right screen immediately.
        val prefs = SharedPreferencesHelper(this)

        if (prefs.isLoggedIn()) {
            startActivity(Intent(this, edu.cit.abelgas.localloop.ui.DashboardActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        finish() // close MainActivity so it's not in the back stack
    }
}