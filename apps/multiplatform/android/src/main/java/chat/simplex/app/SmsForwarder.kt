package chat.simplex.app

import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatController
import chat.simplex.common.model.ChatType
import chat.simplex.common.model.ComposedMessage
import chat.simplex.common.model.MsgContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object SmsForwarder {
  fun forwardIncomingSms(sender: String, body: String) {
    val app = SimplexApp.context
    val chatModel = app.chatModel
    val forwardAddress = ChatController.appPrefs.smsForwardAddress.get()
    if (forwardAddress.isNullOrBlank()) {
      return
    }

    CoroutineScope(Dispatchers.Default).launch {
      try {
        SimplexService.scheduleStart(app)

        val targetChat = findChatByForwardAddress(ensureChatsLoaded(chatModel), forwardAddress)
        if (targetChat == null) {
          return@launch
        }

        val messageText = "$sender: $body"
        ChatController.apiSendMessages(
          targetChat.remoteHostId,
          targetChat.chatInfo.chatType,
          targetChat.chatInfo.apiId,
          null,
          false,
          null,
          listOf(ComposedMessage(null, null, MsgContent.MCText(messageText), emptyMap()))
        )
      } catch (e: Exception) {
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
      cached
    }
  }

  private fun findChatByForwardAddress(chats: List<Chat>, forwardAddress: String): Chat? =
    chats.firstOrNull { chat ->
      val chatInfo = chat.chatInfo
      when (chatInfo) {
        is chat.simplex.common.model.ChatInfo.Direct -> {
          val linkMatches = chatInfo.contact.contactLink == forwardAddress
          val incognitoIdMatches = chatInfo.id == forwardAddress && chatInfo.contact.contactConnIncognito
          linkMatches || incognitoIdMatches
        }
        is chat.simplex.common.model.ChatInfo.Local -> chatInfo.id == forwardAddress
        else -> false
      }
    }
}
