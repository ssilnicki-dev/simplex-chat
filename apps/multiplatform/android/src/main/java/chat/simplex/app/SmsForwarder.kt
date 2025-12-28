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

        val targetChat = findChatByContactLink(ensureChatsLoaded(chatModel), forwardAddress)
        if (targetChat == null) {
          return@launch
        }

        val messageText = "$sender: $body"
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

  private fun findChatByContactLink(chats: List<Chat>, contactLink: String): Chat? =
    chats.firstOrNull { chat ->
      val chatInfo = chat.chatInfo
      if (chatInfo !is chat.simplex.common.model.ChatInfo.Direct) return@firstOrNull false

      val linkMatches = chatInfo.contact.contactLink == contactLink
      val incognitoIdMatches = chatInfo.id == contactLink && chatInfo.contact.contactConnIncognito

      linkMatches || incognitoIdMatches
    }
}
