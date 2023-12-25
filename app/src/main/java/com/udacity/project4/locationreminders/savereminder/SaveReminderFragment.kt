package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.Constants
import com.udacity.project4.utils.Constants.ACTION_GEOFENCE_EVENT
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    private val TAG = SaveReminderFragment::class.java.simpleName

    // Get the view model this time as a single to be shared with the another fragment
    override val mViewModel: SaveReminderViewModel by inject()
    private lateinit var dataBinding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent =
            Intent(requireActivity().applicationContext, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(
            requireActivity().applicationContext,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE
        )
    }
    private lateinit var mContext: Context

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_save_reminder
        dataBinding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        setDisplayHomeAsUpEnabled(true)
        dataBinding.viewModel = mViewModel

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        return dataBinding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataBinding.lifecycleOwner = this
        dataBinding.tvSelectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            val directions = SaveReminderFragmentDirections
                .actionSaveReminderFragmentToSelectLocationFragment()
            mViewModel.navigationCommand.value = NavigationCommand.To(directions)
        }

        dataBinding.btnSaveReminder.setOnClickListener {
            if (mViewModel.reminderTitle.value == null || mViewModel.reminderDescription.value == null
                || mViewModel.latitude.value == null || mViewModel.longitude.value == null) {
                Snackbar.make(
                    dataBinding.root,
                    getString(R.string.save_reminder_error_explanation),
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                checkPermissionsAndStartGeofencing()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    checkPermissionsAndStartGeofencing()                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    checkPermissionsAndStartGeofencing()                }
                permissions.getOrDefault(Manifest.permission.ACCESS_BACKGROUND_LOCATION, false) -> {
                    checkPermissionsAndStartGeofencing()                }
                else -> {
                    Log.i("Permission: ", getString(R.string.denied))
                    Toast.makeText(
                        mContext,
                        getString(R.string.location_permission_was_not_granted),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun checkDeviceLocationSettings(
        resolve: Boolean
    ): Task<LocationSettingsResponse>? {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = this.activity?.let { LocationServices.getSettingsClient(it) }
        val locationSettingsResponseTask =
            settingsClient?.checkLocationSettings(builder.build())
        locationSettingsResponseTask?.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        Constants.REQUEST_TURN_DEVICE_LOCATION_ON,
                        null, 0, 0, 0, null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, getString(R.string.error_getting_location_settings) + sendEx.message)
                }
            } else {
                Snackbar.make(
                    dataBinding.root,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings(true)
                }.show()
            }
        }
        return locationSettingsResponseTask
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationSettingsResponseTask =
            checkDeviceLocationSettings(resolve)
        locationSettingsResponseTask?.addOnCompleteListener {
            if (it.isSuccessful) {
                val reminderDataItem =
                    ReminderDataItem(mViewModel.reminderTitle.value,
                        mViewModel.reminderDescription.value, mViewModel.reminderSelectedLocationStr.value,
                        mViewModel.latitude.value, mViewModel.longitude.value)
                mViewModel.saveReminder(reminderDataItem)
                addGeofence(reminderDataItem.id)
            }
        }
    }

    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        mContext?.let {
                            ActivityCompat.checkSelfPermission(
                                it,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        })
        val backgroundPermissionApproved =
            if (runningQOrLater()) {
                PackageManager.PERMISSION_GRANTED ==
                        mContext?.let {
                            ActivityCompat.checkSelfPermission(
                                it, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            )
                        }
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    private fun runningQOrLater(): Boolean {
        return Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(id: String) {
        val geofence = mViewModel.latitude.value?.let {
            mViewModel.longitude.value?.let { it1 ->
                Geofence.Builder()
                    .setRequestId(id)
                    .setCircularRegion(it, it1, Constants.GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setLoiteringDelay(1000)
                    .build()
            }
        }

        val geofenceRequest = geofence?.let {
            GeofencingRequest.Builder()
                .addGeofence(it)
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .build()
        }

        geofencingClient.addGeofences(geofenceRequest!!, geofencePendingIntent).run {
            addOnSuccessListener {
                Toast.makeText(
                    mContext,
                    requireContext().getString(R.string.geofence_add),
                    Toast.LENGTH_LONG
                ).show()
            }
            addOnFailureListener {
                Toast.makeText(
                    mContext,
                    getString(R.string.exception_occurred, it.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to clear the view model after destroy, as it's a single view model.
        mViewModel.onClear()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d("TAG", "onRequestPermissionResult")

        if (
            grantResults.isEmpty() ||
            grantResults[Constants.LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == Constants.REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[Constants.BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED))
        {
            Snackbar.make(
                dataBinding.root,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", "com.example.android.treasureHunt", null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }
}