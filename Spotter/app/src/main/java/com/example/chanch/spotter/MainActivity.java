package com.example.chanch.spotter;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static int MODE;
    public static final int MODE_IMAGE=0;public static final int MODE_REC=1;public static final int MODE_GALLERY=2;

    public static boolean OPENCVLOADED=false;

    private static final String TAG="Something";

    private Button Pic,Rec,Gal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageStorage.DatabaseConnector(getApplicationContext());

        Pic=(Button)findViewById(R.id.MAIN_ACTIVITY_takePics);
        Rec=(Button)findViewById(R.id.MAIN_ACTIVITY_FaceRec);
        Gal=(Button)findViewById(R.id.MAIN_ACTIVITY_Gallery);

        Pic.setOnClickListener(this);Rec.setOnClickListener(this);Gal.setOnClickListener(this);

        Button temp=(Button)findViewById(R.id.TEMP_DELETE_ALL);
        temp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageStorage.Clear();
            }
        });
    }

    @Override
    protected void onDestroy() {
        ImageStorage.Close(this.getBaseContext());
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.MAIN_ACTIVITY_takePics:
                MODE=MODE_IMAGE;
                startActivity(new Intent(MainActivity.this,GroupAndNameListActivity.class));
                break;
            case R.id.MAIN_ACTIVITY_FaceRec:
                MODE=MODE_REC;
                startActivity(new Intent(MainActivity.this,GroupAndNameListActivity.class));
                break;
            case R.id.MAIN_ACTIVITY_Gallery:
                MODE=MODE_GALLERY;
                startActivity(new Intent(MainActivity.this,GroupAndNameListActivity.class));
                break;
        }
    }
}
