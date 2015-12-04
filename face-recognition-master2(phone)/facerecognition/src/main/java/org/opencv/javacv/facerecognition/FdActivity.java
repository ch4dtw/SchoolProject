package org.opencv.javacv.facerecognition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.javacv.facerecognition.R;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;

import com.google.android.glass.app.Card;
import com.googlecode.javacv.cpp.opencv_imgproc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class FdActivity extends Activity implements CvCameraViewListener2 {
    private static final String    TAG                 = "OCVSample::Activity";
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;

    public static final int TRAINING= 0;
    public static final int SEARCHING= 1;
    public static final int IDLE= 2;

    private static final int frontCam =1;
    private static final int backCam =2;

    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

    private int faceState=IDLE;

    private MenuItem               nBackCam;
    private MenuItem               mFrontCam;

    private Mat                    mRgba;
    private Mat                    mGray;
    private File                   mCascadeFile;
    private CascadeClassifier      mJavaDetector;
    //   private DetectionBasedTracker  mNativeDetector;

    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;

    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;
    private int mLikely=999;

    String mPath="";
    String spokenText;
    private Tutorial3View   mOpenCvCameraView;
    private int mChooseCamera = backCam; //將鏡頭初始化為後置鏡頭

    TextView textresult,ttest,name;
    private  ImageView Iv;
    Bitmap mBitmap;
    Handler mHandler;

    PersonRecognizer fr;
    ToggleButton toggleButtonGrabar;

    ImageView ivGreen,ivYellow,ivRed;
    ImageButton imCamera;

    TextView textState;

    static final long MAXIMG = 10;
    int countImages=0;
    int statecount=0;
    labels labelsFile;

    static PersonRecognizer Person_data = new PersonRecognizer();
    private GestureDetector mGestureDetector;
    private Button mButton,msearch;

    private final static int CODE_SPEECH = 0;
    private final static String PROMPT_TEXT = "Voice Input ";



    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    fr=new PersonRecognizer(mPath);
                    String s = getResources().getString(R.string.Straininig);
                    Toast.makeText(getApplicationContext(),s, Toast.LENGTH_LONG).show();
                    fr.load();

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();

                } break;

                default:
                {
                    super.onManagerConnected(status);
                } break;


            }
        }
    };

    public FdActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }
    public static String search_name(String str){
        int count=0;
        while(str.charAt(count)!='@') count++;
        return str.substring(0,count);
    }

    public static String catch_name(String str){
        int count=0;
        while(str.charAt(count)!='@') count++;
        str = str.substring(count+1);
        return str;
    }


    /** Called when the activity is first created. */
    @Override
//-----------------------------------------------------------------------------------------------------------------------------------------------------------------//
    public void onCreate(Bundle savedInstanceState) {

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        try{
            InputStreamReader fr = new InputStreamReader(new FileInputStream(path + "/Person_data/" + "data.txt"), "BIG5");
            BufferedReader br = new BufferedReader(fr);
            String temp = br.readLine();
            while (temp!=null){
                Person_data.setFr(search_name(temp),catch_name(temp));
                temp=br.readLine();
            }
            fr.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }

        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_surface_view);

        mOpenCvCameraView = (Tutorial3View) findViewById(R.id.tutorial3_activity_java_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mGestureDetector = new GestureDetector(this, new MyOnGestureListener());//Glass gesture detector

        mButton = (Button) findViewById(R.id.buttonCat);
        mButton.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i(getClass().getName(), "onTouch-----" + getActionName(event.getAction()));
                mGestureDetector.onTouchEvent(event);
                // 一定要返回true，不然获取不到完整的事件
                return true;
            }
        });
        msearch = (Button)findViewById(R.id.btsearch);

        //mPath = getFilesDir() + "/facerecogOCV/";//取得 App 內部儲存體存放檔案的目錄 (絕對路徑)
        //預設路徑為 /data/data/[package.name]/files/

        File mmpath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        mPath = mmpath + "/Person_data/";
        labelsFile = new labels(mPath);

        Iv = (ImageView) findViewById(R.id.imageView1);
        textresult = (TextView) findViewById(R.id.textView1);

        mHandler = new Handler() {//Handler專門用來告知你的程式說：你的執行緒跑完了，該更新一下畫面了喔！所以利用Handler把這個message send出去處理
            @Override
            public void handleMessage(Message msg) {
                if (msg.obj == "IMG") {
                    Canvas canvas = new Canvas();
                    canvas.setBitmap(mBitmap);
                    Iv.setImageBitmap(mBitmap);

                    if (countImages >= MAXIMG - 1) {
                        toggleButtonGrabar.setChecked(false);
                        grabarOnclick();
                    }
                } else {
                    textresult.setText(msg.obj.toString());
                    ivGreen.setVisibility(View.INVISIBLE);
                    ivYellow.setVisibility(View.INVISIBLE);
                    ivRed.setVisibility(View.INVISIBLE);

                    String s = Integer.toString(mLikely);
                    ttest.setText(s);

                    if (mLikely < 0) ;
                    else if (mLikely < 50) ivGreen.setVisibility(View.VISIBLE);
                    else if (mLikely < 80) ivYellow.setVisibility(View.VISIBLE);
                    else ivRed.setVisibility(View.VISIBLE);
                }
            }
        };

        name = (TextView) findViewById(R.id.nametext);//語音輸入姓名
        ttest = (TextView)findViewById(R.id.ttest);//%用於search

        toggleButtonGrabar = (ToggleButton) findViewById(R.id.toggleButtonGrabar);//不能刪除，原因暫時不知

        textState = (TextView) findViewById(R.id.textViewState);

        ivGreen = (ImageView) findViewById(R.id.imageView3);
        ivYellow = (ImageView) findViewById(R.id.imageView4);
        ivRed = (ImageView) findViewById(R.id.imageView2);
        imCamera = (ImageButton) findViewById(R.id.imageButton1);

        //初始化狀態
        ivGreen.setVisibility(View.INVISIBLE);
        ivYellow.setVisibility(View.INVISIBLE);
        ivRed.setVisibility(View.INVISIBLE);

        textresult.setVisibility(View.INVISIBLE);
        toggleButtonGrabar.setVisibility(View.INVISIBLE);



        toggleButtonGrabar.setOnClickListener(new View.OnClickListener() {//rec的按鈕
            public void onClick(View v) {
                grabarOnclick();
            }
        });

        imCamera.setOnClickListener(new View.OnClickListener() {//控制前後鏡頭
            public void onClick(View v) {
                if (mChooseCamera==frontCam)
                {
                    mChooseCamera=backCam;
                    mOpenCvCameraView.setCamBack();
                }
                else
                {
                    mChooseCamera=frontCam;
                    mOpenCvCameraView.setCamFront();
                }
            }
        });

        msearch.setOnClickListener(new View.OnClickListener() {//控制前後鏡頭
            public void onClick(View v) {
                if (statecount == 0) {
                    Toast.makeText(getApplicationContext(), "3", Toast.LENGTH_LONG).show();
                    mButton.setText("Back");
                    search();
                    statecount = 2;
                } else if (statecount == 2) {
                    Toast.makeText(getApplicationContext(), "4", Toast.LENGTH_LONG).show();
                    mButton.setText("Select");
                    faceState = IDLE;
                    textState.setText(getResources().getString(R.string.SIdle));
                    toggleButtonGrabar.setVisibility(View.INVISIBLE);
                    textresult.setVisibility(View.INVISIBLE);

                    ivGreen.setVisibility(View.INVISIBLE);
                    ivYellow.setVisibility(View.INVISIBLE);
                    ivRed.setVisibility(View.INVISIBLE);
                    ttest.setText("");

                    statecount = 0;
                }

            }
        });



        boolean success=(new File(mPath)).mkdirs();
        if (!success)
        {
            Log.e("Error","Error creating directory");
        }
    }
    //-------------------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------------------
    void grabarOnclick()
    {
        if (toggleButtonGrabar.isChecked())
            faceState=TRAINING;
        else
        {
            countImages=0;
            faceState=IDLE;
        }
    }
    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------//
    public void search(){
        if (!fr.canPredict())
        {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.SCanntoPredic), Toast.LENGTH_LONG).show();
            return;
        }
        textState.setText(getResources().getString(R.string.SSearching));
        faceState=SEARCHING;
        textresult.setVisibility(View.VISIBLE);
    }
    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------//
    public void getVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, PROMPT_TEXT);
        startActivityForResult(intent, CODE_SPEECH);
    }
    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------//
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_SPEECH && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            spokenText = results.get(0);
            name.setText(spokenText);
        }
    }
    //----------------------------------------------------------------------------------------------
    //view all face
    public void viewallface() {
        Intent i = new Intent(org.opencv.javacv.facerecognition.FdActivity.this,
                org.opencv.javacv.facerecognition.ImageGallery.class);
        i.putExtra("path", mPath);
        startActivity(i);
    }

    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------//
    class MyOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.i(getClass().getName(), "onSingleTapUp-----" + getActionName(e.getAction()));
            if(statecount==0){
                Toast.makeText(getApplicationContext(),"0", Toast.LENGTH_LONG).show();
                viewallface();
            }
            else if (statecount == 1) {
                Toast.makeText(getApplicationContext(), "rec", Toast.LENGTH_LONG).show();
                faceState = TRAINING;
            }
            return false;
        }
        @Override
        public void onLongPress(MotionEvent e) {
            Log.i(getClass().getName(), "onLongPress-----" + getActionName(e.getAction()));
            if(statecount==0) {//---train
                Toast.makeText(getApplicationContext(),"1", Toast.LENGTH_LONG).show();
                textState.setText(getResources().getString(R.string.SEnter));
                mButton.setText("Rec");
                textresult.setVisibility(View.VISIBLE);
                textresult.setText(getResources().getString(R.string.SFaceName));

                ivGreen.setVisibility(View.INVISIBLE);
                ivYellow.setVisibility(View.INVISIBLE);
                ivRed.setVisibility(View.INVISIBLE);
                statecount=1;
                getVoiceInput();
            }
            else if(statecount==1){
                Toast.makeText(getApplicationContext(),"2", Toast.LENGTH_LONG).show();
                textState.setText(R.string.Straininig);
                mButton.setText("Select");
                name.setText("");

                textresult.setText("");
                {
                    toggleButtonGrabar.setVisibility(View.INVISIBLE);
                }
                Toast.makeText(getApplicationContext(),getResources().getString(R.string.Straininig), Toast.LENGTH_LONG).show();
                fr.train();
                textState.setText(getResources().getString(R.string.SIdle));
                statecount=0;
            }

        }
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.i(getClass().getName(), "onScroll-----" + getActionName(e2.getAction()) + ",(" + e1.getX() + "," + e1.getY() + ") ,("
                    + e2.getX() + "," + e2.getY() + ")");

            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.i(getClass().getName(), "onFling-----" + getActionName(e2.getAction()) + ",(" + e1.getX() + "," + e1.getY() + ") ,("
                    + e2.getX() + "," + e2.getY() + ")");
            return false;
        }
        @Override
        public void onShowPress(MotionEvent e) {
            Log.i(getClass().getName(), "onShowPress-----" + getActionName(e.getAction()));         }

        @Override
        public boolean onDown(MotionEvent e) {
            Log.i(getClass().getName(), "onDown-----" + getActionName(e.getAction()));
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.i(getClass().getName(), "onDoubleTap-----" + getActionName(e.getAction()));
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            Log.i(getClass().getName(), "onDoubleTapEvent-----" + getActionName(e.getAction()));
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.i(getClass().getName(), "onSingleTapConfirmed-----" + getActionName(e.getAction()));
            return false;
        }
    }

    private String getActionName(int action) {
        String name = "";
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                name = "ACTION_DOWN";
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                name = "ACTION_MOVE";
                break;
            }
            case MotionEvent.ACTION_UP: {
                name = "ACTION_UP";
                break;
            }
            default:
                break;
        }
        return name;
    }


    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------//
    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {//在這獲取圖像資料

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        MatOfRect faces = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
        else if (mDetectorType == NATIVE_DETECTOR) {}
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] facesArray = faces.toArray();

        if ((facesArray.length==1)&&(faceState==TRAINING)&&(countImages<MAXIMG)&&(!spokenText.isEmpty())) {
            Mat m=new Mat();
            Rect r=facesArray[0];
            m=mRgba.submat(r);
            mBitmap = Bitmap.createBitmap(m.width(),m.height(), Bitmap.Config.ARGB_8888);

            Utils.matToBitmap(m, mBitmap);

            Message msg = new Message();
            String textTochange = "IMG";
            msg.obj = textTochange;
            mHandler.sendMessage(msg);
            if (countImages<MAXIMG)
            {
                fr.add(m, spokenText);
                countImages++;
            }
        }
        else {
            if ((facesArray.length > 0) && (faceState == SEARCHING)) {
                Mat m = new Mat();
                m = mGray.submat(facesArray[0]);
                mBitmap = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);

                Utils.matToBitmap(m, mBitmap);
                Message msg = new Message();
                String textTochange = "IMG";
                msg.obj = textTochange;
                mHandler.sendMessage(msg);

                textTochange = fr.predict(m);
                mLikely = fr.getProb();
                msg = new Message();
                msg.obj = textTochange;
                mHandler.sendMessage(msg);
            }
        }
        for (int i = 0; i < facesArray.length; i++)
            Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);

        return mRgba;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        if (mOpenCvCameraView.numberCameras()>1) {
            nBackCam = menu.add(getResources().getString(R.string.SFrontCamera));
            mFrontCam = menu.add(getResources().getString(R.string.SBackCamera));
        }
        else {
            imCamera.setVisibility(View.INVISIBLE);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        nBackCam.setChecked(false);
        mFrontCam.setChecked(false);

        if (item == nBackCam)
        {
            mOpenCvCameraView.setCamFront();
            mChooseCamera=frontCam;
        }

        else if (item==mFrontCam)
        {
            mChooseCamera=backCam;
            mOpenCvCameraView.setCamBack();

        }

        item.setChecked(true);

        return true;
    }

    private void setMinFaceSize(float faceSize)
    {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

    private void setDetectorType(int type) {}
}
