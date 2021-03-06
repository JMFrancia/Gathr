/**************************************************************************************************
 Title : SearchEvents.java
 Author : Gathr Team
 Purpose : Allows user to search for events based on various criteria
 *************************************************************************************************/

package com.gathr.gathr;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.gathr.gathr.classes.MyGlobals;
import com.gathr.gathr.classes.SidebarGenerator;


public class SearchEvents extends ActionBarActivityPlus {
    public String category, categoryId;
    EditText my_interests;
    MyGlobals global = new MyGlobals();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBar("Search Gathrings");
        setContentView(R.layout.activity_search_events);
        new SidebarGenerator((DrawerLayout)findViewById(R.id.drawer_layout), (ListView)findViewById(R.id.left_drawer),android.R.layout.simple_list_item_1,this);
        my_interests = (EditText) findViewById(R.id.et_categories);
    }

    public void getTimeFilter(View v){
        TimePickerFragment newFragment = new TimePickerFragment((TextView)findViewById(R.id.vw_time));
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public void openCategory(View v){
        Intent i = new Intent(this, ListViewMultipleSelectionActivity.class);
        i.putExtra("from",SearchEvents.class);
        i.putExtra("categoryId", categoryId);
        startActivityForResult(i, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 0) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                category = data.getStringExtra("category");
                categoryId = data.getStringExtra("categoryId");
                my_interests.setText(category);
            }
        }
    }
    public void search(View v){
        EditText locEdit = (EditText)findViewById(R.id.et_location);
        TextView timeEdit = (TextView)findViewById(R.id.vw_time);
        String location = locEdit.getText().toString(),
                time = timeEdit.getText().toString();

        Intent intent = new Intent(getApplicationContext(),MapsActivity.class );
        intent.putExtra("category", category);
        intent.putExtra("categoryId", categoryId);
        intent.putExtra("location",location);
        intent.putExtra("time", time);

        // start the ResultActivity
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

}
