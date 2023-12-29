package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.locationreminders.savereminder.SelectedLocation
import com.udacity.project4.utils.Constants
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.wrapEspressoIdlingResource
import org.koin.android.ext.android.inject
import java.util.Locale

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {
    private val TAG = SelectLocationFragment::class.java.simpleName

    // Use Koin to get the view model of the SaveReminder
    override val mViewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap
    private val zoomLevel = 15f
    private lateinit var myLocation: FusedLocationProviderClient

    private var selectedLocation: SelectedLocation? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_select_location
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        binding.viewModel = mViewModel
        binding.lifecycleOwner = this

        myLocation = LocationServices.getFusedLocationProviderClient(requireContext())

        binding.btnSaveReminderLocation.setOnClickListener {
            onLocationSelected()
        }

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root
    }

    @SuppressLint("NewApi")
    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        )
        { isGranted ->
            if (isGranted) {
                enableLocation()
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle(getString(R.string.permission_required))
                        .setMessage(R.string.permission_denied_explanation)
                        .setPositiveButton(
                            getString(R.string.message_permission_granted)
                        ) { dialog, _ ->
                            enableLocation()
                            dialog.dismiss()
                        }
                        .setNegativeButton(
                            R.string.denied
                        ) { dialog, _ ->
                            raisePermissionDeniedSnackBar(getString(R.string.permission_denied_explanation))
                            dialog.dismiss()
                        }
                        .show()

                } else {
                    raisePermissionDeniedSnackBar(getString(R.string.permission_denied_explanation))
                }
            }

        }

    @SuppressLint("MissingPermission")
    private fun enableLocation() {
        if (permissionCheck(Manifest.permission.ACCESS_FINE_LOCATION)) {
            wrapEspressoIdlingResource {
                map.isMyLocationEnabled = true
                var currentLocation: LatLng?
                myLocation.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        currentLocation = LatLng(location.latitude, location.longitude)
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                currentLocation!!,
                                zoomLevel
                            )
                        )
                        selectedLocation = SelectedLocation(
                            getString(R.string.dropped_pin),
                            currentLocation!!
                        )
                    }
                }
            }
            Toast.makeText(requireActivity(), R.string.select_poi, Toast.LENGTH_LONG).show()
        } else {
            requestPermissionLauncher.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    private fun onLocationSelected() {
        findNavController().navigate(SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment())
    }

    private fun setLongClickMap(map: GoogleMap){
        map.setOnMapClickListener {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 11f))

            map.addMarker(
                MarkerOptions().apply {
                    position(it)
                    title(getString(R.string.add_pin))
                    snippet(getLocationSnippet(it))
                    icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                }
            )

            mViewModel.setLocationSelectedInfo(it, getString(R.string.personalized_localization))
        }
    }

    private fun setPoiClickSelected(googleMap: GoogleMap){
        googleMap.setOnPoiClickListener {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it.latLng, 11f))
            val poiMarker = googleMap.addMarker(
                MarkerOptions().apply {
                    position(it.latLng)
                    title(it.name)
                }
            )
            poiMarker?.showInfoWindow()
            mViewModel.setLocationSelectedInfo(it.latLng, it.name)
        }
    }

    private fun getLocationSnippet(latLng: LatLng): String {
        return String.format(
            Locale.getDefault(),
            getString(R.string.lat_long_snippet),
            latLng.latitude,
            latLng.longitude
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            map.isMyLocationEnabled = true
            Toast.makeText(context, "Location permission is granted.", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                Constants.REQUEST_LOCATION_PERMISSION
            )

        }
    }

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION) === PackageManager.PERMISSION_GRANTED
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireActivity(),
                    R.raw.style_map
                )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        setMapStyle(map)
        setLongClickMap(map)
        setPoiClickSelected(map)
        enableLocation()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        if (requestCode == Constants.REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    enableMyLocation()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        snackBarWasTapped?.let {
            if (permissionCheck(Manifest.permission.ACCESS_FINE_LOCATION)) {
                enableLocation()
            } else {
                raisePermissionDeniedSnackBar(getString(R.string.permission_denied_explanation))
            }
        }
    }
}