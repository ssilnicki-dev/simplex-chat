package chat.simplex.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import chat.simplex.common.model.ChatModel;
import chat.simplex.common.model.Chat;
import chat.simplex.common.model.ChatInfo;
import chat.simplex.common.model.Contact;
import chat.simplex.common.model.CreatedConnLink;
import chat.simplex.common.model.UserContactLinkRec;
import chat.simplex.app.SmsForwarder;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsBridge";

    @Override
    public void onReceive(Context context, Intent intent) {

        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length == 0) {
            return;
        }

        logConfirmedContactAddresses();

        for (SmsMessage message : messages) {
            if (message == null) {
                continue;
            }

            String sender = message.getDisplayOriginatingAddress();
            String body = message.getMessageBody();
            if (sender == null) {
                sender = "<unknown sender>";
            }

            Log.i(TAG, "SMS from " + sender + ": " + body);
            SmsForwarder.INSTANCE.forwardIncomingSms(sender, body);
        }
    }

    private void logConfirmedContactAddresses() {
        try {
            SimplexApp app = SimplexApp.Companion.getContext();
            if (app == null) {
                Log.w(TAG, "SimplexApp context is null; cannot log contact addresses");
                return;
            }

            ChatModel chatModel = app.getChatModel();
            if (chatModel == null) {
                Log.i(TAG, "No confirmed contact addresses available");
                return;
            }

            logUserAddress(chatModel);
            logContactAddresses(chatModel);
        } catch (Exception e) {
            Log.e(TAG, "Failed to log confirmed contact addresses", e);
        }
    }

    private void logUserAddress(ChatModel chatModel) {
        if (chatModel.getUserAddress() == null) {
            Log.i(TAG, "No confirmed contact addresses available");
            return;
        }

        UserContactLinkRec userAddress = chatModel.getUserAddress().getValue();
        if (userAddress == null) {
            Log.i(TAG, "No confirmed contact addresses available");
            return;
        }

        CreatedConnLink contactLink = userAddress.getConnLinkContact();
        if (contactLink == null) {
            Log.i(TAG, "No confirmed contact addresses available");
            return;
        }

        Log.i(TAG, "Confirmed contact address (full): " + contactLink.getConnFullLink());
        if (contactLink.getConnShortLink() != null) {
            Log.i(TAG, "Confirmed contact address (short): " + contactLink.getConnShortLink());
        }
    }

    private void logContactAddresses(ChatModel chatModel) {
        if (chatModel.getChats() == null || chatModel.getChats().getValue() == null) {
            Log.i(TAG, "No confirmed contact addresses available");
            return;
        }

        boolean anyLogged = false;
        for (Chat chat : chatModel.getChats().getValue()) {
            if (chat == null) {
                continue;
            }

            ChatInfo chatInfo = chat.getChatInfo();
            if (chatInfo instanceof ChatInfo.Direct) {
                Contact contact = ((ChatInfo.Direct) chatInfo).getContact();
                if (contact != null && contact.getContactLink() != null) {
                    anyLogged = true;
                    Log.i(TAG, "Confirmed contact address for " + contact.getDisplayName() + ": " + contact.getContactLink());
                }
            }
        }

        if (!anyLogged) {
            Log.i(TAG, "No confirmed contact addresses available");
        }
    }

}
