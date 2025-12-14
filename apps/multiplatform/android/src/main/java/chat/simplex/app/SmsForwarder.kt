package chat.simplex.app

import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatController
import chat.simplex.common.model.ChatType
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
    val chatModel = app.chatModel
    val forwardAddress = HARDCODED_FORWARD_ADDRESS
    if (forwardAddress.isNullOrBlank()) {
      Log.i(TAG, "SMS forward address not configured; skipping forward")
      return
    }

    CoroutineScope(Dispatchers.Default).launch {
      try {
        SimplexService.scheduleStart(app)

        val targetChat = findChatByContactLink(ensureChatsLoaded(chatModel), forwardAddress)
        if (targetChat == null) {
          Log.w(TAG, "No chat found for configured SMS forward address: $forwardAddress")
          return@launch
        }

        val messageText = "SMS from $sender: $body"
        ChatController.apiSendMessages(
          targetChat.remoteHostId,
          ChatType.Direct,
          targetChat.chatInfo.apiId,
          null,
          false,
          null,
          listOf(ComposedMessage(null, null, MsgContent.MCText(messageText), emptyMap()))
        )
      } catch (e: Exception) {
        Log.e(TAG, "Failed to forward SMS: ${e.message ?: "unknown error"}")
      }
    }
  }

  private suspend fun ensureChatsLoaded(chatModel: chat.simplex.common.model.ChatModel): List<Chat> {
    val cached = chatModel.chats.value
    if (cached.isNotEmpty()) return cached

    return runCatching {
      val refreshed = ChatController.apiGetChats(chatModel.remoteHostId())
      withContext(Dispatchers.Main) { chatModel.chatsContext.updateChats(refreshed) }
      refreshed
    }.getOrElse {
      Log.w(TAG, "Failed to refresh chats for SMS forwarding: ${it.message}")
      cached
    }
  }

  private fun findChatByContactLink(chats: List<Chat>, contactLink: String): Chat? =
    chats.firstOrNull { chat ->
      val chatInfo = chat.chatInfo
      chatInfo is chat.simplex.common.model.ChatInfo.Direct && chatInfo.contact.contactLink == contactLink
    }
}
