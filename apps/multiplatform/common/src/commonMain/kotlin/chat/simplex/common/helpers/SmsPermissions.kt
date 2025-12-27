package chat.simplex.common.helpers

expect fun ensureSmsForwardingPermission(
  forwardAddress: String?,
  onDenied: (() -> Unit)? = null,
)
