package com.example.nextface_android.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.nextface_android.R

class RegisterActivity : AppCompatActivity(R.layout.activity_register) {
    private lateinit var navCtrl: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val regNavHostFragment = supportFragmentManager
            .findFragmentById(R.id.regNavigation) as NavHostFragment
        navCtrl = regNavHostFragment.navController
        setupActionBarWithNavController(navCtrl)
    }

//    override fun onSupportNavigateUp(): Boolean {
//        return super.onSupportNavigateUp()
//    }
}