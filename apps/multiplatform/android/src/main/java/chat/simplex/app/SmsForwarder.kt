package chat.simplex.app

import chat.simplex.common.model.ChatController
import chat.simplex.common.model.ComposedMessage
import chat.simplex.common.model.MsgContent
import chat.simplex.common.platform.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object SmsForwarder {
  private const val TAG = "SmsBridge"
  private const val HARDCODED_FORWARD_ADDRESS = "simplex:sms-forward-target"

  fun forwardIncomingSms(sender: String, body: String) {
    val app = SimplexApp.context
    val forwardAddress = HARDCODED_FORWARD_ADDRESS
    if (forwardAddress.isNullOrBlank()) {
      Log.i(TAG, "SMS forward address not configured; skipping forward")
      return
    }

    CoroutineScope(Dispatchers.Default).launch {
      try {
        SimplexService.scheduleStart(app)

        val messageText = "SMS from $sender: $body"
        ChatController.apiSendMessagesToAddress(
          forwardAddress,
          listOf(ComposedMessage(null, null, MsgContent.MCText(messageText), emptyMap()))
        )
      } catch (e: Exception) {
        Log.e(TAG, "Failed to forward SMS: ${e.message ?: "unknown error"}")
      }
    }
  }
}
