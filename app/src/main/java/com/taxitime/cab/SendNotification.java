package com.taxitime.cab;

import org.json.JSONException;
import org.json.JSONObject;
import com.onesignal.OneSignal;
public class SendNotification {

    public  SendNotification(final String message, final String heading, final String notificationKey) {

        try {
            JSONObject notificationContent = new JSONObject("{'contents': {'en': '" + message + "'}," +
                    "'include_player_ids': ['" + notificationKey + "'], " +
                    "'headings': {'en': '" + heading + "'}}");
            OneSignal.postNotification(notificationContent, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
