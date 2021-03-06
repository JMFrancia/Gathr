/**************************************************************************************************
 Title : Profile.java
 Author : Gathr Team
 Purpose : Activity which shows a user profile and all fields associated with it
 *************************************************************************************************/

package com.gathr.gathr;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import com.facebook.widget.ProfilePictureView;
import com.gathr.gathr.classes.AuthUser;
import com.gathr.gathr.classes.MyGlobals;
import com.gathr.gathr.classes.SidebarGenerator;
import com.gathr.gathr.database.DatabaseCallback;
import com.gathr.gathr.database.QueryDB;

import org.json.JSONArray;
import org.json.JSONObject;

public class Profile extends ActionBarActivityPlus {
    public String userId = AuthUser.getUserId(this), interests = "", categoryId = "", inst, fb, tw;
    Boolean following = false, blocking = false;
    MyGlobals global = new MyGlobals(this);

    class getUser implements DatabaseCallback {
        public void onTaskCompleted(final String results){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setProfileInfo(results);
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        //Create the instance of the sidebar
        new SidebarGenerator((DrawerLayout)findViewById(R.id.drawer_layout), (ListView)findViewById(R.id.left_drawer),android.R.layout.simple_list_item_1,this);

        Intent i = getIntent();
        String temp = i.getStringExtra("userId"); //get the userId of the current page
        QueryDB DBconn = new QueryDB(this, "getProfile.php");
        if (temp != null)
            userId = temp;

        DBconn.executeQuery(userId, new getUser());
    }

    public void setProfileInfo(String r)
    {
        try {
            ProfilePictureView profilePictureView = (ProfilePictureView) findViewById(R.id.selection_profile_pic);
            profilePictureView.setCropped(true);
            TextView about_me = (TextView) findViewById(R.id.about_me);
            //gets the user information from the database
            if(!r.contains("ERROR")) {
                JSONArray json = new JSONArray(r);
                JSONObject elem1 = json.getJSONObject(0);
                //sets the Action Bar with User Full Name
                setActionBar(elem1.getString("First_Name") + " " + elem1.getString("Last_Name"));
                //Sets user profile picture
                profilePictureView.setProfileId(elem1.getString("Facebook_Id"));
                //Sets user profile about me area or sets it to none if there is nothing
                if (elem1.getString("About_Me").equals(""))
                    about_me.setText("None");
                else
                    about_me.setText(elem1.getString("About_Me"));
                categoryId = elem1.getString("InterestsIds");
                //Sets user's interests
                interests = elem1.getString("Interests");
                //Sets user's interests to None if there is nothing set
                if(interests.equals("null")){
                    interests = "None";
                    categoryId = "";
                }
                ((TextView) findViewById(R.id.my_interests)).setText(interests);
                //Sets user's gathrings to None if there is nothing set
                if(elem1.getString("Past_Events").equals("null"))
                    ((TextView) findViewById(R.id.past_events)).setText("None");
                else
                    ((TextView) findViewById(R.id.past_events)).setText(elem1.getString("Past_Events"));
                //Gets statistics about gathrings attended, organized and the number of followers
                ((TextView) findViewById(R.id.events_created)).append(elem1.getString("Num_Created_Events").trim());
                ((TextView) findViewById(R.id.events_attended)).append(elem1.getString("Num_Joined_Events"));
                ((TextView) findViewById(R.id.followers)).append(elem1.getString("Num_Friends"));

                //get the social networks urls
                inst = elem1.getString("Instagram");
                fb = elem1.getString("Facebook");
                tw = elem1.getString("Twitter");
                setImages(inst, R.id.insta, R.drawable.insta, R.drawable.inst_g);
                setImages(fb, R.id.face, R.drawable.facebook, R.drawable.fb_g);
                setImages(tw, R.id.twit, R.drawable.twit, R.drawable.tw_g);
            }
        }catch(Exception e){
            global.errorHandler(e);
        }
    }
    //Sets the images to greyed out ones if the urls are not populated and to the color one if they are populated
    public void setImages(String sid,int vid, int dr_id,int gdr_id)
    {
        if (!sid.equals("")) {
            (findViewById(vid)).setBackgroundResource(dr_id);
            (findViewById(vid)).setVisibility(View.VISIBLE);
            (findViewById(vid)).setClickable(true);
        } else {
            (findViewById(vid)).setBackgroundResource(gdr_id);
            (findViewById(vid)).setClickable(false);
        }
    }
    //Opening user's instagram profile
    public void goToInsta (View view ) {
        goToUrl ("https://instagram.com/"+ parseUN(inst));
    }
    //Opening user's facebook profile
    public void goToFace (View view) {
        goToUrl ( "https://facebook.com/"+fb);
    }
    //Opening user's twitter profile
    public void goToTwit (View view) {
        goToUrl ( "http://twitter.com/"+ parseUN(tw));
    }
    //Parse the URL ( get rid of the @)
    public String parseUN(String u){
        if (u.charAt(0)=='@')
            u = u.substring(1, u.length());
        return u;
    }
    //Open the URL in the Browser
    private void goToUrl (String url) {
        Uri uriUrl = Uri.parse(url);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(launchBrowser);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profile, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //Blocks/Unblocks the User
        if (id == R.id.action_block) {
            if (!blocking) {
                new QueryDB(this).executeQuery("INSERT INTO BLOCKED_USERS (`User_Id`, `Blocked_User_Id`) VALUES ('" + AuthUser.getUserId(this) + "', '" + userId + "');");
            } else {
                new QueryDB(this).executeQuery("DELETE FROM BLOCKED_USERS WHERE Blocked_User_Id = " + userId + " AND User_Id = " + AuthUser.getUserId(this) + " ;");
            }
            //refresh();
            blocking = !blocking;
            return true;
        }
        //Follows/Unfollows the User
        if (id == R.id.action_follow) {
            if (!following) {
                new QueryDB(this).executeQuery("INSERT INTO FRIENDS (`User_Id`, `Friend_User_Id`) VALUES ('" + AuthUser.getUserId(this) + "', '" + userId + "');");
            } else {
                new QueryDB(this).executeQuery("DELETE FROM FRIENDS WHERE Friend_User_Id = "+userId + " AND User_Id = "+ AuthUser.getUserId(this) +" ;");
            }
            global.getFollowersJSON(1); //our following list has changed, we should update our cache
            refresh();
            return true;
        }
        //Opens Edit Profile Activity and passing selected categories
        if (id == R.id.action_edit_profile) {
            Intent i = new Intent(this, EditProfile.class);
            i.putExtra("category",interests);
            i.putExtra("categoryId",categoryId);
            startActivity(i);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    public void refresh(){
        finish();
        startActivity(getIntent());
    }
    //sets Menu item to Follow/Unfollow
    public boolean onPrepareOptionsMenu(final Menu menu){
        class getFollowing implements DatabaseCallback {
            public void onTaskCompleted(final String results){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            if (!results.contains("ERROR")) {
                                JSONArray json = new JSONArray(results);
                                String count = json.getJSONObject(0).getString("Count").trim();
                                if (count.equals("0")) {
                                    menu.findItem(R.id.action_follow).setTitle("Follow");
                                    following = false;
                                } else {
                                    menu.findItem(R.id.action_follow).setTitle("Unfollow");
                                    following = true;
                                }
                                menu.findItem(R.id.action_follow).setVisible(true);
                            }
                        }catch(Exception e) {
                            if (!e.getMessage().equals("NO RESULTS")) {
                                global.errorHandler(e);}
                        }
                    }
                });
            }
        }
        //sets Menu item to Block/Unblock
        class getBlocking implements DatabaseCallback {
            public void onTaskCompleted(final String results){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            if (!results.contains("ERROR")) {
                                JSONArray json = new JSONArray(results);
                                String count = json.getJSONObject(0).getString("Count").trim();
                                if (count.equals("0")) {
                                    menu.findItem(R.id.action_block).setTitle("Block");
                                    blocking = false;
                                } else {
                                    menu.findItem(R.id.action_block).setTitle("Unblock");
                                    blocking = true;
                                }
                                menu.findItem(R.id.action_block).setVisible(true);
                            }
                        }catch(Exception e) {
                            if (!e.getMessage().equals("NO RESULTS")) {
                                global.errorHandler(e);}
                        }
                    }
                });
            }
        }

        if(!(AuthUser.getUserId(this).equals(userId))){//Shows the Block/Unblock and Follow/Unfollow items if user looks at not his profile
            new QueryDB(this).executeQuery("SELECT COUNT(Friend_User_Id) AS Count FROM FRIENDS WHERE User_Id = " + AuthUser.getUserId(this) + " AND Friend_User_Id = " + userId + ";", new getFollowing());
            new QueryDB(this).executeQuery("SELECT COUNT(Blocked_User_Id) AS Count FROM BLOCKED_USERS  WHERE User_Id = " + AuthUser.getUserId(this) + " AND Blocked_User_Id = " + userId + ";", new getBlocking());
        }else{
            //Shows the Edit Profile item if user looks at his profile
            menu.findItem(R.id.action_edit_profile).setVisible(true);
        }
        return true;
    }

}