/**************************************************************************************************
 Title : ViewGathring.java
 Author : Gathr Team
 Purpose : Activity which represents a Gathring and all information associated with it. This
 infomration is loaded from the database based on the Gathring id. Button functionality
 to share the gathring, join/leave, and enter the chatroom are found here as well.
 *************************************************************************************************/

package com.gathr.gathr;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import org.json.JSONArray;
import android.content.Intent;

import com.devsmart.android.ui.HorizontalListView;
import com.facebook.widget.ProfilePictureView;
import com.gathr.gathr.chat.ui.activities.SplashActivity;
import com.gathr.gathr.classes.AuthUser;
import com.gathr.gathr.classes.Event;
import com.gathr.gathr.classes.MyGlobals;
import com.gathr.gathr.classes.SidebarGenerator;
import com.gathr.gathr.database.DatabaseCallback;
import com.gathr.gathr.database.QueryDB;

public class ViewGathring extends ActionBarActivityPlus {

    private Context c = this;
    private String[] attendees, attendeeIds;
    private boolean partOf = false, loggedin = false, cancelled = false;
    private QueryDB DBConn = new QueryDB(this);
    private String eventId = "1", event_json = "", eventOrganizer, userId = AuthUser.getUserId(this);
    private MyGlobals global = new MyGlobals(this);
    private Event event;
    HorizontalListView listview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_gathring);
        setActionBar("View Gathrings");
        listview = (HorizontalListView) findViewById(R.id.listview); //List of others in the Gathring
        try{
            Intent intent = getIntent();

            Bundle extras = intent.getExtras();
            if(extras != null) //If we get to this page from inside the app (they should be passing EventId)
                eventId = (String)extras.get("eventId");

            //If we get to this page from http://wegathr.tk/viewEvent/{eventId}
            Uri data = intent.getData();
            if(data != null) {
                String[] url = data.toString().split("/");
                eventId = url[(url.length - 1)];
            }

            if(AuthUser.getUserId(this) != null){ //Is this person logged in?
                loggedin = true;
                new SidebarGenerator((DrawerLayout)findViewById(R.id.drawer_layout), (ListView)findViewById(R.id.left_drawer),android.R.layout.simple_list_item_1,this);
            }

            DBConn.executeQuery("SELECT Facebook_Id, Id FROM (USERS JOIN (SELECT User_Id FROM JOINED_EVENTS WHERE Event_Id = " + eventId + " )  AS JOINED) WHERE Id = User_Id", new loadAttendees());


        }catch(Exception e){
            global.errorHandler(e);
        }
    }
    public void joinOrLeave(View view){ //Joining or leaving an event
        if(!loggedin){
            Intent intent = new Intent(this, MainActivity.class); // Send them back to login page
            startActivity(intent);
            return;
        }
        if (!partOf) {
            DBConn.executeQuery("INSERT INTO JOINED_EVENTS(User_Id, Event_Id) VALUES (" + userId + "," + eventId + ");");
            global.tip("Welcome to the Gathring");
        } else {
            DBConn.executeQuery("DELETE FROM JOINED_EVENTS WHERE User_Id = " + userId + " and Event_Id = " + eventId + ";");
            global.tip("You have left the Gathring");
        }
        refresh();
    }
    public void refresh(){
        finish();
        startActivity(getIntent());
    }
    public void showOnMap(View view){ //Show the Gathring on the maps page
        Intent i = new Intent(this, MapsActivity.class);
        i.putExtra("event_json", event_json);
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_view_gathring, menu);
        return true;
    }

    public void share(View v){ //Share the Gathring
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this event on Gathr: http://www.wegathr.tk/viewEvent/"+ eventId +" !" );
        startActivity(Intent.createChooser(shareIntent, "Share this event"));
    }

    public void openChat(View v){ //Go to the chatroom
        Intent i = new Intent(this, SplashActivity.class);
        i.putExtra("EventId", eventId );
        i.putExtra("EventName", event.name );
        startActivity(i);
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu){//Set menu options
        if(!cancelled && userId.equals(eventOrganizer)){
            menu.findItem(R.id.action_edit).setVisible(true);
            menu.findItem(R.id.action_cancel).setVisible(true);
        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_edit) {//If edit event option is selected, go to create event activity with prefilled information
            Intent i = new Intent(this, CreateEvent.class);
            i.putExtra("prefill", event_json );
            startActivity(i);
            finish();
            return true;
        }

        if (id == R.id.action_share) {
            share(item.getActionView());
            return true;
        }
        if (id == R.id.action_cancel) {
            MsgBox("Confirm","Are you sure you want to cancel this event? You can not undo this action.", this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void MsgBox(String Title, String Message, final Context c){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(Title);
        alertDialogBuilder.setMessage(Message).setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        DBConn.executeQuery("UPDATE EVENTS SET Status = 'CLOSED' WHERE Id = " + eventId);
                        global.tip("Event was Cancelled!");
                        Intent i = new Intent(c, GathringsListActivity.class);
                        startActivity(i);
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    class loadAttendees implements DatabaseCallback {
        public void onTaskCompleted(String results) {
            QueryDB getEvent = new QueryDB(c, "getEvent.php");
            getEvent.executeQuery(eventId, new loadEvent());
            if (results.contains("ERROR")) {
                attendees = new String[]{};
                attendeeIds = new String[]{};
            } else {
                try {
                    JSONArray json = new JSONArray(results);
                    int numFriends = json.length();
                    attendeeIds = new String[numFriends];
                    attendees = new String[numFriends];

                    for (int i = 0; i < json.length(); i++) {
                        attendeeIds[i] = json.getJSONObject(i).getString("Id");
                        attendees[i] = json.getJSONObject(i).getString("Facebook_Id");
                    }
                } catch (Exception e) {
                    global.errorHandler(e);
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listview.setAdapter(mAdapter);
                }
            });
        }
    }
    class loadEvent implements DatabaseCallback{//Loads all event information and sets the corresponding fields
        public void onTaskCompleted(String results){
            try {
                event_json = results;
                event = new Event(event_json);
                eventOrganizer = event.event_organizer;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Set Event Details
                        ((TextView) findViewById(R.id.gathring_name_text)).setText(event.name);
                        ((TextView) findViewById(R.id.gathring_description_text)).setText(event.description);
                        ((TextView) findViewById(R.id.gathring_address_text)).setText(event.address);
                        ((TextView) findViewById(R.id.gathring_category_text)).setText(event.categories);
                        ((TextView) findViewById(R.id.gathring_limit_text)).setText(event.pop + "/" + event.capacity);
                        ((TextView) findViewById(R.id.gathring_time_text)).setText(global.normalTime(event.time));
                        ((TextView) findViewById(R.id.gathring_date_text)).setText(global.nDate(event.date));

                        TextView buttonText = (TextView) findViewById(R.id.join_leave_button);
                        if (event.status.equals("CLOSED")) { //Event Cancelled
                            ((TextView) findViewById(R.id.gathring_date_text)).setText("Cancelled");
                            ((TextView) findViewById(R.id.gathring_time_text)).setText("");
                            buttonText.setVisibility(View.GONE); //Cannot Join
                            cancelled = true;
                        }else if(!loggedin){ //Not logged into the app
                            buttonText.setText("Login");
                        }else { //They are logged in
                            if (!eventOrganizer.equals(userId)) {
                                partOf = false;
                                for (String attendee : attendeeIds) {
                                    if (attendee.equals(userId)) {
                                        partOf = true;
                                        buttonText.setText("Leave"); //They can't join, they must leave
                                        (findViewById(R.id.joinChat)).setVisibility(View.VISIBLE); //They can Chat
                                        break;
                                    }
                                }

                            } else { // They are the organizer
                                partOf = true;
                                buttonText.setVisibility(View.GONE); //They cannot join/leave
                                (findViewById(R.id.joinChat)).setVisibility(View.VISIBLE); // They Can Chat
                            }
                        }
                    }
                });
            }catch(Exception e){
                global.errorHandler(e);
            }
        }
    }
    private BaseAdapter mAdapter = new BaseAdapter() {

        @Override
        public int getCount() {
            return attendees.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View retval = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem, null);
            ProfilePictureView imgView = (ProfilePictureView) retval.findViewById(R.id.attendee_profile_pic);
            //new MyGlobals(context).tip(images[position]);
            imgView.setCropped(true);
            imgView.setProfileId(attendees[position]);

            imgView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    String friendID = attendeeIds[position];
                    Intent i = new Intent(c, Profile.class);
                    i.putExtra("userId", friendID);
                    startActivity(i);
                }
            });

            return retval;
        }
    };
}