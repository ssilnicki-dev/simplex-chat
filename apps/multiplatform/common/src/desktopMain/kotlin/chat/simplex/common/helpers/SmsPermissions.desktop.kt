package chat.simplex.common.helpers

actual fun ensureSmsForwardingPermission(
  forwardAddress: String?,
  onDenied: (() -> Unit)?,
) = Unit
