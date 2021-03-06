package com.maku.easydata.ui;

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED
import com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.maku.easydata.EasyDataApplication

import com.maku.easydata.R;
import com.maku.easydata.databinding.ActivitySplashBinding
import com.maku.easydata.databinding.ActivityWelcomeBinding
import com.maku.easydata.utils.MapesaUtils
import com.thekhaeng.pushdownanim.PushDownAnim
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.android.synthetic.main.airtime_fragment.*
import timber.log.Timber

class WelcomeActivity : AppCompatActivity() {

    //glue between the layout and its views
    private lateinit var binding: ActivityWelcomeBinding

    val mContext: Context =
            EasyDataApplication.applicationContext()

    // Creates instance of the manager.
    var appUpdateManager = AppUpdateManagerFactory.create(mContext)

    // Returns an intent object that you use to check for an update.
    val appUpdateInfoTask = appUpdateManager.appUpdateInfo

    private val MY_REQUEST_CODE = 20154

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView<ActivityWelcomeBinding>(this, R.layout.activity_welcome)

        startInAppUpdate()

        // videos button
        PushDownAnim.setPushDownAnimTo( binding.welcome )
        .setOnClickListener{ view ->
            startActivity(Intent(this, LoginActivity::class.java))
        };

//        binding.welcome.setOnClickListener { view ->
//            MapesaUtils.buttonEffect(binding.welcome)
//            startActivity(Intent(this, LoginActivity::class.java))
//        }
    }

    private fun startInAppUpdate() {

        // Before starting an update, register a listener for updates.
        appUpdateManager.registerListener(listener)

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            Timber.d(
                    "appUpdateInfo : packageName :" + appUpdateInfo.packageName() + ", " + "availableVersionCode :" + appUpdateInfo.availableVersionCode() + ", " + "updateAvailability :" + appUpdateInfo.updateAvailability() + ", " + "installStatus :" + appUpdateInfo.installStatus()
            )
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(FLEXIBLE)
            ) {
                requestUpdate(appUpdateInfo)
                Timber.d("UpdateAvailable update is there ")
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_NOT_AVAILABLE) {
                Timber.d("Update 3")
//                popupSnackbarForCompleteUpdate()
            } else {
                Toast.makeText(this, "No Update Available", Toast.LENGTH_SHORT)
                        .show()
                Timber.d("NoUpdateAvailable Update is not there ")
            }

        }

    }

    //request update
    private fun requestUpdate(appUpdateInfo: AppUpdateInfo): Unit {
        try {
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    FLEXIBLE,
                    this,
                    MY_REQUEST_CODE
            )
            onResume()
        } catch (e: IntentSender.SendIntentException) {
            e.printStackTrace()
        }
    }

    //get a callback update status
    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MY_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> if (resultCode != Activity.RESULT_OK) {
                    Toast.makeText(this, "RESULT_OK$resultCode", Toast.LENGTH_LONG).show()
                    Timber.d("RESULT_OK  :"+ "" + resultCode)
                }
                Activity.RESULT_CANCELED -> if (resultCode != Activity.RESULT_CANCELED) {
                    Toast.makeText(this, "RESULT_CANCELED$resultCode", Toast.LENGTH_LONG).show()
                    Timber.d("RESULT_CANCELED  :"+ "" + resultCode)
                }
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> if (resultCode != RESULT_IN_APP_UPDATE_FAILED) {
                    Toast.makeText(
                            this,
                            "RESULT_IN_APP_UPDATE_FAILED$resultCode",
                            Toast.LENGTH_LONG
                    ).show()
                    Timber.d("RESULT_IN_APP_FAILED:"+ "" + resultCode)
                }
            }
        }
    }

    //handling the flexible ui
    var listener =
            InstallStateUpdatedListener { state ->
                Timber.d("installState"+ state.toString())
                if (state.installStatus() == InstallStatus.DOWNLOADED) { // After the update is downloaded, show a notification
                    // and request user confirmation to restart the app.
                    popupSnackbarForCompleteUpdate()
                }
            }



    private fun popupSnackbarForCompleteUpdate() {
        val snackbar = Snackbar.make(
                findViewById(android.R.id.content),
                "An update has just been downloaded.",
                Snackbar.LENGTH_INDEFINITE
        )
        snackbar.apply {
            setAction("RESTART") { appUpdateManager.completeUpdate() }
            setActionTextColor(resources.getColor(R.color.snackbar_action_text_color))
            show()
        }

        snackbar.setActionTextColor(
                getResources().getColor(R.color.snackbar_action_text_color));
        snackbar.show()

    }

    // Checks that the update is not stalled during 'onResume()'.
    // However, you should execute this check at all app entry points.
    override fun onResume() {
        super.onResume()

        appUpdateManager
                .appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->
                    // If the update is downloaded but not installed,
                    // notify the user to complete the update.
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        popupSnackbarForCompleteUpdate()
                    }
                }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(listener);
    }
}
