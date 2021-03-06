package com.example.chanch.spotter;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.graphics.BitmapFactory;

public class GalleryActivity extends AppCompatActivity {

    private static String TAG="GALLERY";

    private String[] names;

    private Context context;

    private BitmapFactory.Options options=new BitmapFactory.Options();

    private LinearLayout imageViewer;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        context=getApplicationContext();
        ImageStorage.DatabaseConnector(getApplicationContext());

        ImageStorage.LoadNamesFromGroup(GroupAndNameListActivity.group);
        names=ImageStorage.tempNameList;
        //ids=ImageStorage.GetIDsFromName(GroupAndNameListActivity.person);

        options.inPreferredConfig= Bitmap.Config.RGB_565;
        //options.inJustDecodeBounds=true;
        options.inSampleSize=8;

        imageViewer=(LinearLayout)findViewById(R.id.GALLERY_ACTIVITY_ImageList);

        ViewGroup.LayoutParams layout=new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        for(String name:names) {
            HorizontalScrollView scrollView=new HorizontalScrollView(context);
            scrollView.setLayoutParams(layout);

            LinearLayout list=new LinearLayout((context));
            list.setOrientation(LinearLayout.HORIZONTAL);
            list.setLayoutParams(layout);

            scrollView.addView(list);

            String[] ids=ImageStorage.GetIDsFromName(name);
            for (String id : ids) {
                ImageView imageView = new ImageView(context);
                imageView.setImageBitmap(BitmapFactory.decodeFile(id, options));

                list.addView(imageView);
            }

            imageViewer.addView(scrollView);
        }

    }


//        Imageview.setImageBitmap(BitmapFactory.decodeFile(id))


    @Override
    protected void onDestroy() {
        ImageStorage.Close(this.getBaseContext());
        super.onDestroy();
    }

    /*
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
                imageView.setImageBitmap(BitmapFactory.decodeFile(ids[a],options));

            return imageView;
        }
    }
*/
    private Toast previousToast;
    public void makeToast(String out){
        if(previousToast!=null) {
            previousToast.cancel();
        }

        previousToast=Toast.makeText(getApplicationContext(),out,Toast.LENGTH_SHORT);
        previousToast.show();
    }

}
