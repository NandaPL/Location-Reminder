package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
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
import com.udacity.project4.utils.wrapEspressoIdlingResource
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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
    private lateinit var mContext: Context
    private lateinit var reminderDataItem: ReminderDataItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dataBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        geofencingClient = LocationServices.getGeofencingClient(requireContext())

        setDisplayHomeAsUpEnabled(true)

        dataBinding.viewModel = mViewModel

        return dataBinding.root
    }

    @SuppressLint("NewApi")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutId = R.layout.fragment_save_reminder

        setDisplayHomeAsUpEnabled(true)
        dataBinding.viewModel = mViewModel

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        dataBinding.tvSelectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            mViewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        dataBinding.btnSaveReminder.setOnClickListener {

            reminderDataItem = ReminderDataItem(mViewModel.reminderTitle.value,
                mViewModel.reminderDescription.value, mViewModel.reminderSelectedLocationStr.value,
                mViewModel.latitude.value, mViewModel.longitude.value)

            if (mViewModel.validateEnteredData(reminderDataItem)) {
                if (permissionCheck(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (permissionCheck(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        if (permissionCheck(Manifest.permission.POST_NOTIFICATIONS)) {
                            addGeofence()
                        } else {
                            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        raiseExplanationDialogue()
                    }
                } else {
                    requestFineLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }
    }



    @SuppressLint("NewApi")
    @RequiresApi(Build.VERSION_CODES.Q)
    val requestFineLocationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        )
        { isGranted ->
            if (isGranted) {
                if (!permissionCheck(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    raiseExplanationDialogue()
                } else {
                    if(!permissionCheck(Manifest.permission.POST_NOTIFICATIONS)){
                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            } else {
                raisePermissionDeniedSnackBar(getString(R.string.location_required_error))
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun raiseExplanationDialogue() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.permission_required))
            .setMessage(R.string.background_permission_rationale)
            .setPositiveButton(
                getString(R.string.message_permission_granted)
            ) { dialog, _ ->
                requestBackgroundLocationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
                dialog.dismiss()
            }
            .setNegativeButton(
                R.string.denied
            ) { dialog, _ ->
//                enableSaveButton(false)
                raisePermissionDeniedSnackBar(getString(R.string.location_required_error))
                dialog.dismiss()
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun raiseNotificationExplanationDialogue() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.permission_required))
            .setMessage(R.string.background_permission_rationale)
            .setPositiveButton(
                getString(R.string.denied)
            ) { dialog, _ ->
                requestNotificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
                dialog.dismiss()
            }
            .setNegativeButton(
                R.string.location_permission_was_not_granted
            ) { dialog, _ ->
                raisePermissionDeniedSnackBar(getString(R.string.denied))
                dialog.dismiss()
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    val requestNotificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        )
        { isEnabled ->
            if (!isEnabled) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    raiseNotificationExplanationDialogue()
                } else {
                    raisePermissionDeniedSnackBar(getString(R.string.denied))
                }
            }
        }

    @SuppressLint("NewApi")
    @RequiresApi(Build.VERSION_CODES.Q)
    val requestBackgroundLocationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        )
        { isGranted ->
            if (!isGranted) {
                raisePermissionDeniedSnackBar(getString(R.string.location_required_error))
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    @SuppressLint("VisibleForTests", "MissingPermission")
    private fun createGeofence() {
        wrapEspressoIdlingResource {
            val geofence = Geofence.Builder()
                .setRequestId(reminderDataItem.id)
                .setCircularRegion(
                    reminderDataItem.latitude!!,
                    reminderDataItem.longitude!!,
                    Constants.GEOFENCE_RADIUS
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            val geofenceRequest = GeofencingRequest.Builder()
                .addGeofence(geofence)
                .build()

            geofencingClient.addGeofences(geofenceRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    mViewModel.validateEnteredData(reminderDataItem)
                }
                addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.geofences_not_added),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
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
    private fun addGeofence() {
        val geofence = mViewModel.latitude.value?.let {
            mViewModel.longitude.value?.let { it1 ->
                Geofence.Builder()
                    .setRequestId(reminderDataItem.id)
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