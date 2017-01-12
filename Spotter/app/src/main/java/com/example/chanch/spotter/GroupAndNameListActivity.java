package com.example.chanch.spotter;

import android.content.Intent;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GroupAndNameListActivity extends AppCompatActivity {

    private static final String TAG="GANLA";
    private TextView words;
    private EditText name;
    private Button add;
    private LinearLayout scrollList;

    public static String group;
    public static String person;
    public static String id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_and_name_list);

        ImageStorage.DatabaseConnector(getApplicationContext());
        //can access all names

        words=(TextView)findViewById(R.id.GANL_ACTIVITY_textbox);
        name=(EditText)findViewById(R.id.GANL_ACTIVITY_nameBox);
        add=(Button)findViewById(R.id.GANL_ACTIVITY_add);
        scrollList =(LinearLayout)findViewById(R.id.GANL_ACTIVITY_scrollview);
        loadGroups1();

        switch (MainActivity.MODE) {
            case MainActivity.MODE_IMAGE:
                words.setText("Click a group or create a new group");
                name.setText("New Group");add.setText("Add Group");
                add.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        process1(name.getText().toString());
                    }
                });
                break;
            case MainActivity.MODE_REC:
                words.setText("Select a group");
                name.setText("");name.setEnabled(false);add.setEnabled(false);
                name.setVisibility(View.GONE);add.setVisibility(View.GONE);
                break;
            case MainActivity.MODE_GALLERY:
                words.setText("Select a group");
                name.setText("");name.setEnabled(false);add.setEnabled(false);
                name.setVisibility(View.GONE);add.setVisibility(View.GONE);
                break;
            //
            //view images button, cgange to view all images of one person, show in grid.
        }
    }

    @Override
    protected void onDestroy() {
        ImageStorage.Close(this.getBaseContext());
        super.onDestroy();
    }

    private void loadGroups1(){
        String[] list=ImageStorage.GetAllGroups();
        //String[] list={"a","b","c","d","e","f","g","h","i","k","l","m"};
        Log.d(TAG, "loadGroup 1");
        for(String s:list){
            Button b=new Button(this);
            b.setText(s);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    process1(((Button)view).getText().toString() );
                }
            });
            scrollList.addView(b);
        }
    }

    private void process1(String s){
        group=s;
        switch (MainActivity.MODE){
            case MainActivity.MODE_IMAGE:
                loadGroups2();
                break;
            case MainActivity.MODE_REC:
                startActivity(new Intent(GroupAndNameListActivity.this,CameraActivity.class));
                break;
            case MainActivity.MODE_GALLERY:
                startActivity(new Intent(GroupAndNameListActivity.this, GalleryActivity.class));
                //loadGroups2();
                //load gallery based on group
                break;
        }
    }

    private void loadGroups2(){
        scrollList.removeAllViews();
        ImageStorage.GetNamesFromGroup(group);
        String[] list=ImageStorage.tempNameList;
        //String[] list={"1","2","3","4","5","6","7","8","9","10","11","12"};

        switch (MainActivity.MODE) {
            case MainActivity.MODE_IMAGE:
                name.setText("New Person");
                add.setText("Add Name");
                add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                        process2(name.getText().toString());
                        }
                });
                break;
        }
        Log.d(TAG, "loadGroup 2");
        for(String s:list){
            Button b=new Button(this);
            b.setText(s);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    process2(((Button)view).getText().toString() );
                }
            });
            scrollList.addView(b);
        }
    }

    private void process2(String s){
        person = s;
        switch (MainActivity.MODE){
            case MainActivity.MODE_IMAGE:
                String[] list = ImageStorage.tempNameList;
                //String[] list={"1","2","3","4","5","6","7","8","9","10","11","12"};
                boolean tof = false;
                for (String string : list) {
                    if (s.equals(string)) {
                        tof = true;
                    }
                }
                if (!tof) {
                    ImageStorage.AddPersonIntoGroup(group, s);
                }

                startActivity(new Intent(GroupAndNameListActivity.this, CameraActivity.class));
                break;
            case MainActivity.MODE_GALLERY:
                //startActivity(new Intent(GroupAndNameListActivity.this, GalleryActivity.class));
                break;
        }
    }
}
