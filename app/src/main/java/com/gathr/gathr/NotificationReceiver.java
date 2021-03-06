/**************************************************************************************************
 Title : NotificationReceiver.java
 Author : Gathr Team
 Purpose : Broadcast reciever used to send out notifications for events that may be interesting
 to the user
 *************************************************************************************************/

package com.gathr.gathr;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;

import com.gathr.gathr.classes.AuthUser;
import com.gathr.gathr.classes.MyGlobals;
import com.gathr.gathr.database.DatabaseCallback;
import com.gathr.gathr.database.QueryDB;

import org.json.JSONArray;

public class NotificationReceiver extends BroadcastReceiver {
    private MyGlobals global;

    @Override
    public void onReceive(final Context context, Intent intent) {//Recieved a notification request

        //Update the current notification counter saved in shared preferences
        SharedPreferences sharedPrefs = context.getSharedPreferences("AuthUser", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        int counter = sharedPrefs.getInt("counter",0 );
        editor.putInt("counter",counter + 1);
        editor.apply();
        QueryDB DBconn = new QueryDB(context);
        class getEvent implements DatabaseCallback {
            public void onTaskCompleted(String results) {
                if (results.contains("ERROR")) {
                   //Don't send anything
                } else {
                    try {
                        //Get notification details
                        JSONArray json = new JSONArray(results);
                        final String eventName = json.getJSONObject(0).getString("Name");
                        final String eventId = json.getJSONObject(0).getString("Id");
                        final String eventDate = json.getJSONObject(0).getString("Date");
                        final String eventTime = json.getJSONObject(0).getString("Time");
                        //Send a push notification with those details
                        PushNotification(Integer.parseInt(eventId), "Gathring For You!", eventName, eventDate + " " + eventTime, ViewGathring.class, context);

                    } catch (Exception e) {
                        global = new MyGlobals(context);
                        global.errorHandler(e);
                    }
                }

            }
        }
        try {
            if(counter % 72 == 0) //Every 12 hours, send a notification for an interesting event
                DBconn.executeQuery("SELECT Id, Name, Date, Time, COUNT(My_Interests) AS Weight FROM (SELECT EVENTS.Id, EVENTS.Name, EVENTS.Date, EVENTS.Time , EC.Category_Id AS Event_Category FROM EVENTS LEFT OUTER JOIN (EVENT_CATEGORIES AS EC, JOINED_EVENTS AS JE) ON (EC.Event_Id = Id = JE.Event_Id) WHERE User_Id <> 7 AND Capacity > Population AND Status = 'OPEN') AS J1 JOIN ((SELECT Category_Id AS My_Interests FROM USERS JOIN USER_INTERESTS ON User_Id = Id WHERE Id = 7) UNION (SELECT Category_Id AS My_Past_Interests FROM JOINED_EVENTS AS JE2 JOIN EVENT_CATEGORIES AS EC2 ON EC2.Event_Id = JE2.Event_Id WHERE User_Id = 7) UNION (SELECT Category_Id AS My_Searched_Interests FROM SEARCHES JOIN SEARCH_CATEGORY ON Id = Search_Id WHERE User_Id = 7)) AS J2 WHERE My_Interests = Event_Category AND (Date > DATE(NOW()) OR (Date = DATE(NOW()) AND Time > TIME(NOW()))) GROUP BY Id ORDER BY Weight DESC, Date DESC, Time ASC LIMIT 1",new getEvent());
            if(counter % 3 == 0){ //Every 2 hours send a notification if someone you are following has created an event
                DBconn.executeQuery("SELECT * FROM EVENTS, USERS, FRIENDS  WHERE FRIENDS.User_Id = USERS.Id AND FRIENDS.User_Id = " + AuthUser.getUserId(context) +" AND Organizer = Friend_User_Id");
            }

        } catch (Exception e) {
            global = new MyGlobals(context);
            global.errorHandler(e);
        }
    }

    public void PushNotification(int uniqueID, String tickerText, String nTitle, String nText, Class<?> cls,  Context c){
        //Lets you build new notification
        NotificationCompat.Builder notification;

        notification = new NotificationCompat.Builder(c);
        notification.setAutoCancel(true); //This is to make the notification go away when you get to the proper intent screen
        notification.setSmallIcon(R.mipmap.icon); //Set picture or logo of app for the notification
        notification.setTicker(tickerText); //Notification Text
        notification.setWhen(System.currentTimeMillis()); // Notification Time
        notification.setContentTitle(nTitle);
        notification.setContentText(nText);
        notification.setDefaults(Notification.DEFAULT_VIBRATE);
        notification.setDefaults(Notification.DEFAULT_SOUND);

        //send them back to the proper activity
        Intent intent = new Intent(c, cls);

        //Gives device access to all intents in the app

        intent.putExtra("eventId", Integer.toString(uniqueID));
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setContentIntent(pendingIntent);

        //Builds the notification and issues it
        NotificationManager nm = (NotificationManager) c.getSystemService(c.NOTIFICATION_SERVICE);
        nm.notify(uniqueID, notification.build());

    }
}

