package com.example.nextface_android.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.nextface_android.R

class HomeActivity : AppCompatActivity(R.layout.activity_home) {
    private lateinit var navCtrl: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val homeNavHostFragment = supportFragmentManager
            .findFragmentById(R.id.homeNavigation) as NavHostFragment
        navCtrl = homeNavHostFragment.navController
        supportActionBar
    }

    override fun onSupportNavigateUp(): Boolean {
        return navCtrl.navigateUp() || super.onSupportNavigateUp()
    }
}