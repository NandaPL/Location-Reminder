package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import com.google.android.gms.location.Priority
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.Constants
import com.udacity.project4.utils.Constants.ACTION_GEOFENCE_EVENT
import com.udacity.project4.utils.isDeviceLocationEnabled
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class SaveReminderFragment : BaseFragment() {
    private val TAG = SaveReminderFragment::class.java.simpleName

    // Get the view model this time as a single to be shared with the another fragment
    override val mViewModel: SaveReminderViewModel by activityViewModel()
    private lateinit var dataBinding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity().applicationContext, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE        )
    }
    private lateinit var mContext: Context
    private lateinit var reminderDataItem: ReminderDataItem

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
        reminderDataItem =
            ReminderDataItem(mViewModel.reminderTitle.value,
                mViewModel.reminderDescription.value, mViewModel.reminderSelectedLocationStr.value,
                mViewModel.latitude.value, mViewModel.longitude.value)

        dataBinding.btnSaveReminder.setOnClickListener {
            if (mViewModel.validateEnteredData(reminderDataItem)){
                checkPermissionsAndStartGeofencing()
            } else {
                Snackbar.make(
                    dataBinding.root,
                    getString(R.string.save_reminder_error_explanation),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    checkPermissionsAndStartGeofencing()
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    checkPermissionsAndStartGeofencing()
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_BACKGROUND_LOCATION, false) -> {
                    checkPermissionsAndStartGeofencing()
                }
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
            if (isDeviceLocationEnabled())
                addGeofence(reminderDataItem.id)
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
    ) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = this.activity?.let {
            LocationServices.getSettingsClient(it)
        }
        val locationSettingsResponseTask = settingsClient?.checkLocationSettings(builder.build())
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
        locationSettingsResponseTask?.addOnSuccessListener {
            addGeofence(reminderDataItem.id)
        }
    }

    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (mContext.let {
                    ActivityCompat.checkSelfPermission(it,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)

        val backgroundPermissionApproved = if (runningQOrLater()) {
                PackageManager.PERMISSION_GRANTED ==
                        mContext.let {
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
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setLoiteringDelay(1000)
                    .build()
            }
        }

        val geofenceRequest = geofence?.let {
            GeofencingRequest.Builder()
                .addGeofence(it)
                .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }

        geofencingClient.addGeofences(geofenceRequest!!, geofencePendingIntent).run {
            addOnSuccessListener {
                mViewModel.saveReminder(reminderDataItem)
                Toast.makeText(
                    mContext,
                    requireContext().getString(R.string.geofence_add),
                    Toast.LENGTH_LONG
                ).show()
            }
            addOnFailureListener {
                Log.e("TAG", "startGeofence:${it} ")
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
}