package chat.simplex.common.views.usersettings

import SectionBottomSpacer
import SectionDividerSpaced
import SectionTextFooter
import SectionView
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalUriHandler
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.ChatModel
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.helpers.ensureSmsForwardingPermission
import chat.simplex.common.platform.*
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import chat.simplex.common.views.TerminalView
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR

private const val SMS_FORWARD_LOG_TAG = "SMS_FORWARD"

@Composable
fun DeveloperView(withAuth: (title: String, desc: String, block: () -> Unit) -> Unit
) {
  val m = chatModel
  ColumnWithScrollBar {
    val uriHandler = LocalUriHandler.current
    AppBarTitle(stringResource(MR.strings.settings_developer_tools))
    val developerTools = m.controller.appPrefs.developerTools
    val devTools = remember { developerTools.state }
    val unchangedHints = mutableStateOf(unchangedHintPreferences())
    SectionView {
      InstallTerminalAppItem(uriHandler)
      ChatConsoleItem { withAuth(generalGetString(MR.strings.auth_open_chat_console), generalGetString(MR.strings.auth_log_in_using_credential)) { ModalManager.start.showModalCloseable { TerminalView(false) } } }
      ResetHintsItem(unchangedHints)
      SettingsPreferenceItem(painterResource(MR.images.ic_code), stringResource(MR.strings.show_developer_options), developerTools)
      SectionTextFooter(
        generalGetString(if (devTools.value) MR.strings.show_dev_options else MR.strings.hide_dev_options) + " " +
            generalGetString(MR.strings.developer_options)
      )
    }
    if (devTools.value) {
      SectionDividerSpaced(maxTopPadding = true)
      SectionView(stringResource(MR.strings.developer_options_section).uppercase()) {
        SettingsActionItemWithContent(painterResource(MR.images.ic_breaking_news), stringResource(MR.strings.debug_logs)) {
          DefaultSwitch(
            checked = remember { appPrefs.logLevel.state }.value <= LogLevel.DEBUG,
            onCheckedChange = { appPrefs.logLevel.set(if (it) LogLevel.DEBUG else LogLevel.WARNING) }
          )
        }
        SettingsPreferenceItem(painterResource(MR.images.ic_drive_folder_upload), stringResource(MR.strings.confirm_database_upgrades), m.controller.appPrefs.confirmDBUpgrades)
        if (appPlatform.isAndroid) {
          SettingsPreferenceItem(
            painterResource(MR.images.ic_visibility_off),
            stringResource(MR.strings.allow_screenshots),
            appPreferences.allowScreenshots
          ) { allow ->
            platform.androidSetAllowScreenshots(allow)
          }
          SmsForwardAddressSetting(m)
        }
        if (appPlatform.isDesktop) {
          TerminalAlwaysVisibleItem(m.controller.appPrefs.terminalAlwaysVisible) { checked ->
            if (checked) {
              withAuth(generalGetString(MR.strings.auth_open_chat_console), generalGetString(MR.strings.auth_log_in_using_credential)) {
                m.controller.appPrefs.terminalAlwaysVisible.set(true)
              }
            } else {
              m.controller.appPrefs.terminalAlwaysVisible.set(false)
            }
          }
        }
        SettingsPreferenceItem(painterResource(MR.images.ic_report), stringResource(MR.strings.show_internal_errors), appPreferences.showInternalErrors)
        SettingsPreferenceItem(painterResource(MR.images.ic_avg_pace), stringResource(MR.strings.show_slow_api_calls), appPreferences.showSlowApiCalls)
      }
    }
    SectionDividerSpaced(maxTopPadding = true)
    SectionView(stringResource(MR.strings.deprecated_options_section).uppercase()) {
      val simplexLinkMode = chatModel.controller.appPrefs.simplexLinkMode
      SimpleXLinkOptions(chatModel.simplexLinkMode, onSelected = {
        simplexLinkMode.set(it)
        chatModel.simplexLinkMode.value = it
      })
      SectionBottomSpacer()
    }
  }
}

fun showInDevelopingAlert() {
  AlertManager.shared.showAlertMsg(
    title = generalGetString(MR.strings.in_developing_title),
    text = generalGetString(MR.strings.in_developing_desc)
  )
}

@Composable
private fun SmsForwardAddressSetting(m: ChatModel) {
  val smsForwardAddress = remember { appPrefs.smsForwardAddress.state }
  var lastSmsForwardAddress by remember { mutableStateOf(smsForwardAddress.value) }

  val addressOptions = remember(m.userAddress.value, m.chats.value, smsForwardAddress.value) {
    val options = mutableListOf<Pair<String?, String>>()
    options.add(null to generalGetString(MR.strings.sms_forward_address_none))

    val excludedContactNames = setOf("SimpleX Status", "Ask SimpleX Team")
    m.chats.value?.forEach { chat ->
      val directChat = chat.chatInfo as? ChatInfo.Direct
      val contact = directChat?.contact
      if (contact != null) {
        val contactLink = contact.contactLink
        val optionAddress = when {
          !contactLink.isNullOrBlank() -> contactLink
          contact.contactConnIncognito -> directChat.id
          else -> null
        }
        val displayName = contact.displayName
        if (!optionAddress.isNullOrBlank() && displayName !in excludedContactNames) {
          options.add(optionAddress to displayName)
        }
      } else if (chat.chatInfo is ChatInfo.Local) {
        val localInfo = chat.chatInfo as ChatInfo.Local
        options.add(localInfo.id to localInfo.displayName)
      }
    }

    val current = smsForwardAddress.value
    if (current != null && options.none { it.first == current }) {
      options.add(current to current)
    }

    options
  }

  ExposedDropDownSettingRow(
    title = stringResource(MR.strings.sms_forward_address),
    values = addressOptions,
    selection = smsForwardAddress,
    icon = painterResource(MR.images.ic_forward_to_inbox),
    onSelected = { address ->
      val currentAddress = smsForwardAddress.value
      if (address != currentAddress) {
        appPrefs.smsForwardAddress.set(address)
        ensureSmsForwardingPermission(address) {
          appPrefs.smsForwardAddress.set(null)
        }
      } else {
      }
    }
  )
}
