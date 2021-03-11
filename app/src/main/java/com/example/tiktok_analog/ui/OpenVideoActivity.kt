package com.example.tiktok_analog.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.tiktok_analog.R
import com.example.tiktok_analog.data.model.User
import com.example.tiktok_analog.ui.menu_screens.fragments.CommentsFragment
import com.example.tiktok_analog.ui.menu_screens.fragments.ProfileFragment
import com.example.tiktok_analog.util.TabViewPagerAdapter
import com.example.tiktok_analog.util.ViewPagerAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_open_video.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import org.json.JSONObject


class OpenVideoActivity : AppCompatActivity() {

    private lateinit var videoIdList: List<Int>
    private lateinit var userData: User

    private lateinit var requestQueue: RequestQueue

    private val profileFragment: ProfileFragment = ProfileFragment()

    // private val videoFragment = ...

    private val commentsFragment: CommentsFragment = CommentsFragment()

    public fun nextPage(pageId: Int) {
        viewPager2.doOnLayout {
            viewPager2.setCurrentItem(pageId + 1, true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_open_video)

        openFileInput("userData").use {
            userData = User.newUser(JSONObject(it.readBytes().toString(Charsets.UTF_8)))
        }

        requestQueue = Volley.newRequestQueue(applicationContext)

        videoIdList = intent.getIntegerArrayListExtra("id")!!.toList()

        viewPager2.adapter =
            ViewPagerAdapter(videoIdList, this, seekBar, progressBar, timeCode, pauseButton)

        viewPager2.offscreenPageLimit = 10

        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                (viewPager2.adapter as ViewPagerAdapter).setPage(position)
                Log.d("DEBUG", position.toString())
            }
        })

        val bottomSheetBehavior: BottomSheetBehavior<*> =
            BottomSheetBehavior.from<View>(bottom_sheet)

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        bottomSheetBehavior.isHideable = false

        val openVideoActivity = this

        bottomSheetBehavior.setBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                hideKeyboard(activity = openVideoActivity)
            }
        })

        openCommentsButton.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

            if ((viewPager2.adapter as ViewPagerAdapter).isVideoPlaying()) {
                (viewPager2.adapter as ViewPagerAdapter).pauseVideo()
            }

            updateComments()
        }

        backArrowButton.setOnClickListener {
            super.onBackPressed()
        }

        setupViewPager(tabViewPager)
        tabLayout.run {
            setupWithViewPager(tabViewPager)

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {}
                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })
        }
    }

    fun fillVideoData(videoId: Int, videoView: VideoView) {
        pauseButton.setOnClickListener {
            if (videoView.isPlaying) {
                pauseButton.setBackgroundResource(R.drawable.ic_baseline_play_arrow_24)
                videoView.pause()
            } else {
                pauseButton.setBackgroundResource(R.drawable.ic_baseline_pause_24)
                videoView.start()
            }
        }

        videoView.setOnClickListener {
            if (videoView.isPlaying) {
                pauseButton.setBackgroundResource(R.drawable.ic_baseline_play_arrow_24)
                videoView.pause()
            } else {
                pauseButton.setBackgroundResource(R.drawable.ic_baseline_pause_24)
                videoView.start()
            }
        }

        videoView.setOnCompletionListener {
            pauseButton.setBackgroundResource(R.drawable.ic_baseline_play_arrow_24)
            nextPage(pageId = (viewPager2.adapter as ViewPagerAdapter).currentPosition)
        }

        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {}

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    videoView.seekTo(progress)
                }
            }
        })

        sendButton.setOnClickListener {
            addComment(commentText.text.toString())
            commentText.setText("")
        }

        likeButton.setOnClickListener {
            val url =
                "https://kepler88d.pythonanywhere.com/likeVideo?videoId=" +
                        "$videoId&email=${userData.email}&phone=${userData.phone}"

            val videoLikeCountRequest = StringRequest(Request.Method.GET, url, { response ->
                run {
                    val result = JSONObject(response)
                    likeButton.setBackgroundResource(
                        if (result.getBoolean("isLiked"))
                            R.drawable.ic_like
                        else
                            R.drawable.ic_baseline_favorite_border_24
                    )
                    likeCount.text = result.getInt("likeCount").toString()
                }
            }, { Log.e("LikeVideo", "Error at sign in : " + it.message) })

            requestQueue.add(videoLikeCountRequest)
        }

        val url =
            "https://kepler88d.pythonanywhere.com/videoLikeCount?videoId=" +
                    "$videoId&email=${userData.email}&phone=${userData.phone}"

        val videoLikeCountRequest = StringRequest(Request.Method.GET, url, { response ->
            run {
                val result = JSONObject(response)
                likeButton.setBackgroundResource(
                    if (result.getBoolean("isLiked"))
                        R.drawable.ic_like
                    else
                        R.drawable.ic_baseline_favorite_border_24
                )
                likeCount.text =
                    result.getInt("likeCount").toString()
            }
        }, {
            Log.e("LikeVideo", "Error at sign in : " + it.message)
        })

        requestQueue.add(videoLikeCountRequest)

        val openVideoUrl = "https://kepler88d.pythonanywhere.com/openVideo?videoId=$videoId"

        val openVideoRequest = StringRequest(Request.Method.GET, openVideoUrl, { response ->
            run {
                val result = JSONObject(response)

                viewCount.text = result.getString("viewCount")
                commentCount.text = result.getString("commentCount")
            }
        }, {
            Log.e("OpenVideo", "Error at sign in : " + it.message)
        })

        requestQueue.add(openVideoRequest)
    }

    private fun updateComments() {
        commentsContainer.removeAllViews()

        val url =
            "https://kepler88d.pythonanywhere.com/getComments?videoId=" +
                    "${(viewPager2.adapter as ViewPagerAdapter).getCurrentVideoId()}"

        val commentRequest = StringRequest(Request.Method.GET, url, { response ->
            run {
                val result = JSONObject(response).getJSONArray("result")

                for (index in 0 until result.length()) {
                    addCommentView(result.getJSONObject(index))
                }
            }
        }, { Log.e("Comments", "Error at sign in : " + it.message) })

        requestQueue.add(commentRequest)

        fillVideoData(
            (viewPager2.adapter as ViewPagerAdapter).getCurrentVideoId(),
            (viewPager2.adapter as ViewPagerAdapter).currentVideoView
        )
    }

    private fun addCommentView(jsonObject: JSONObject) {
        val newView =
            LayoutInflater.from(applicationContext)
                .inflate(R.layout.comment_item, null, false)
        newView.findViewWithTag<TextView>("sender").text =
            jsonObject.getString("authorUsername")
        newView.findViewWithTag<TextView>("commentText").text =
            jsonObject.getString("text")
        commentsContainer.addView(newView)

        newView.findViewWithTag<ImageView>("likeIcon").setOnClickListener {
            it.setBackgroundResource(R.drawable.ic_like)
        }

        commentsScrollView.post {
            commentsScrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private fun addComment(commentText: String) {
        val url = "https://kepler88d.pythonanywhere.com/addComment?videoId=" +
                "${(viewPager2.adapter as ViewPagerAdapter).getCurrentVideoId()}" +
                "&commentText=${commentText.trim()}&email=${userData.email}&phone=${userData.phone}"

        val addCommentRequest = StringRequest(Request.Method.GET, url, {
            run {
                updateComments()
            }
        }, {
            Log.e("Add comment", "Error at sign in : " + it.message)
        })

        requestQueue.add(addCommentRequest)
    }

    private fun hideKeyboard(activity: Activity) {
        (activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(
                (if (activity.currentFocus == null) View(activity)
                else activity.currentFocus)!!.windowToken, 0
            )
    }

    private fun setupViewPager(viewPager: ViewPager) {
        val adapter = TabViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(profileFragment, "Profile")
        // adapter.addFragment(dashboardFragment, "Videos")
        adapter.addFragment(commentsFragment, "Comments")
//        tabViewPager.adapter = adapter
//        tabViewPager.currentItem = 1
    }
}