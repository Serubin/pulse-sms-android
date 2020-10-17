package xyz.klinker.messenger.shared.util

import android.app.Activity
import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.work.WorkManager
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.api.implementation.firebase.ScheduledTokenRefreshService
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.pojo.SwipeOption
import xyz.klinker.messenger.shared.service.ContactResyncService
import xyz.klinker.messenger.shared.service.jobs.*

class UpdateUtils(private val context: Activity) {

    private val appVersion: Int
        get() = try {
            val packageInfo = context.packageManager
                    .getPackageInfo(context.packageName, 0)
            packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            -1
        }

    fun checkForUpdate(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val storedAppVersion = sharedPreferences.getInt("app_version", 0)
        ContactResyncService.runIfApplicable(context, sharedPreferences, storedAppVersion)

        if (sharedPreferences.getBoolean("notify-owner-change", true)) {
            sharedPreferences.edit().putBoolean("notify-owner-change", false).commit()
            notifyOwnerChange()
        }

        val currentAppVersion = appVersion

        return if (storedAppVersion != currentAppVersion) {
            Log.v(TAG, "new app version")
            sharedPreferences.edit().putInt("app_version", currentAppVersion).apply()
            true
        } else {
            false
        }
    }

    private fun notifyOwnerChange() {
        AlertDialog.Builder(context)
                .setMessage(if (Account.exists()) R.string.new_owner_with_account else R.string.new_owner_no_account)
                .setPositiveButton(R.string.ok) { _, _ -> }
                .setNegativeButton(R.string.privacy_policy) { _, _ ->
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://messenger.klinkerapps.com/privacy.html")
                    context.startActivity(intent)
                }.show()
    }

    companion object {

        private const val TAG = "UpdateUtil"

        fun rescheduleWork(context: Context) {
            if (Build.FINGERPRINT == "robolectric") {
                return
            }

            WorkManager.getInstance().cancelAllWork()

            CleanupOldMessagesWork.scheduleNextRun(context)
            FreeTrialNotifierWork.scheduleNextRun(context)
            ScheduledMessageJob.scheduleNextRun(context)
            ContactSyncWork.scheduleNextRun(context)
            SubscriptionExpirationCheckJob.scheduleNextRun(context)
            SignoutJob.scheduleNextRun(context)
            ScheduledTokenRefreshService.scheduleNextRun(context)
            SyncRetryableRequestsWork.scheduleNextRun(context)
            RepostQuickComposeNotificationWork.scheduleNextRun(context)
        }

    }
}
