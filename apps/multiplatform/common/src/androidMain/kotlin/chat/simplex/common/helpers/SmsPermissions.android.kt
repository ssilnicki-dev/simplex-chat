package chat.simplex.common.helpers

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import chat.simplex.common.platform.androidAppContext
import chat.simplex.common.views.helpers.AlertManager
import chat.simplex.common.views.helpers.generalGetString
import chat.simplex.res.MR

actual fun ensureSmsForwardingPermission(forwardAddress: String?) {
  if (forwardAddress.isNullOrBlank()) return

  val granted = ContextCompat.checkSelfPermission(
    androidAppContext,
    Manifest.permission.READ_SMS
  ) == PackageManager.PERMISSION_GRANTED

  if (!granted) {
    AlertManager.shared.showAlertMsg(
      title = generalGetString(MR.strings.sms_forward_permission_title),
      text = generalGetString(MR.strings.sms_forward_permission_desc),
      confirmText = generalGetString(MR.strings.permissions_open_settings),
      onConfirm = { androidAppContext.openAppSettingsInSystem() },
    )
  }
}
