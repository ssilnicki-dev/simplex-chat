package chat.simplex.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsBridge";

    @Override
    public void onReceive(Context context, Intent intent) {

        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length == 0) {
            return;
        }

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
        }
    }

}
