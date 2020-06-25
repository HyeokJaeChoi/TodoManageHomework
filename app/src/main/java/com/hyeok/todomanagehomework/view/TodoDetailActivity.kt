package com.hyeok.todomanagehomework.view

import android.Manifest
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
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
import com.hyeok.todomanagehomework.util.sqlite.DbHelper
import kotlinx.android.synthetic.main.activity_todo_detail.*
import org.threeten.bp.LocalDateTime
import splitties.toast.toast
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
}