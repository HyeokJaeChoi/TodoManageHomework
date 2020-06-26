package com.hyeok.todomanagehomework.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.hyeok.todomanagehomework.R
import com.hyeok.todomanagehomework.util.file.DocumentUriConverter
import com.hyeok.todomanagehomework.util.sqlite.DbHelper
import com.hyeok.todomanagehomework.util.validator.UriValidator
import kotlinx.android.synthetic.main.activity_todo_detail.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDateTime
import splitties.lifecycle.coroutines.MainAndroid
import splitties.toast.toast
import java.io.FileInputStream
import java.util.*

class TodoDetailActivity : AppCompatActivity(), View.OnClickListener, OnMapReadyCallback {

    private val imm by lazy { getSystemService<InputMethodManager>() }
    private val dbHelper by lazy { DbHelper(this) }
    private val fusedLocationProvider by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val geocoder by lazy { Geocoder(this, Locale.getDefault()) }
    private lateinit var googleMap: GoogleMap
    private lateinit var userLocation: LatLng
    private lateinit var mapMarker: Marker
    private val loadingDialog by lazy { LoadingDialog(this) }
    private var isMultimediaDataSelected = false
        set(value) {
            field = value
            if(value && todo_detail_multimedia_memo_add_btn.text.toString() == getString(R.string.todo_detail_multimedia_memo_add)) {
                todo_detail_multimedia_memo_add_btn.text = getString(R.string.todo_detail_multimedia_memo_replace)
            }
        }
    private var lastSelectedMultimediaType = MULTIMEDIA_TYPE_NONE
    private val mediaPlayer by lazy { MediaPlayer() }
    private var currentMediaPlayerPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo_detail)

        (todo_detail_place_map as SupportMapFragment).getMapAsync(this)

        todo_detail_place_search_btn.setOnClickListener(this)
        todo_detail_multimedia_memo_add_btn.setOnClickListener(this)
        todo_detail_place_input_field.setOnEditorActionListener { v, actionId, event ->
            when(actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    todo_detail_place_search_btn.performClick()
                }
            }
            true
        }
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.todo_detail_place_search_btn -> {
                loadingDialog.setOnShowListener {
                    val searchPlace = todo_detail_place_input_field.text.toString()
                    if(searchPlace.isNotEmpty()) {
                        imm?.hideSoftInputFromWindow(todo_detail_place_input_field.windowToken, 0)
                        val address = geocoder.getFromLocationName(searchPlace, 3)[0]

                        if(this::googleMap.isInitialized && this::mapMarker.isInitialized) {
                            userLocation = LatLng(address.latitude, address.longitude)

                            mapMarker.run {
                                position = userLocation
                                title = address.featureName
                            }
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                        }
                    }
                    else {
                        toast("검색할 장소를 입력해주세요.")
                    }

                    it.dismiss()
                }

                loadingDialog.show()
            }
            R.id.todo_detail_multimedia_memo_add_btn -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }

                startActivityForResult(intent, REQUEST_SYSTEM_PICKER)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == REQUEST_SYSTEM_PICKER && resultCode == RESULT_OK && data != null) {
            data.data?.let {
                when {
                    UriValidator.isImageUri(it) -> {
                        changeMultimediaViewVisibility(MULTIMEDIA_TYPE_IMAGE)

                        GlobalScope.launch {
                            withContext(Dispatchers.IO) {
                                DocumentUriConverter.getBitmapFromContentUri(this@TodoDetailActivity, it)?.let {
                                    Log.d(javaClass.simpleName, "${it.width} ${it.height}")
                                    withContext(Dispatchers.Main) {
                                        Glide.with(this@TodoDetailActivity)
                                            .load(it)
                                            .into(todo_detail_multimedia_data_img)

                                        isMultimediaDataSelected = true
                                    }
                                }
                            }
                        }
                    }
                    UriValidator.isAudioUri(it) -> {
                        changeMultimediaViewVisibility(MULTIMEDIA_TYPE_AUDIO)

                        contentResolver.openFileDescriptor(it, "r")?.fileDescriptor?.let {
                            mediaPlayer.run {
                                setDataSource(it)
                                prepare()
                            }
                            todo_detail_multimedia_data_audio_video_play_btn.setOnClickListener {
                                if(currentMediaPlayerPosition > 0) {
                                    mediaPlayer.seekTo(currentMediaPlayerPosition)
                                }
                                mediaPlayer.start()
                            }
                            todo_detail_multimedia_data_audio_video_pause_btn.setOnClickListener {
                                mediaPlayer.run {
                                    pause()
                                    currentMediaPlayerPosition = currentPosition
                                }
                            }
                            todo_detail_multimedia_data_audio_video_stop_btn.setOnClickListener {
                                mediaPlayer.run {
                                    pause()
                                    currentMediaPlayerPosition = 0
                                }
                            }
                        }
                    }
                    UriValidator.isVideoUri(it) -> {

                    }
                    else -> {
                        toast("이미지 / 오디오 / 비디오 타입의 파일을 선택해주세요.")
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.let {
            menuInflater.inflate(R.menu.todo_detail_action, it)

            return true
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.todo_detail_save_btn -> {

            }
            R.id.todo_detail_remove_btn -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onMapReady(p0: GoogleMap?) {
        p0?.let {
            googleMap = it
            googleMap.uiSettings.setAllGesturesEnabled(false)
            checkLocationPermission()
        }
    }

    private fun checkLocationPermission() {
        TedPermission.with(this)
            .setPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            .setPermissionListener(object : PermissionListener {
                override fun onPermissionGranted() {
                    getLastKnownLocation()
                }

                override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                    setLayoutNewTodoAdd()
                }
            })
            .check()
    }

    private fun getLastKnownLocation() {
        try {
            fusedLocationProvider.lastLocation
                .addOnSuccessListener {
                    userLocation = LatLng(it.latitude, it.longitude)
                    setLayoutNewTodoAdd()
                }
                .addOnFailureListener {
                    setLayoutNewTodoAdd()
                }
        } catch(e: SecurityException) {
            setLayoutNewTodoAdd()
        }
    }

    private fun setLayoutNewTodoAdd() {
        val currentDateTime = LocalDateTime.now()

        todo_detail_date_picker.updateDate(currentDateTime.year, currentDateTime.monthValue.minus(1), currentDateTime.dayOfMonth)
        todo_detail_start_time_picker.run {
            setIs24HourView(true)
            hour = currentDateTime.hour
            minute = currentDateTime.minute
        }
        todo_detail_end_time_picker.run {
            setIs24HourView(true)
            hour = currentDateTime.hour
            minute = currentDateTime.minute
        }

        if(this::googleMap.isInitialized) {
            if(!this::userLocation.isInitialized) {
                //서울시청을 기본 위치로 설정
                userLocation = LatLng(37.566257, 126.978020)
            }

            googleMap.run {
                mapMarker = addMarker(MarkerOptions().position(userLocation))
                moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15F))
            }
        }

        todo_detail_multimedia_memo_add_btn.text = getString(R.string.todo_detail_multimedia_memo_add)
    }

    private fun changeMultimediaViewVisibility(multimediaType: Int) {
        if(lastSelectedMultimediaType != MULTIMEDIA_TYPE_NONE) {
            when(lastSelectedMultimediaType) {
                MULTIMEDIA_TYPE_IMAGE -> {
                    todo_detail_multimedia_data_img.run {
                        setImageResource(0)
                        visibility = View.GONE
                    }
                }
                MULTIMEDIA_TYPE_AUDIO -> {
                    todo_detail_multimedia_data_audio_video_play_btn.visibility = View.GONE
                    todo_detail_multimedia_data_audio_video_pause_btn.visibility = View.GONE
                    todo_detail_multimedia_data_audio_video_stop_btn.visibility = View.GONE
                }
                MULTIMEDIA_TYPE_VIDEO -> {

                }
            }
        }

        when(multimediaType) {
            MULTIMEDIA_TYPE_IMAGE -> {
                todo_detail_multimedia_data_img.visibility = View.VISIBLE
            }
            MULTIMEDIA_TYPE_AUDIO -> {
                todo_detail_multimedia_data_audio_video_play_btn.visibility = View.VISIBLE
                todo_detail_multimedia_data_audio_video_pause_btn.visibility = View.VISIBLE
                todo_detail_multimedia_data_audio_video_stop_btn.visibility = View.VISIBLE
            }
            MULTIMEDIA_TYPE_VIDEO -> {

            }
        }

        lastSelectedMultimediaType = multimediaType
    }

    companion object {
        const val REQUEST_SYSTEM_PICKER = 1
        const val MULTIMEDIA_TYPE_NONE = 100
        const val MULTIMEDIA_TYPE_IMAGE = 101
        const val MULTIMEDIA_TYPE_AUDIO = 102
        const val MULTIMEDIA_TYPE_VIDEO = 103
    }
}