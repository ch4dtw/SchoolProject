package org.opencv.javacv.facerecognition;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;


import android.os.Environment;
import android.util.Log;

public class labels {

	String mPath;
	//-----------------------------------------------------------------------------------//
	class label {
		int num;
		String thelabel;

		public label(String s, int n)
		{
			thelabel = s;
			num = n;
		}
	}
	//-----------------------------------------------------------------------------------//
	ArrayList<label> thelist = new ArrayList<label>();
	//-----------------------------------------------------------------------------------//
	//建構式
	public labels(String Path)
	{
		mPath=Path;
	}
	//-----------------------------------------------------------------------------------//
	//檢查是否為空
	public boolean isEmpty()
	{
		return !(thelist.size()>0);
	}
	//-----------------------------------------------------------------------------------//
	//新增元素
	public void add(String s,int n)
	{
		thelist.add( new label(s,n));
	}
	//-----------------------------------------------------------------------------------//
	//取得元素(int)
	public String get(int i) {
		Iterator<label> Ilabel = thelist.iterator();//Ilabel可以取出thelist集合內所有的值
		while (Ilabel.hasNext()) {
			label l = Ilabel.next();
			if (l.num==i)
				return l.thelabel;
		}
		return "";
	}
	//-----------------------------------------------------------------------------------//
	//取得元素(string)
	public int get(String s) {
		Iterator<label> Ilabel = thelist.iterator();//Ilabel可以取出thelist集合內所有的值
		while (Ilabel.hasNext()) {
			label l = Ilabel.next();
			if (l.thelabel.equalsIgnoreCase(s)) return l.num;//如果名子相同 就回傳該名子的起始編號
			// 都沒有 則回傳-1
		}
		return -1;
	}
	//-----------------------------------------------------------------------------------//
	public void Save()
	{
		try {
			File f=new File (mPath+"faces.txt");
			f.createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			Iterator<label> Ilabel = thelist.iterator();
			while (Ilabel.hasNext()) {
				label l = Ilabel.next();
				bw.write(l.thelabel+","+l.num);
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e("error", e.getMessage() + " " + e.getCause());
			e.printStackTrace();
		}
	}
	//-----------------------------------------------------------------------------------//
	public void Read() {
		try {

			FileInputStream fstream = new FileInputStream(
					mPath+"faces.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(
					fstream));

			String strLine;
			thelist= new ArrayList<label>();
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				StringTokenizer tokens=new StringTokenizer(strLine,",");
				String s=tokens.nextToken();
				String sn=tokens.nextToken();

				thelist.add(new label(s,Integer.parseInt(sn)));
			}
			br.close();
			fstream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int max() {
		int m=0;
		Iterator<label> Ilabel = thelist.iterator();
		while (Ilabel.hasNext()) {
			label l = Ilabel.next();
			if (l.num>m) m=l.num;
		}
		return m;
	}

}
