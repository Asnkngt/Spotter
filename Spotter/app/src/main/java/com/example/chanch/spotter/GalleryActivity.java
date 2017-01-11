package com.example.chanch.spotter;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;
import android.content.Context;
import android.widget.BaseAdapter;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.graphics.BitmapFactory;

public class GalleryActivity extends AppCompatActivity {
    String[] names = ImageStorage.tempNameList;
    private String[][] ids = new String[names.length][];



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        ImageStorage.DatabaseConnector(getApplicationContext());
        for(int a=0;a < names.length ;a++)

        {
            ids[a] = ImageStorage.GetIDsFromName(names[a]);
        }
        GridView gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(new ImageAdapter(this));

        gridview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                Toast.makeText(GalleryActivity.this, "" + position,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


//        Imageview.setImageBitmap(BitmapFactory.decodeFile(id))


    @Override
    protected void onDestroy() {
        ImageStorage.Close(this.getBaseContext());
        super.onDestroy();
    }

    public class ImageAdapter extends BaseAdapter {

        private Context mContext;



        public ImageAdapter(Context c) {
            mContext = c;
        }

        public int getCount() {
            return ids.length;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {

            ImageView imageView;
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }
            for(int a = 0; a<ids.length; a++)
                imageView.setImageBitmap(BitmapFactory.decodeFile(ids[a][position]));

            return imageView;
        }
    }
}
