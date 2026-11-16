package com.netlens.wifi

import android.app.Activity
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle

class NetAdmin : DeviceAdminReceiver() {
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        val admin = ComponentName(context, NetAdmin::class.java)
        dpm.setProfileName(admin, "Dados")
        val pm = context.packageManager
        // No perfil de trabalho: ativa o cofre e esconde o analisador
        pm.setComponentEnabledSetting(ComponentName(context, DiagActivity::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(ComponentName(context, MainActivity::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        dpm.setProfileEnabled(admin)
    }
}

class SetupActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (intent.action == DevicePolicyManager.ACTION_GET_PROVISIONING_MODE) {
            setResult(RESULT_OK, Intent().putExtra(DevicePolicyManager.EXTRA_PROVISIONING_MODE,
                DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE))
        } else setResult(RESULT_OK)
        finish()
    }
}
