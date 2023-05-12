package com.example.nextface_android.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.nextface_android.R

class AuthenticationActivity : AppCompatActivity(R.layout.activity_authentication) {
    private lateinit var navCtrl: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authNavHostFragment = supportFragmentManager
            .findFragmentById(R.id.authNavigation) as NavHostFragment
        navCtrl = authNavHostFragment.navController
        setupActionBarWithNavController(navCtrl)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navCtrl.navigateUp() || super.onSupportNavigateUp()
    }
}