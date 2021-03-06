package com.example.tiktok_analog.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.tiktok_analog.R
import com.example.tiktok_analog.data.model.User
import com.example.tiktok_analog.ui.menuscreens.fragments.CommentsFragment
import com.example.tiktok_analog.ui.menuscreens.fragments.OpenVideoFragment
import com.example.tiktok_analog.ui.menuscreens.fragments.ProfileFragment
import com.example.tiktok_analog.util.dataclasses.AppConfig
import com.example.tiktok_analog.util.viewpageradapters.TabViewPagerAdapter
import com.example.tiktok_analog.util.viewpageradapters.ViewPagerAdapter
import kotlinx.android.synthetic.main.activity_open_video.*
import kotlinx.android.synthetic.main.fragment_open_video.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject


class OpenVideoActivity : AppCompatActivity() {
    val userData: User
        get() = readUserData()

    private lateinit var requestQueue: RequestQueue

    private val profileFragment: ProfileFragment = ProfileFragment()
    private lateinit var openVideoFragment: OpenVideoFragment
    private val commentsFragment: CommentsFragment = CommentsFragment()

    private lateinit var config: AppConfig
    private var savedLocation: Location? = null

    private var isActivityStopped = false
    private var isAnotherOpenVideoActivityOpened = false

    fun getViewPager2(): ViewPager2 {
        return openVideoFragment.getViewPager2()
    }

    fun updateCommentsFragment() {
        try {
            commentsFragment.updateComments()
        } catch (e: Exception) {
            // will handle this error later (no)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userDataFile = applicationContext.getFileStreamPath("userData")
        if (userDataFile == null && userDataFile?.exists()!!.not()) {
            finish()
        }

        updateLocation()
        config = getConfig()

        setContentView(R.layout.activity_open_video)

        openVideoFragment = OpenVideoFragment.newInstance(
            videoIdList = intent.getIntegerArrayListExtra("id")!!,
            title = "Лента",
            showMenuButtons = true,
            showAd = true
        )

        requestQueue = Volley.newRequestQueue(applicationContext)
        setupViewPager(tabViewPager)

        tabViewPager.addOnPageChangeListener(
            object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                }

                override fun onPageSelected(position: Int) {
                    if (position != 1) {
                        openVideoFragment.pauseVideo()
                    } else {
                        (viewPager2.adapter as ViewPagerAdapter).currentVideoView.requestFocus()
                    }
                }

                override fun onPageScrollStateChanged(state: Int) {}
            }
        )
    }

    private fun setupViewPager(viewPager: ViewPager) {
        val adapter = TabViewPagerAdapter(supportFragmentManager)

        adapter.addFragment(profileFragment, "Profile")
        adapter.addFragment(openVideoFragment, "Videos")
        adapter.addFragment(commentsFragment, "Comments")

        viewPager.adapter = adapter
        viewPager.currentItem = 1
    }

    @SuppressLint("MissingPermission")
    private fun updateLocation() {
        val locationManager =
            this.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val locationListener: LocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (savedLocation == null) {
                    savedLocation = location

                    val url = resources.getString(R.string.base_url) +
                            "/updateCoordinates?" +
                            "email=${userData.email}&" +
                            "phone=${userData.phone}&" +
                            "coordinates=${location.latitude}:${location.longitude}"

                    requestQueue.add(
                        StringRequest(Request.Method.GET, url, { run {} }, {
                            Log.e("Does user exist", "Error at sign in : " + it.message)
                        })
                    )

                    Log.d("LocationUpdated", url)
                }
            }

            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            0,
            0f,
            locationListener
        )
    }

    fun getConfig(): AppConfig {
        val configDataFile = applicationContext.getFileStreamPath("appConfig")

        if (configDataFile != null && configDataFile.exists()) {
            openFileInput("appConfig").use {
                return Json.decodeFromString(it.readBytes().toString(Charsets.UTF_8))
            }
        } else {
            return AppConfig()
        }
    }

    fun setConfig(config: AppConfig) {
        this.openFileOutput("appConfig", Context.MODE_PRIVATE)
            .write(Json.encodeToString(config).toByteArray())
    }

    fun openFragment(fragment: Fragment) {
        if (fragment is OpenVideoFragment) {
            isAnotherOpenVideoActivityOpened = true
        }

        supportFragmentManager.commit {
            setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out
            )
            replace(R.id.fragmentContainer, fragment)
            addToBackStack(null)
        }
    }

    override fun onBackPressed() =
        when {
            isAnotherOpenVideoActivityOpened -> {
                isAnotherOpenVideoActivityOpened = false
                super.onBackPressed()
            }
            tabViewPager.currentItem != 1 -> {
                tabViewPager.currentItem = 1
            }
            else -> {
                openVideoFragment.onBackPressed { super.onBackPressed() }
            }
        }

    override fun onStop() {
        super.onStop()
        openVideoFragment.pauseVideo()
        isActivityStopped = true
    }

    override fun onResume() {
        super.onResume()
        if (isActivityStopped) {
            openVideoFragment.pauseVideo()
        }
    }

    private fun readUserData(): User {
        openFileInput("userData").use {
            return User.fromJson(JSONObject(it.readBytes().toString(Charsets.UTF_8)))
        }
    }

    private fun writeUserData(data: User): Unit {
        openFileOutput("userData", Context.MODE_PRIVATE)
            .write(data.toJsonString().toByteArray())
    }
}