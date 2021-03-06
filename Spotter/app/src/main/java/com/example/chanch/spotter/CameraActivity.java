package com.example.chanch.spotter;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION_RESULT=0;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSSION_RESULT=0;
    private static final int STATE_PREVIEW=0;
    private static final int STATE_WAIT_LOCK=1;
    private int mCaptureState=STATE_PREVIEW;

    private static int CAMERA_DIRECTION=1;

    private TextureView mTextureView;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            setUpCamera(width, height);
            //Log.d("Something", "onSurfaceTextureAvailable: "+mCameraDevice);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
            //Toast.makeText(getApplicationContext(),"Camera Connection Made",Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            mCameraDevice = null;
        }
    };

    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;

    private String[] mCameraID=new String[2];

    private Size mPreviewSize;

    private CaptureRequest.Builder mCaptureRequestBuilder;

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private int mTotalRotation;

    private ImageButton mStillImageButton;
    private ImageButton mChangeCameraButton;
    //private OpenCVHelper helper = new OpenCVHelper();

    private Button mCompletionButton;

    private static String TAG="Something";

    private FaceDetector singleFaceDetector;
    private FaceDetector multiFaceDetector;

    private RecognitionHelper recognitionHelper;
    private BitmapFactory.Options options=new BitmapFactory.Options();

    private Button mPrintButton;

    private static NameAndError FINAL_LIST;

    private boolean lock=false;
    private boolean docLock=false;

    private final float PASS_VALUE=2.75f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        checkWriteStoragePermission();
        ImageStorage.DatabaseConnector(getApplicationContext());
        createImageFolder();
        createFileFolder();
        //ImageStorage.Clear();
        recognitionHelper=new RecognitionHelper();

        singleFaceDetector=new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setProminentFaceOnly(true)
                .setMode(FaceDetector.FAST_MODE)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMinFaceSize(0.5f)
                .build();

        mCompletionButton=(Button)findViewById(R.id.CAMERA_completed);
        mCompletionButton.setEnabled(false);
        mCompletionButton.setVisibility(View.GONE);

        mPrintButton=(Button) findViewById(R.id.CAMERA_Print_File);
        mPrintButton.setEnabled(false);
        mPrintButton.setVisibility(View.GONE);

        if(MainActivity.MODE==MainActivity.MODE_REC){
            FINAL_LIST=new NameAndError();

            mCompletionButton.setEnabled(true);
            mCompletionButton.setVisibility(View.VISIBLE);
            mCompletionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String[] out=FINAL_LIST.GetString(PASS_VALUE,2*PASS_VALUE,3*PASS_VALUE);
                    makeToast(out[0]+"\n\n"+out[1]+"\n\n"+out[2]+"\n\n"+out[3]);
                }
            });

            mPrintButton.setEnabled(true);
            mPrintButton.setVisibility(View.VISIBLE);
            mPrintButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!docLock){
                        docLock=true;
                        try {
                            File output = createDocFileName();
                            String[] out=FINAL_LIST.GetString(PASS_VALUE,2*PASS_VALUE,3*PASS_VALUE);
                            String outString= myDocDate+"\nAttendence: \n\n"+out[0]+"\n\n"+out[1]+"\n\n"+out[2]+"\n\n"+out[3];
                            BufferedWriter writer=new BufferedWriter(new FileWriter(output.getAbsolutePath()));
                            writer.write(outString);
                            writer.close();
                            //FileOutputStream fileOutputStream=new FileOutputStream(output);
                            //fileOutputStream.write(Base64.decode(outString,Base64.NO_WRAP));
                            //fileOutputStream.close();

                            makeToast("Finished Creating File");
                        }catch (IOException e){
                        }
                        docLock=false;
                    }else{
                    }
                }
            });

            if(multiFaceDetector!=null) {
                if (!multiFaceDetector.isOperational()) {
                    multiFaceDetector = new FaceDetector.Builder(getApplicationContext())
                            .setMode(FaceDetector.FAST_MODE)
                            .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                            .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                            .setMinFaceSize(0.2f)
                            .build();
                }
            }else{
                multiFaceDetector = new FaceDetector.Builder(getApplicationContext())
                        .setMode(FaceDetector.FAST_MODE)
                        .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                        .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                        .setMinFaceSize(0.2f)
                        .build();
            }
            final Handler handler=new Handler();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    recognitionHelper.LoadGroup();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"Loading Recognization Finished",Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "run: Loading Recognization Finished");
                        }
                    });
                }
            }).start();
            Toast.makeText(getApplicationContext(),"Loading Recognization",Toast.LENGTH_SHORT).show();
        }

        mTextureView = (TextureView) findViewById(R.id.CAMERA_CameraView);

        mStillImageButton=(ImageButton)findViewById(R.id.CAMERA_TakePic);
        mStillImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!lock) {
                    lock=true;
                    lockFocus();
                }else {
                    makeToast("Stop Clicking so Fast");
                }
            }
        });

        mChangeCameraButton=(ImageButton)findViewById(R.id.CAMERA_ChangeCamera);
        mChangeCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(CAMERA_DIRECTION==1){
                    CAMERA_DIRECTION=0;
                }else if(CAMERA_DIRECTION==0){
                    CAMERA_DIRECTION=1;
                }
                closeCamera();
                connectCamera();
            }
        });
        options.inPreferredConfig= Bitmap.Config.ARGB_8888;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if (hasFocus) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        /*
        singleFaceDetector=new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setProminentFaceOnly(true)
                .setMode(FaceDetector.ACCURATE_MODE)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMinFaceSize(0.5f)
                .build();

        if(MainActivity.MODE==MainActivity.MODE_REC){
            multiFaceDetector=new FaceDetector.Builder(getApplicationContext())
                    .setMode(FaceDetector.ACCURATE_MODE)
                    .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                    .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                    .setMinFaceSize(0.2f)
                    .build();
        }
        */
        if (mTextureView.isAvailable()) {
            setUpCamera(mTextureView.getWidth(), mTextureView.getHeight());
            connectCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        /*
        singleFaceDetector.release();
        if(multiFaceDetector!=null) {
            multiFaceDetector.release();
        }
        */
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        ImageStorage.Close(this.getBaseContext());
        singleFaceDetector.release();
        singleFaceDetector=null;
        if(multiFaceDetector!=null) {
            multiFaceDetector.release();
            multiFaceDetector=null;
        }
        if(MainActivity.MODE==MainActivity.MODE_REC) {
            FINAL_LIST.refresh();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==REQUEST_CAMERA_PERMISSION_RESULT){
            if(grantResults[0]!= PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getApplicationContext(),"Application Won't Run Without Camera",Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSSION_RESULT) {
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                //mRecordImageButton.setImageResource(R.mipmap.video_busy);
                try {
                    createImageFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                Toast.makeText(this,"Needs Storage Permisssions to Run",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setUpCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraID : cameraManager.getCameraIdList()) {

                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraID);
                int value;
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    value = 0;
                } else if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    value = 1;
                } else {
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                mTotalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);

                boolean swapRotation = (mTotalRotation == 90 || mTotalRotation == 270);

                int rotatedWidth = width;
                int rotatedHeight = height;

                if (swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);

                mImageSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotatedHeight);
                mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, 1);

                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);


                mCameraID[value] = cameraID;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){
                    cameraManager.openCamera(mCameraID[CAMERA_DIRECTION], mCameraDeviceStateCallBack, mBackgroundHandler);
                }else{
                    if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                        Toast.makeText(this, "Camera Access is Required",Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[]{Manifest.permission.CAMERA},REQUEST_CAMERA_PERMISSION_RESULT);
                }

            }else{
                cameraManager.openCamera(mCameraID[CAMERA_DIRECTION], mCameraDeviceStateCallBack, mBackgroundHandler);
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview(){
        SurfaceTexture surfaceTexture=mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
        Surface previewSurface =new Surface(surfaceTexture);

        try {
            mCaptureRequestBuilder=mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface,mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            mPreviewCaptureSession=session;
                            try {
                                mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),null,mBackgroundHandler);
                                //session.setRepeatingRequest(mCaptureRequestBuilder.build(),null,mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(getApplicationContext(),"Unable To Setup Camera Preview",Toast.LENGTH_SHORT).show();
                        }
                    },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera(){
        if(mCameraDevice!=null){
            mCameraDevice.close();
            mCameraDevice=null;
        }
    }

    private void startBackgroundThread(){
        mBackgroundHandlerThread=new HandlerThread("Camera2 API");
        mBackgroundHandlerThread.start();
        mBackgroundHandler=new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread(){
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread=null;
            mBackgroundHandler=null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation){
        int sensorOrientation =cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation=ORIENTATIONS.get(deviceOrientation);
        Log.d("Rotations", "sensorToDeviceRotation: "+sensorOrientation+" "+deviceOrientation+" "+(sensorOrientation+deviceOrientation+360)%360);
        return (sensorOrientation+deviceOrientation+360)%360;
    }

    private static class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum(((long)lhs.getWidth()*lhs.getHeight())/
                    ((long)rhs.getWidth()*rhs.getHeight()));
        }
    }

    private static Size chooseOptimalSize(Size[] choices,int width,int height){
        List<Size> bigEnough=new ArrayList<Size>();
        for(Size option:choices){
            if(option.getHeight()==option.getWidth()*height/width&&
                    option.getWidth()>=width&&
                    option.getHeight()>=height){
                bigEnough.add(option);
            }
        }
        if(bigEnough.size()>0){
            return Collections.min(bigEnough,new CompareSizeByArea());
        }else {
            return choices[0];
        }
    }


    private static File mImageFolder;
    private String mImageName;
    private ImageReader mImageReader;
    private Size mImageSize;

    private static File mDocFolder;
    private String myDocDate;

    private CameraCaptureSession mPreviewCaptureSession;
    private CameraCaptureSession.CaptureCallback mPreviewCaptureCallback=new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            process(result);
        }

        private void process(CaptureResult captureResult){
            switch(mCaptureState){
                case STATE_PREVIEW:
                    //Do Nothing
                    break;
                case STATE_WAIT_LOCK:
                    mCaptureState=STATE_PREVIEW;
                    /*
                    Integer afState=captureResult.get(CaptureResult.CONTROL_AF_STATE);
                    if(afState==CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED||
                            afState==CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED){
                        Toast.makeText(getApplicationContext(),"Autofocus Locked",Toast.LENGTH_SHORT).show();
                        startStillCaptureRequest();
                    }
                    */
                    startStillCaptureRequest();
                    break;
            }
        }
    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener=new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            mBackgroundHandler.post(new ImageSaver((imageReader.acquireLatestImage())));
            //ImageStorage.storeImage(imageReader.acquireLatestImage());
            //startActivity(new Intent(Camera2ApiMethod.this,ImageViewer.class));
        }
    };

    private static int runs=0;

    private final static int BREAKS=2;

    //All of the image processing goes here
    private class ImageSaver implements Runnable{
        private final Image mImage;

        public ImageSaver(Image image){
            mImage=image;
        }

        public void run(){
            File tempFile=null;
            try {
                tempFile=createImageFileName();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);

            FileOutputStream fileOutPutStream = null;
            try {
                fileOutPutStream = new FileOutputStream(mImageName);
                fileOutPutStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (fileOutPutStream != null) {
                    try {
                        fileOutPutStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            lock=false;
            //Toast.makeText(getApplicationContext(),"Beginning Face Detection",Toast.LENGTH_SHORT).show();

            switch (MainActivity.MODE) {
                case MainActivity.MODE_IMAGE:
                    Frame frame=new Frame.Builder().setBitmap(BitmapFactory.decodeFile(tempFile.getAbsolutePath(),options)).build();
                    SparseArray<Face> faces= singleFaceDetector.detect(frame);
                    Log.d(TAG, "run: "+faces.size());
                    if(faces.size()==1){
                        if(faces.valueAt(0)==null){
                            tempFile.delete();
                            makeToast("Face is Null. Face Detector may be malfunctioning. Please restart Applicaion");
                            runs++;
                            break;
                        }
                        if(Math.abs(faces.valueAt(0).getEulerY())<5.0f && Math.abs(faces.valueAt(0).getEulerZ())<5.0f &&
                                recognitionHelper.checkKeyComponents(faces.valueAt(0))) {

                            makeToast("Found A Face. Image Saved");
                            ImageStorage.AddImage(GroupAndNameListActivity.person,mImageName);
                        }
                        else{
                            makeToast("Found A Face. Incorrect Rotations. Image Not Saved");
                            tempFile.delete();
                        }
                    }else{
                        makeToast("Cannot Find Any Faces");
                        tempFile.delete();
                    }
                    break;
                case MainActivity.MODE_REC:
                    if(!recognitionHelper.RecLoaded){
                        makeToast("Recognition Not Loaded Yet");
                        tempFile.delete();
                        return;
                    }
                    Frame frame2=new Frame.Builder().setBitmap(BitmapFactory.decodeFile(tempFile.getAbsolutePath(),options)).build();
                    SparseArray<Face> faces2= multiFaceDetector.detect(frame2);
                    for (int i=0;i<faces2.size();i++){
                        if(faces2.valueAt(i)==null){
                            runs++;
                            makeToast("Face is Null. Face Detector may be malfunctioning. Please restart Applicaion");
                            Log.d(TAG, "run: Face is null");
                            continue;
                        }
                        boolean check=recognitionHelper.checkKeyComponents(faces2.valueAt(i));
                        if(Math.abs(faces2.valueAt(i).getEulerY())<5.0f && Math.abs(faces2.valueAt(i).getEulerZ())<5.0f &&
                                check) {
                            float[] tempFloatArray=recognitionHelper.getLandmarkData(faces2.valueAt(i));
                            float error=100.0f;
                            String name="";

                            for(int j=0;j<recognitionHelper.distancesList.size();j++){
                                float[] distances=recognitionHelper.distancesList.get(j);
                                float tempError=0.0f;
                                float tempAverageDeviation=0.0f;

                                for(int k=0;k<BREAKS;k++) {
                                    for (int l = tempFloatArray.length/BREAKS*k; l < tempFloatArray.length/BREAKS*(k+1); l++) {
                                        switch (k) {
                                            case 0:
                                                float errorPercent1 = Math.abs((tempFloatArray[l] - distances[l]) / distances[l + tempFloatArray.length]);
                                                tempAverageDeviation += distances[l + tempFloatArray.length];
                                                tempError += (errorPercent1 * errorPercent1);
                                                break;
                                            case 1:
                                                float errorPercent2 = Math.abs(tempFloatArray[l] - distances[l]);
                                                while(errorPercent2>0.5f){
                                                    errorPercent2-=0.5f;
                                                }
                                                errorPercent2/= distances[l + tempFloatArray.length];
                                                tempError += (errorPercent2 * errorPercent2);
                                                break;
                                        }
                                    }
                                }
                                tempError=((float) Math.sqrt(tempError/tempFloatArray.length))*tempAverageDeviation*10.0f;
                                if(tempError<error){
                                    error=tempError;
                                    name=recognitionHelper.namesList.get(j);
                                }
                            }
                            makeToast("Most likely: "+name+" with Error: "+error);
                            FINAL_LIST.add(name,error);
                        }
                        else if(!check){
                            //makeToast("Can't detect landmarks properly");
                            //Log.d(TAG, "run: missing landmarks");
                        }
                        else{
                            makeToast("Can't detect face properly");
                            Log.d(TAG, "run: wrong orientation");
                        }
                    }
                    tempFile.delete();
                    break;
            }
            Log.d(TAG, "run: running count:"+runs );
            if(runs>3) {
                runs=0;
                Log.d(TAG, "run: running the restart thingy");
                //finish();
                //finishAffinity();
                //System.exit(0);
                /*
                multiFaceDetector.release();
                multiFaceDetector=null;
                System.gc();
                multiFaceDetector=new FaceDetector.Builder(getApplicationContext())
                        .setMode(FaceDetector.FAST_MODE)
                        .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                        .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                        .setMinFaceSize(0.2f)
                        .build();
                */
                /*
                Intent i = getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage(getBaseContext().getPackageName());
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                */
            }
        }
    }

    private void createImageFolder(){
        File imageFile= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        mImageFolder=new File(imageFile,"Faces");
        if(!mImageFolder.exists()){
            mImageFolder.mkdirs();
        }
    }

    private void createFileFolder(){
        mDocFolder=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        /*if(!mDocFolder.exists()){
            mDocFolder.mkdirs();
        }*/
    }

    private File createImageFileName()throws IOException{
        String time= "IMAGE"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File imageFile=File.createTempFile(time,".jpg",mImageFolder);
        mImageName=imageFile.getAbsolutePath();
        //imageFile.delete(); deletes file
        return imageFile;
    }

    private File createDocFileName() throws IOException{
        String time=new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        myDocDate=time.substring(0,7);
        File docFile=File.createTempFile(GroupAndNameListActivity.group+" "+time,".txt",mDocFolder);
        return docFile;
    }

    private void startStillCaptureRequest(){
        try {
            mCaptureRequestBuilder =mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,mTotalRotation);

            CameraCaptureSession.CaptureCallback stillCaptureCallback=new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                }
            };
            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(),stillCaptureCallback,null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void lockFocus(){
        mCaptureState=STATE_WAIT_LOCK;
        //Toast.makeText(getApplicationContext(),"Taking Picture",Toast.LENGTH_SHORT).show();
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(),mPreviewCaptureCallback,mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    //Storage Permisssions
    private void checkWriteStoragePermission(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
            }
            else{
                if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    Toast.makeText(this,"App needs Permission to save images",Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSSION_RESULT);
            }
        }
        else{
        }
    }

    private Toast previousToast;
    public void makeToast(String out){
        if(previousToast!=null) {
            previousToast.cancel();
        }

        previousToast=Toast.makeText(getApplicationContext(),out,Toast.LENGTH_SHORT);
        previousToast.show();
    }

    private class NameAndError{
        private ArrayList<String> names;
        private ArrayList<Float> errors;
        public NameAndError(){
            refresh();
        }
        public void refresh(){
            names=new ArrayList<String>();
            errors=new ArrayList<Float>();
        }

        public void add(String name,float error){
            int i=-1;
            for(int x=0;x<names.size();x++){
                if(names.get(x).equals(name)){
                    i=x;
                }
            }
            if(i!=-1){
                if(errors.get(i)>error) {
                    errors.set(i, error);
                }
            }else {
                names.add(name);
                errors.add(error);
            }
        }

        public String[] GetString(float high,float med,float low){
            String outHigh="Highly Likely:\n";String outMed="Somewhat Likely:\n";String outLow="Unlikely:\n";String outNon="Not Here:\n";
            for(int x=0;x<names.size();x++){
                if(errors.get(x)<=high){
                    outHigh+=names.get(x)+"\n";
                }else if(errors.get(x)<=med){
                    outMed+=names.get(x)+"\n";
                }else if(errors.get(x)<=low){
                    outLow+=names.get(x)+"\n";
                }else{
                    outNon+=names.get(x)+"\n";
                }
            }
            return new String[]{outHigh,outMed,outLow,outNon};
        }
    }


    private class RecognitionHelper{
        public boolean RecLoaded =false;
        ArrayList<float[]> distancesList=new ArrayList<float[]>();
        ArrayList<String> namesList=new ArrayList<String>();

        public boolean checkKeyComponents(Face face) {
            List<Landmark> marks = face.getLandmarks();
            Log.d(TAG, "checkKeyComponents: Running");
            int countWrong = 0;
            for (Landmark l : marks) {
                if (l.getType() == Landmark.LEFT_EAR || l.getType() == Landmark.LEFT_EAR_TIP || l.getType() == Landmark.RIGHT_EAR || l.getType() == Landmark.RIGHT_EAR_TIP) {
                    countWrong++;
                }
            }
            return (marks.size() - countWrong) >= 8;
        }

        public void LoadGroup(){
            ImageStorage.LoadNamesFromGroup(GroupAndNameListActivity.group);
            String[] names = ImageStorage.tempNameList;
            Boolean[] check=ImageStorage.tempBooleanList;
            Log.d(TAG, "LoadGroup: "+names.length);
            for(int a=0;a<names.length;a++){
                FINAL_LIST.add(names[a],100.0f);

                if(check[a]){
                    distancesList.add(ImageStorage.getBlobData(names[a]));
                    namesList.add(names[a]);
                    Log.d(TAG, "LoadGroup: "+names[a]);
                }
                else {
                    ArrayList<float[]> tempList=new ArrayList<float[]>();
                    String[] ids = ImageStorage.GetIDsFromName(names[a]);
                    for (String id : ids) {
                        try {
                            Frame frame = new Frame.Builder().setBitmap(BitmapFactory.decodeFile(id)).build();
                            Face f = singleFaceDetector.detect(frame).valueAt(0);
                            float[] tempFloatArray=getLandmarkData(f);
                            tempList.add(tempFloatArray);
                        } catch (Error e) {
                        }
                    }
                    //get all averages
                    int arrayLength=tempList.get(0).length;
                    float[] output=new float[arrayLength*2];

                    for(int i=0;i<arrayLength*2;i++){
                        output[i]=0.0f;
                    }

                    for(float[] tempArray:tempList ){
                        for(int i=0;i<arrayLength;i++){
                            output[i]+=tempArray[i];
                        }
                    }

                    for (int i = 0; i < arrayLength; i++) {
                        output[i] /= tempList.size();
                    }
                    //use temp list to get standard deviation
                    for (int x = 0; x < arrayLength; x++) {
                        float sum=0;
                        for(int y=0;y<tempList.size();y++){
                            float tempSquareRoot= tempList.get(y)[x]-output[x];
                            sum+=(tempSquareRoot*tempSquareRoot);
                        }
                        output[arrayLength+x]=(float)Math.sqrt(sum/tempList.size());
                    }
                    ImageStorage.storeBlobData(names[a],output);
                    distancesList.add(output);
                    namesList.add(names[a]);
                }
            }
            RecLoaded=true;
        }

        public float[] getLandmarkData(Face f){
            Landmark[] marks =new Landmark[8];
            for (Landmark landmark : f.getLandmarks()) {
                int i = -1;
                switch (landmark.getType()) {
                    case Landmark.LEFT_EYE:
                        i = 0;
                        break;
                    case Landmark.RIGHT_EYE:
                        i = 1;
                        break;
                    case Landmark.LEFT_CHEEK:
                        i = 2;
                        break;
                    case Landmark.RIGHT_CHEEK:
                        i = 3;
                        break;
                    case Landmark.LEFT_MOUTH:
                        i = 4;
                        break;
                    case Landmark.RIGHT_MOUTH:
                        i = 5;
                        break;
                    case Landmark.NOSE_BASE:
                        i = 6;
                        break;
                    case Landmark.BOTTOM_MOUTH:
                        i = 7;
                        break;
                }
                if (i == -1) {
                } else {
                    marks[i] = landmark;
                }
            }
            return getAllFaceValues(marks);
        }

        private float[] getAllFaceValues(Landmark[] sortedLandmarks){
            float lengthNormalizer=getDistance(sortedLandmarks[2],sortedLandmarks[3]);
            float angleNormalizer=getAngle(sortedLandmarks[0],sortedLandmarks[1],0.0f);

            return new float[]{
                    getDistance(sortedLandmarks[0],sortedLandmarks[1])/lengthNormalizer, getDistance(sortedLandmarks[4],sortedLandmarks[5])/lengthNormalizer,
                    getDistance(sortedLandmarks[0],sortedLandmarks[4])/lengthNormalizer, getDistance(sortedLandmarks[1],sortedLandmarks[5])/lengthNormalizer,
                    getDistance(sortedLandmarks[4],sortedLandmarks[7])/lengthNormalizer, getDistance(sortedLandmarks[4],sortedLandmarks[6])/lengthNormalizer,
                    getDistance(sortedLandmarks[5],sortedLandmarks[7])/lengthNormalizer, getDistance(sortedLandmarks[5],sortedLandmarks[6])/lengthNormalizer,
                    getDistance(sortedLandmarks[2],sortedLandmarks[6])/lengthNormalizer, getDistance(sortedLandmarks[3],sortedLandmarks[6])/lengthNormalizer,

                    getDistance(sortedLandmarks[0],sortedLandmarks[2])/lengthNormalizer, getDistance(sortedLandmarks[1],sortedLandmarks[3])/lengthNormalizer,
                    getDistance(sortedLandmarks[2],sortedLandmarks[4])/lengthNormalizer, getDistance(sortedLandmarks[3],sortedLandmarks[5])/lengthNormalizer,

                    getAngle(sortedLandmarks[0],sortedLandmarks[1],angleNormalizer), getAngle(sortedLandmarks[4],sortedLandmarks[5],angleNormalizer),
                    getAngle(sortedLandmarks[0],sortedLandmarks[4],angleNormalizer), getAngle(sortedLandmarks[1],sortedLandmarks[5],angleNormalizer),
                    getAngle(sortedLandmarks[4],sortedLandmarks[7],angleNormalizer), getAngle(sortedLandmarks[4],sortedLandmarks[6],angleNormalizer),
                    getAngle(sortedLandmarks[5],sortedLandmarks[7],angleNormalizer), getAngle(sortedLandmarks[5],sortedLandmarks[6],angleNormalizer),
                    getAngle(sortedLandmarks[2],sortedLandmarks[6],angleNormalizer), getAngle(sortedLandmarks[3],sortedLandmarks[6],angleNormalizer),

                    getAngle(sortedLandmarks[0],sortedLandmarks[2],angleNormalizer), getAngle(sortedLandmarks[1],sortedLandmarks[3],angleNormalizer),
                    getAngle(sortedLandmarks[2],sortedLandmarks[4],angleNormalizer), getAngle(sortedLandmarks[3],sortedLandmarks[5],angleNormalizer)
            };
        }

        private float getAngle(Landmark l1,Landmark l2,float normalizer){
            float xLength=l1.getPosition().x-l2.getPosition().x;
            //float yLength=l1.getPosition().y-l2.getPosition().y;
            try {
                //return (((float) Math.acos(yLength / xLength))-normalizer+(float)Math.PI)/((float)Math.PI*2.0f);
                float out=(float)((Math.acos(xLength/getDistance(l1,l2))/Math.PI)-normalizer);
                while(out>1.0f){
                    out-=1.0f;
                }
                while(out<0.0f){
                    out+=1.0f;
                }
                //value will be between 0,1
                return out;
            }catch (Error e){
                return 0.5f-(normalizer/((float)Math.PI));
            }
        }

        private float getDistance(Landmark l1,Landmark l2){
            float xLength=l1.getPosition().x-l2.getPosition().x;
            float yLength=l1.getPosition().y-l2.getPosition().y;
            return (float) Math.sqrt(xLength*xLength+yLength*yLength);
        }

    }
}

/*
                    try {
                        createImageFileName();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Bitmap bMap = mTextureView.getBitmap();

                    Log.d(TAG, "run1: "+bMap.getByteCount());
                    helper.alterImage(bMap);

                    Log.d(TAG, "run2: "+bMap.getByteCount());

                    ByteBuffer byteBuffer2 = ByteBuffer.allocate(bMap.getByteCount());
                    bMap.copyPixelsToBuffer(byteBuffer2);
                    byteBuffer2.rewind();
                    byte[] bytes2 = new byte[byteBuffer2.remaining()];
                    byteBuffer2.get(bytes2);

                    Log.d(TAG, "run4: "+bytes2.length);
                    FileOutputStream fileOutPutStream2 = null;
                    try {
                        fileOutPutStream2 = new FileOutputStream(mImageName);
                        fileOutPutStream2.write(bytes2);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (fileOutPutStream2 != null) {
                            try {
                                fileOutPutStream2.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    bMap.recycle();
                    mImage.close();
*/