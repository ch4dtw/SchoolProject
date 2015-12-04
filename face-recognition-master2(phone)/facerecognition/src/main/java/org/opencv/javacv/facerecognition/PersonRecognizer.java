package org.opencv.javacv.facerecognition;

import static  com.googlecode.javacv.cpp.opencv_highgui.*;
import static  com.googlecode.javacv.cpp.opencv_core.*;

import static  com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_contrib.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import com.googlecode.javacv.cpp.opencv_imgproc;
import com.googlecode.javacv.cpp.opencv_contrib.FaceRecognizer;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_core.MatVector;

import android.graphics.Bitmap;
import android.util.Log;

public  class PersonRecognizer {

	public final static int MAXIMG = 100;
	FaceRecognizer faceRecognizer;
	String mPath;
	int count=0;
	labels labelsFile;

    public static String Lable_Data[] = new String[100];
    public static String Name_Data[] = new String[100];
    int index=0,i;

	static  final int WIDTH= 128;
	static  final int HEIGHT= 128;;
	private int mProb=999;

    PersonRecognizer() {}

	PersonRecognizer(String path)
	{
		faceRecognizer =  com.googlecode.javacv.cpp.opencv_contrib.createLBPHFaceRecognizer(2,8,8,8,200);
		mPath=path;
		labelsFile= new labels(mPath);
	}

    public void setFr(String data1,String data2){
        if(index<100) {
            Lable_Data[index] = data1;
            Name_Data[index++] = data2;
        }
    }

    public String search_data(String data){
        for(i=0;i<100;i++){
            if(Lable_Data[i]==null) break;
            if(Lable_Data[i].equals(data)) {
                return Name_Data[i];
            }
        }
        return "尚未建檔";
    }


	void add(Mat m, String description) {
		Bitmap bmp= Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);

		Utils.matToBitmap(m,bmp);
		bmp= Bitmap.createScaledBitmap(bmp, WIDTH, HEIGHT, false);

		FileOutputStream f;
		try {
			f = new FileOutputStream(mPath+description+"-"+count+".jpg",true);
			count++;
			bmp.compress(Bitmap.CompressFormat.JPEG, 100, f);
			f.close();

		} catch (Exception e) {
			Log.e("error",e.getCause()+" "+e.getMessage());
			e.printStackTrace();

		}
	}

	public boolean train() {

		File root = new File(mPath);

		FilenameFilter pngFilter = new FilenameFilter() {//過濾器
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".jpg");
			};
		};

		File[] imageFiles = root.listFiles(pngFilter); //回傳File陣列，也就是說目錄裡面的有哪些檔案 通通自動變成File類別存在imageFiles陣列中

		MatVector images = new MatVector(imageFiles.length);//陣列長度

		int[] labels = new int[imageFiles.length];//創建labels陣列 長度跟imageFiles陣列長度一樣

		int counter = 0;
		int label;

		IplImage img=null; //IplImage圖形資料空間
		IplImage grayImg;

		int i1=mPath.length();//字串長度


		for (File image : imageFiles) {//使用 Collection 或陣列 (array) ，
			// 可以在小括弧中直接宣告一個與 Collection 或陣列相同資料型態的變數，
			// 然後可以在迴圈中的每一輪藉由這個變數依序取得元素值
			String p = image.getAbsolutePath();

			img = cvLoadImage(p);

			if (img==null)
				Log.e("Error","Error cVLoadImage");
			Log.i("image",p);

			int i2=p.lastIndexOf("-");//回傳指定字串最後出現的位置
			int i3=p.lastIndexOf(".");//EX: "123456786"  要找"6"  則回傳8
			int icount=Integer.parseInt(p.substring(i2+1,i3));//擷取圖片編號
			if (count<icount) count++;

			String description=p.substring(i1,i2);//名子

			if (labelsFile.get(description)<0)
				labelsFile.add(description, labelsFile.max()+1);

			label = labelsFile.get(description);

			grayImg = IplImage.create(img.width(), img.height(), IPL_DEPTH_8U, 1);

			cvCvtColor(img, grayImg, CV_BGR2GRAY);//將img轉成灰階 儲存至grayimg

			images.put(counter, grayImg);

			labels[counter] = label;

			counter++;
		}
		if (counter>0)
			if (labelsFile.max()>1)
				faceRecognizer.train(images, labels);
		labelsFile.Save();
		return true;
	}

	public boolean canPredict()
	{
		if (labelsFile.max()>1)
			return true;
		else
			return false;

	}

    public String predict(Mat m) {
        if (!canPredict())
            return "";
        int n[] = new int[1];
        double p[] = new double[1];
        IplImage ipl = MatToIplImage(m,WIDTH, HEIGHT);
//		IplImage ipl = MatToIplImage(m,-1, -1);

        faceRecognizer.predict(ipl, n, p);

        if (n[0]!=-1)
            mProb=(int)p[0];
        else
            mProb=-1;
        //	if ((n[0] != -1)&&(p[0]<95))
        if (n[0] != -1)
        {
            String label_name = labelsFile.get(n[0]);
            return search_data(label_name);
        }
        else
            return "Unkown";
    }

    IplImage MatToIplImage(Mat m,int width,int heigth)
	{


		Bitmap bmp=Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);


		Utils.matToBitmap(m, bmp);
		return BitmapToIplImage(bmp,width, heigth);

	}

	IplImage BitmapToIplImage(Bitmap bmp, int width, int height) {

		if ((width != -1) || (height != -1)) {
			Bitmap bmp2 = Bitmap.createScaledBitmap(bmp, width, height, false);
			bmp = bmp2;
		}

		IplImage image = IplImage.create(bmp.getWidth(), bmp.getHeight(),
				IPL_DEPTH_8U, 4);

		bmp.copyPixelsToBuffer(image.getByteBuffer());

		IplImage grayImg = IplImage.create(image.width(), image.height(),
				IPL_DEPTH_8U, 1);

		cvCvtColor(image, grayImg, opencv_imgproc.CV_BGR2GRAY);

		return grayImg;
	}


	public void load() {
		train();
	}

	public int getProb() {
		// TODO Auto-generated method stub
		return mProb;
	}


}