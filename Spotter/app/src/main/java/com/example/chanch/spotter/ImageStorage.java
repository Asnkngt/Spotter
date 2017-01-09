package com.example.chanch.spotter;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;

import static android.content.ContentValues.TAG;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.BitmapFactory;
import android.util.Log;

/*
import org.opencv.android.OpenCVLoader;
import org.opencv.videoio.VideoCapture;
*/


public class ImageStorage {

    public static String[] temp;

    private static SQLiteDatabase personDatabase;
    private static SQLiteDatabase groupDatabase;

    private static SQLiteOpenHelper personDatabaseOpenHelper;
    private static SQLiteOpenHelper groupDatabaseOpenHelper;

    private static Context currentContext;

    private static class GroupDataBase{
        public static String TABLE_NAME="Group_Database";
        public static String GROUP_NAME="Group_Name";
        public static String PERSON_NAME="Person_Name";
        public static String BOOLEAN_CHECK="Boolean_Check";
        public static String BLOB_DATA="Blob_Data";
    }


    private static class PersonDatabase{
        public static String TABLE_NAME="Person_Database";
        public static String PERSON_NAME="Person_Name";
        public static String IMAGE_ID="Image_ID";
    }

    public static void DatabaseConnector(Context context)
    {
        currentContext=context;
        personDatabaseOpenHelper=new SQLiteOpenHelper(currentContext,PersonDatabase.TABLE_NAME,null,1) {
            @Override
            public void onCreate(SQLiteDatabase sqLiteDatabase) {
                String createQuery = "CREATE TABLE IF NOT EXISTS " +PersonDatabase.TABLE_NAME+
                        " ("+PersonDatabase.PERSON_NAME+ " TEXT NOT NULL,"+
                        PersonDatabase.IMAGE_ID+" TEXT NOT NULL);";
                sqLiteDatabase.execSQL(createQuery);
            }
            @Override
            public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            }
        };
        groupDatabaseOpenHelper=new SQLiteOpenHelper(currentContext,GroupDataBase.GROUP_NAME,null,1) {
            @Override
            public void onCreate(SQLiteDatabase sqLiteDatabase) {
                String createQuery="CREATE TABLE IF NOT EXISTS "+GroupDataBase.TABLE_NAME+
                        " ("+GroupDataBase.GROUP_NAME+" TEXT NOT NULL,"+
                        GroupDataBase.PERSON_NAME+" TEXT NOT NULL, " +
                        GroupDataBase.BOOLEAN_CHECK+" TINYINT NOT NULL, "+
                        GroupDataBase.BLOB_DATA+" BLOB);";
                sqLiteDatabase.execSQL(createQuery);
            }
            @Override
            public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            }
        };
        Log.d("Something","SQL OPENED");
    }

    public static void Close(Context context) {
        if(context!=currentContext){
            return;
        }
        try {
            personDatabase.close();
            groupDatabase.close();
            Log.d("Something","SQL CLOSED");
        } catch (Exception e) {
        }
    }

    public static void Clear(){
        groupDatabase=groupDatabaseOpenHelper.getWritableDatabase();
        //groupDatabase.execSQL("DROP TABLE IF EXISTS "+GroupDataBase.TABLE_NAME+";");
        groupDatabase.execSQL("DELETE FROM "+GroupDataBase.TABLE_NAME+";");
        personDatabase=personDatabaseOpenHelper.getWritableDatabase();
        //personDatabase.execSQL("DROP TABLE IF EXISTS "+PersonDatabase.TABLE_NAME+";");
        personDatabase.execSQL("DELETE FROM "+PersonDatabase.TABLE_NAME+";");
    }

    //Group database
    public static void AddPersonIntoGroup(String group, String name){
        groupDatabase=groupDatabaseOpenHelper.getWritableDatabase();
        groupDatabase.execSQL("INSERT INTO "+GroupDataBase.TABLE_NAME+"("+
                        GroupDataBase.GROUP_NAME+", "+
                        GroupDataBase.PERSON_NAME+", " +
                        GroupDataBase.BOOLEAN_CHECK+", "+
                        GroupDataBase.BLOB_DATA+") VALUES (?,?,?,?)",
                new Object[]{group,name,0,new byte[0]});
    }

    public static String[] tempNameList=new String[0];
    public static Boolean[] tempBooleanList=new Boolean[0];
    //o=false
    //1=true
    public static void GetNamesFromGroup(String group){
        ArrayList<String> listNames=new ArrayList<String>();
        ArrayList<Boolean> listBools=new ArrayList<Boolean>();

        groupDatabase=groupDatabaseOpenHelper.getReadableDatabase();
        Cursor c=groupDatabase.rawQuery("SELECT DISTINCT "+GroupDataBase.PERSON_NAME +", "+ GroupDataBase.BOOLEAN_CHECK+" FROM "+GroupDataBase.TABLE_NAME+" WHERE "+GroupDataBase.GROUP_NAME+" ='"+group+"'",null);
        c.moveToLast();
        int last=c.getPosition();
        c.moveToFirst();
        while(c.getPosition()<=last){
            listNames.add(c.getString(0));
            if(c.getInt(1)==0){
                listBools.add(false);
            }else{
                listBools.add(true);
            }
            c.moveToNext();
        }
        tempNameList=listNames.toArray(new String[listNames.size()]);
        tempBooleanList=listBools.toArray(new Boolean[listBools.size()]);

        c.close();
    }

    public static String[] GetAllGroups(){
        ArrayList<String> list=new ArrayList<String>();
        groupDatabase=groupDatabaseOpenHelper.getReadableDatabase();
        Cursor c=groupDatabase.rawQuery("SELECT DISTINCT "+GroupDataBase.GROUP_NAME +" FROM "+GroupDataBase.TABLE_NAME+";",null);
        c.moveToLast();
        int last=c.getPosition();
        c.moveToFirst();
        while(c.getPosition()<=last){
            list.add(c.getString(0));
            c.moveToNext();
        }
        c.close();
        return list.toArray(new String[list.size()]);
    }

    public static float[] getBlobData(String name){
        groupDatabase=groupDatabaseOpenHelper.getReadableDatabase();
        Cursor c=groupDatabase.rawQuery("SELECT "+GroupDataBase.BLOB_DATA +" FROM "+GroupDataBase.TABLE_NAME+" WHERE "+GroupDataBase.PERSON_NAME+" ='"+name+"' LIMIT 1",null);
        c.moveToFirst();
        byte[] blob=c.getBlob(0);
        ByteBuffer bb=ByteBuffer.wrap(blob);
        bb.rewind();
        float[] out=new float[blob.length/4];
        bb.asFloatBuffer().get(out);
        c.close();
        return out;
    }

    public static void storeBlobData(String name, float[] data){
        groupDatabase=groupDatabaseOpenHelper.getWritableDatabase();
        byte[] out=new byte[data.length*4];
        ByteBuffer bb=ByteBuffer.allocate(data.length*4);
        for(float f:data){
            bb.putFloat(f);
        }
        bb.rewind();
        bb.get(out);
        groupDatabase.execSQL("UPDATE "+GroupDataBase.TABLE_NAME+" SET "+GroupDataBase.BLOB_DATA +"= ?"+
                " WHERE "+GroupDataBase.PERSON_NAME+" ='"+name+"', "+GroupDataBase.BOOLEAN_CHECK+"=1",new Object[]{out});
    }

    //person database
    //adds image and updates group database to reset data next time
    public static void AddImage(String name, String id){
        personDatabase=personDatabaseOpenHelper.getWritableDatabase();
        personDatabase.execSQL("INSERT INTO "+PersonDatabase.TABLE_NAME+"("+
                PersonDatabase.PERSON_NAME+", "+
                PersonDatabase.IMAGE_ID+") VALUES (?,?)",
                new String[]{name,id});

        groupDatabase=groupDatabaseOpenHelper.getWritableDatabase();
        groupDatabase.execSQL("UPDATE "+GroupDataBase.TABLE_NAME+" SET "+GroupDataBase.BOOLEAN_CHECK+"= 0 WHERE "+
                GroupDataBase.PERSON_NAME+"= '"+name+"';");
    }

    public static void DeleteImage(String name){
        personDatabase=personDatabaseOpenHelper.getWritableDatabase();
        personDatabase.execSQL("DELETE FROM "+PersonDatabase.TABLE_NAME+" WHERE "+PersonDatabase.PERSON_NAME+" = '"+name+"'");
        Log.d(TAG, "DeleteImage: Deleted " +name);
    }

    public static String[] GetAllNames(){
        ArrayList<String> list=new ArrayList<String>();
        personDatabase=personDatabaseOpenHelper.getReadableDatabase();
        Cursor c=personDatabase.rawQuery("SELECT DISTINCT "+PersonDatabase.PERSON_NAME +" FROM "+PersonDatabase.TABLE_NAME,null);
        c.moveToLast();
        int last=c.getPosition();
        c.moveToFirst();
        while(c.getPosition()<=last){
            list.add(c.getString(0));
            c.moveToNext();
        }
        c.close();
        return list.toArray(new String[list.size()]);
    }

    public static String[] GetIDsFromName(String name){
        ArrayList<String> list=new ArrayList<String>();
        personDatabase=personDatabaseOpenHelper.getReadableDatabase();
        Cursor c=personDatabase.rawQuery("SELECT DISTINCT "+PersonDatabase.IMAGE_ID +" FROM "+PersonDatabase.TABLE_NAME+" WHERE "+PersonDatabase.PERSON_NAME+" ='"+name+"'",null);
        c.moveToLast();
        int last=c.getPosition();
        c.moveToFirst();
        while(c.getPosition()<=last){
            list.add(c.getString(0));
            c.moveToNext();
        }
        c.close();
        return list.toArray(new String[list.size()]);
    }
    //We will be using this
    //Bitmap bMap= BitmapFactory.decodeFile(/*id,options*/);


/*
    public static Bitmap GetImage(String id){
        personDatabase=personDatabaseOpenHelper.getReadableDatabase();
        imageDatabase=imageDatabaseOpenHelper.getReadableDatabase();


        Cursor cursor= imageDatabase.rawQuery("SELECT * FROM "+ImageDatabase.TABLE_NAME+" LIMIT 1",null);
        cursor.moveToFirst();
        Log.d(TAG, "GetBitmap: "+cursor.getCount()+" "+cursor.getString(ImageDatabase.indexes[0]));
        cursor.close();


        Bitmap bMap;
        Cursor c=imageDatabase.rawQuery("SELECT * FROM "+ImageDatabase.TABLE_NAME+" WHERE "+ImageDatabase.IMAGE_ID+" ='"+id+"'",null);
        if(c.moveToLast()){
            ByteBuffer bb=ByteBuffer.allocate(c.getInt(ImageDatabase.indexes[2])*c.getInt(ImageDatabase.indexes[3])*4);
            bb.wrap(c.getBlob(ImageDatabase.indexes[1]));
            bMap = Bitmap.createBitmap(c.getInt(ImageDatabase.indexes[2]),c.getInt(ImageDatabase.indexes[3]), Bitmap.Config.ARGB_8888);
            bMap.copyPixelsFromBuffer(bb);
            c.close();
            return bMap;
        }
        c.close();
        Log.d(TAG, "GetBitmap: No bitmap found");
        return null;
    }


*/
}
