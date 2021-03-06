/**************************************************************************************************
 Title : Settings.java
 Author : Gathr Team
 Purpose : Settings activity, allowing the user the log out and turn notifications on/off
 *************************************************************************************************/

package com.gathr.gathr;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.gathr.gathr.classes.MyGlobals;
import com.gathr.gathr.classes.SidebarGenerator;

public class Settings extends ActionBarActivityPlus {

    MyGlobals global = new MyGlobals(this);
    Context context = this;

    private PendingIntent pendingIntent;
    private AlarmManager manager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setActionBar("Settings");
        ProgressBar spinner;
        spinner = (ProgressBar)findViewById(R.id.progressBar1);
        spinner.setVisibility(View.GONE);

        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);

        new SidebarGenerator((DrawerLayout)findViewById(R.id.drawer_layout), (ListView)findViewById(R.id.left_drawer),android.R.layout.simple_list_item_1,this);

        //Set the toggle based on the shared preferences
        SharedPreferences sharedPrefs = getSharedPreferences("AuthUser", MODE_PRIVATE);
        ToggleButton toggle = (ToggleButton)findViewById(R.id.notificationsButton);
        toggle.setChecked(sharedPrefs.getBoolean("notifications", true));


    }
    //Sets up the alarm manager to send/check for notifications every 10 minutes
    public void getNotifications(){
        manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        int interval = 600000; //10 minutes

        manager.setRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis(),interval,pendingIntent);
        Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
    }

    public void onToggleClicked(View view) { //What we do when the toggle button is clicked
        // Is the toggle on?
        boolean on = ((ToggleButton) view).isChecked();
        SharedPreferences settings = this.getSharedPreferences("AuthUser", 0);
        SharedPreferences.Editor editor = settings.edit();
        if (on) {
            //Begin sending notifications
            Intent notificationIntent = new Intent(this, NotificationReceiver.class);
            pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent,0);
            getNotifications();

            //Remember the preferences of notifications on and set the counter to 0
            editor.putBoolean("notifications", true);
            editor.putInt("counter",0);
            editor.apply();

        } else {//toggle has been turned off
            editor.putBoolean("notifications", false);
            editor.apply();
            try {
                //Create that same broadcast to cancel the notification alarm since these values are not saved
                Intent notificationIntent = new Intent(this, NotificationReceiver.class);
                pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent,0);
                manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
                int interval = 10000;
                manager.setRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis(),interval,pendingIntent);

                manager.cancel(pendingIntent);
            }catch(Exception e){
                global = new MyGlobals(context);
                global.errorHandler(e);
            }
        }
    }
    //Logs the user out when the logout button is clicked
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override

        public void call(Session session, SessionState state, Exception exception) {
            if (session != null && (session.isClosed()) ) {
                //clear the users preferences
                SharedPreferences settings = context.getSharedPreferences("AuthUser", Context.MODE_PRIVATE);
                settings.edit().clear().apply();

                global.tip("You are now logged out!");
                //go back to the main page(Activity)
                Intent i = new Intent(context, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();

            }
            uiHelper.onResume();
        }
    };

    private UiLifecycleHelper uiHelper;

    @Override
    public void onResume() {
        super.onResume();
        uiHelper.onResume();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }


}