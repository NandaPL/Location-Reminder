package com.udacity.project4.base

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R

/**
 * Base Fragment to observe on the common LiveData objects
 */
abstract class BaseFragment : Fragment() {
    /**
     * Every fragment has to have an instance of a view model that extends from the BaseViewModel
     */
    abstract val mViewModel: BaseViewModel
    var snackBarWasTapped: Boolean? = null
    private val runningQOrLater =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    private val runningTiramisuOrLater =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    override fun onStart() {
        super.onStart()
        mViewModel.showErrorMessage.observe(this) {
            Toast.makeText(activity, it, Toast.LENGTH_LONG).show()
        }
        mViewModel.showToast.observe(this) {
            Toast.makeText(activity, it, Toast.LENGTH_LONG).show()
        }
        mViewModel.showSnackBar.observe(this) {
            Snackbar.make(this.requireView(), it, Snackbar.LENGTH_LONG).show()
        }
        mViewModel.showSnackBarInt.observe(this) {
            Snackbar.make(this.requireView(), getString(it), Snackbar.LENGTH_LONG).show()
        }

        mViewModel.navigationCommand.observe(this) { command ->
            when (command) {
                is NavigationCommand.To -> findNavController().navigate(command.directions)
                is NavigationCommand.Back -> findNavController().popBackStack()
                is NavigationCommand.BackTo -> findNavController().popBackStack(
                    command.destinationId,
                    false
                )
            }
        }
    }

    open fun permissionCheck(permission: String): Boolean {
        return when(permission){
            Manifest.permission.ACCESS_BACKGROUND_LOCATION -> {
                !runningQOrLater || ContextCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
            Manifest.permission.POST_NOTIFICATIONS -> {
                !runningTiramisuOrLater ||
                        ContextCompat.checkSelfPermission(
                            requireContext(),
                            permission) == PackageManager.PERMISSION_GRANTED
            }
            else -> ContextCompat.checkSelfPermission(
                requireContext(),
                permission) == PackageManager.PERMISSION_GRANTED

        }
    }

    fun raisePermissionDeniedSnackBar(message: String) {
        Snackbar.make(
            requireView(),
            message, Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.settings) {
                snackBarWasTapped = true
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data =
                        Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
    }

}