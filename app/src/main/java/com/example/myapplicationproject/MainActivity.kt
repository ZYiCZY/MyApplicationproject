package com.example.myapplicationproject

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView

// 【準時出門助手】主 Activity - 承載三大主分頁 (今天、紀錄、設定)
class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var detailContainer: View

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            showNotificationPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        viewPager = findViewById(R.id.viewPager)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        detailContainer = findViewById(R.id.detailContainer)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> HomeFragment()
                    1 -> RecordsFragment()
                    2 -> ProfileFragment()
                    else -> HomeFragment()
                }
            }
        }

        // ViewPager2 滑動切換 BottomNavigationView 狀態
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bottomNavigation.menu.getItem(position).isChecked = true
            }
        })

        // BottomNavigationView 點擊切換 ViewPager2 頁面
        bottomNavigation.setOnItemSelectedListener { item ->
            if (detailContainer.visibility == View.VISIBLE) {
                closeDetailFragment()
            }

            when (item.itemId) {
                R.id.nav_home -> viewPager.currentItem = 0
                R.id.nav_records -> viewPager.currentItem = 1
                R.id.nav_profile -> viewPager.currentItem = 2
            }
            true
        }

        // 處理返回鍵 (Back Press)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (detailContainer.visibility == View.VISIBLE) {
                    closeDetailFragment()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun showNotificationPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要通知權限")
            .setMessage("「準時出門助手」需要通知權限，才能在對的時間提示您出門與提醒必帶物品。如需此功能，請在系統設定中開啟通知。")
            .setPositiveButton("確定", null)
            .show()
    }

    fun openDetailFragment(fragment: Fragment) {
        val currentDetail = supportFragmentManager.findFragmentById(R.id.detailContainer)
        if (currentDetail != null && currentDetail::class == fragment::class) {
            return
        }

        viewPager.visibility = View.GONE
        detailContainer.visibility = View.VISIBLE

        supportFragmentManager.beginTransaction()
            .replace(R.id.detailContainer, fragment)
            .addToBackStack(fragment::class.java.simpleName)
            .commit()
    }

    fun closeDetailFragment() {
        detailContainer.visibility = View.GONE
        viewPager.visibility = View.VISIBLE
        supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
}
