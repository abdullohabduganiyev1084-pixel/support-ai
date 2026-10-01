package com.supportai.assistant.ui

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.supportai.assistant.R

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var tvToolbarTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        tvToolbarTitle = findViewById(R.id.tv_toolbar_title)

        val btnMenu = findViewById<ImageView>(R.id.btn_menu)
        btnMenu.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        setupDrawerNavigation()

        // Boshlang'ich fragment sifatida HomeFragment yuklash
        if (savedInstanceState == null) {
            loadFragment(HomeFragment(), getString(R.string.nav_main))
        }
    }

    private fun setupDrawerNavigation() {
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment(), getString(R.string.nav_main))
                }
                R.id.nav_permissions -> {
                    loadFragment(PermissionsFragment(), getString(R.string.nav_permissions))
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment(), getString(R.string.nav_settings))
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    fun loadFragment(fragment: Fragment, title: String) {
        tvToolbarTitle.text = title
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
