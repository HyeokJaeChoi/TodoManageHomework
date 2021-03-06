package com.hyeok.todomanagehomework.view

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.location.Geocoder
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.SurfaceHolder
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import com.bumptech.glide.Glide
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
import com.hyeok.todomanagehomework.model.Todo
import com.hyeok.todomanagehomework.util.file.DocumentUriConverter
import com.hyeok.todomanagehomework.util.sqlite.DbHelper
import com.hyeok.todomanagehomework.util.sqlite.TodoContract
import com.hyeok.todomanagehomework.util.validator.UriValidator
import kotlinx.android.synthetic.main.activity_todo_detail.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDateTime
import splitties.alertdialog.appcompat.message
import splitties.toast.toast
import java.util.*

class TodoDetailActivity : AppCompatActivity(), View.OnClickListener, OnMapReadyCallback {

    private val imm by lazy { getSystemService<InputMethodManager>() }
    private val dbHelper by lazy { DbHelper(this) }
    private val currentDateTime by lazy { LocalDateTime.now() }
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
    private lateinit var lastSelectedMultimediaUri: Uri
    private val mediaPlayer by lazy { MediaPlayer() }
    private var currentMediaPlayerPosition = 0
    private lateinit var surfaceHolder: SurfaceHolder
    private val existTodo by lazy { intent.extras?.getParcelable<Todo>(MainActivity.EXIST_TODO) }

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
        supportActionBar?.title = "일정 상세"
    }

    override fun onDestroy() {
        mediaPlayer.release()
        super.onDestroy()
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
                handleMultimediaUri(it)
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
                val saveFailMessage = getErrorMessageForSaveMemo()

                if(saveFailMessage.isNotEmpty()) {
                    toast(saveFailMessage)
                }
                else {
                    val selectedDate = todo_detail_date_picker.year.zeroFormat() + "-" + todo_detail_date_picker.month.plus(1).zeroFormat() + "-" + todo_detail_date_picker.dayOfMonth.zeroFormat()
                    val startTime = todo_detail_start_time_picker.hour.zeroFormat() + ":" + todo_detail_start_time_picker.minute.zeroFormat()
                    val endTime = todo_detail_end_time_picker.hour.zeroFormat() + ":" + todo_detail_end_time_picker.minute.zeroFormat()
                    val multimediaUri = if(this@TodoDetailActivity::lastSelectedMultimediaUri.isInitialized) {
                        lastSelectedMultimediaUri.toString()
                    }
                    else {
                        null
                    }

                    val contentValues = ContentValues().apply {
                        put(TodoContract.TodoEntry.TITLE, todo_detail_title_input_filed.text.toString())
                        put(TodoContract.TodoEntry.DATE, selectedDate)
                        put(TodoContract.TodoEntry.START_TIME, startTime)
                        put(TodoContract.TodoEntry.END_TIME, endTime)
                        put(TodoContract.TodoEntry.LATITUDE, userLocation.latitude)
                        put(TodoContract.TodoEntry.LONGITUDE, userLocation.longitude)
                        put(TodoContract.TodoEntry.CONTENT, todo_detail_memo_input_field.text.toString())
                        put(TodoContract.TodoEntry.MULTIMEDIA_CONTENT_URI, multimediaUri)
                    }
                    dbHelper.insert(TodoContract.TodoEntry.TABLE_NAME, contentValues)

                    toast("일정을 저장하였습니다.")
                    setResult(RESULT_OK)
                    finish()
                }
            }
            R.id.todo_detail_remove_btn -> {
                existTodo?.let {
                    val alertDialogBuilder = AlertDialog.Builder(this).apply {
                        title = "일정 삭제"
                        message = "일정을 삭제하시겠습니까?"
                        setPositiveButton("예") { dialog, which ->
                            dbHelper.delete(TodoContract.TodoEntry.TABLE_NAME, BaseColumns._ID + "=?", arrayOf(it.id.toString()))

                            toast("일정을 삭제하였습니다.")
                            setResult(RESULT_OK)
                            finish()
                        }
                        setNegativeButton("아니오") { dialog, which ->
                            dialog.dismiss()
                        }

                    }

                    alertDialogBuilder.create().show()
                } ?: run {
                    toast("추가되지 않은 할일은 삭제할 수 없습니다.")
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onMapReady(p0: GoogleMap?) {
        p0?.let {
            googleMap = it
            googleMap.uiSettings.setAllGesturesEnabled(false)
        }

        existTodo?.let {
            setLayoutExistTodo(it)
        } ?: run {
            checkLocationPermission()
        }
    }

    private fun handleMultimediaUri(uri: Uri) {
        when {
            UriValidator.isImageUri(uri) -> {
                changeMultimediaViewVisibility(MULTIMEDIA_TYPE_IMAGE)

                GlobalScope.launch {
                    withContext(Dispatchers.IO) {
                        DocumentUriConverter.getBitmapFromContentUri(this@TodoDetailActivity, uri)?.let { bitmap ->
                            withContext(Dispatchers.Main) {
                                Glide.with(this@TodoDetailActivity)
                                    .load(bitmap)
                                    .into(todo_detail_multimedia_data_img)

                                isMultimediaDataSelected = true
                                lastSelectedMultimediaUri = uri
                            }
                        }
                    }
                }
            }
            UriValidator.isAudioUri(uri) -> {
                mediaPlayer.reset()
                changeMultimediaViewVisibility(MULTIMEDIA_TYPE_AUDIO)

                contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor?.let { fileDescriptor ->
                    mediaPlayer.run {
                        setDataSource(fileDescriptor)
                        prepare()
                    }
                    setPlayPauseStopBtnOnClicked()

                    isMultimediaDataSelected = true
                    lastSelectedMultimediaUri = uri
                }
            }
            UriValidator.isVideoUri(uri) -> {
                mediaPlayer.reset()
                changeMultimediaViewVisibility(MULTIMEDIA_TYPE_VIDEO)

                surfaceHolder = todo_detail_multimedia_data_video.holder.apply {
                    addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder?) {
                            Log.d(this@TodoDetailActivity.javaClass.simpleName, "surfaceCreated")
                            onVideoSelected(uri)
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder?,
                            format: Int,
                            width: Int,
                            height: Int
                        ) {

                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder?) {

                        }
                    })
                }
            }
            else -> {
                toast("이미지 / 오디오 / 비디오 타입의 파일을 선택해주세요.")
            }
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

    private fun setLayoutExistTodo(todo: Todo) {
        todo.run {
            val dateSplit = date.split("-")
            val startTimeSplit = startTime.split(":")
            val endTimeSplit = endTime.split(":")

            todo_detail_title_input_filed.setText(title)
            todo_detail_date_picker.updateDate(dateSplit[0].toInt(), dateSplit[1].toInt().minus(1), dateSplit[2].toInt())
            todo_detail_start_time_picker.run {
                setIs24HourView(true)
                hour = startTimeSplit[0].toInt()
                minute = startTimeSplit[1].toInt()
            }
            todo_detail_end_time_picker.run {
                setIs24HourView(true)
                hour = endTimeSplit[0].toInt()
                minute = endTimeSplit[1].toInt()
            }
            todo_detail_memo_input_field.setText(content)

            if(this@TodoDetailActivity::googleMap.isInitialized) {
                val position = LatLng(latitude, longitude)

                googleMap.run {
                    addMarker(MarkerOptions().position(position))
                    moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15F))
                }
            }

            if(multimediaContentUri != null) {
                handleMultimediaUri(Uri.parse(multimediaContentUri))
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
                    todo_detail_multimedia_data_audio_video_play_btn.visibility = View.GONE
                    todo_detail_multimedia_data_audio_video_pause_btn.visibility = View.GONE
                    todo_detail_multimedia_data_audio_video_stop_btn.visibility = View.GONE
                    todo_detail_multimedia_data_video.visibility = View.GONE
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
                todo_detail_multimedia_data_audio_video_play_btn.visibility = View.VISIBLE
                todo_detail_multimedia_data_audio_video_pause_btn.visibility = View.VISIBLE
                todo_detail_multimedia_data_audio_video_stop_btn.visibility = View.VISIBLE
                todo_detail_multimedia_data_video.visibility = View.VISIBLE
            }
        }

        lastSelectedMultimediaType = multimediaType
    }

    private fun setPlayPauseStopBtnOnClicked() {
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
                seekTo(currentMediaPlayerPosition)
            }
        }
    }

    private fun onVideoSelected(uri: Uri) {
        contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor?.let {
            mediaPlayer.run {
                setDataSource(it)
                setDisplay(surfaceHolder)
                prepare()
                setOnCompletionListener {
                    it.seekTo(0)
                }
            }
            setPlayPauseStopBtnOnClicked()

            isMultimediaDataSelected = true
            lastSelectedMultimediaUri = uri
        }
    }

    private fun getErrorMessageForSaveMemo(): String {
        if(todo_detail_title_input_filed.text.isEmpty()) {
            return getString(R.string.todo_detail_save_fail_title_empty)
        }
        else if(todo_detail_date_picker.year < currentDateTime.year
            && todo_detail_date_picker.month < currentDateTime.monthValue.minus(1)
            && todo_detail_date_picker.dayOfMonth < currentDateTime.dayOfMonth) {
            return getString(R.string.todo_detail_save_fail_date_is_before_than_current)
        }
        else if(todo_detail_start_time_picker.hour > todo_detail_end_time_picker.hour
            || (todo_detail_start_time_picker.hour == todo_detail_end_time_picker.hour && todo_detail_start_time_picker.minute > todo_detail_end_time_picker.minute)) {
            return getString(R.string.todo_detail_save_fail_start_time_is_after_than_end_time)
        }
        else if(!this::userLocation.isInitialized) {
            return getString(R.string.todo_detail_save_fail_not_selected_place)
        }
        else if(todo_detail_memo_input_field.text.isEmpty()) {
            return getString(R.string.todo_detail_save_fail_memo_content_empty)
        }
        else {
            return ""
        }
    }

    private fun Int.zeroFormat(): String {
        return String.format("%02d", this)
    }

    companion object {
        const val REQUEST_SYSTEM_PICKER = 1
        const val MULTIMEDIA_TYPE_NONE = 100
        const val MULTIMEDIA_TYPE_IMAGE = 101
        const val MULTIMEDIA_TYPE_AUDIO = 102
        const val MULTIMEDIA_TYPE_VIDEO = 103
    }
}