package com.henugao.mobilemulti;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
	ProgressBar pb ;
	TextView tv;
	// 要下载资源的服务器地址
	String path = "http://192.168.80.1:8080/download/apache-tomcat-6.0.26.exe";
	int finishCount = 0;
	int threadCount = 3;
	int currentLength;

	// 从请求的Url中获取文件名
	public String getFileFromPath(String path) {
		int index = path.lastIndexOf("/");
		return path.substring(index + 1);
	}


	Handler handler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			//把变量改成long，否则在计算大的文件时，容易出错
			tv.setText((long)pb.getProgress() *100 /pb.getMax()+"%");
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		pb = (ProgressBar) findViewById(R.id.pb);
		tv = (TextView) findViewById(R.id.tv);
	}

	public void click(View v) {
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					URL url = new URL(path);
					HttpURLConnection conn = (HttpURLConnection) url
							.openConnection();
					conn.setRequestMethod("GET");
					conn.setConnectTimeout(5000);
					conn.setReadTimeout(5000);
					if (conn.getResponseCode() == 200) {
						// 获得内容的大小
						int length = conn.getContentLength();
						pb.setMax(length);
						int size = length / threadCount;
						for (int i = 0; i < threadCount; i++) {
							int startIndex = i * size;
							int endIndex = (i + 1) * size - 1;
							if (i == threadCount - 1) {
								endIndex = length - 1;
							}
							new DownloadThread(startIndex, endIndex, i).start();
						}
					}

				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		t.start();
	}

	class DownloadThread extends Thread {
		int startIndex; // 每个线程下载的起始位置
		int endIndex; // 每个线程下载的结束位置
		int threadID; // 线程的ID号

		public DownloadThread(int startIndex, int endIndex, int threadID) {
			super();
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.threadID = threadID;
		}

		@Override
		public void run() {
			URL url;
			try {
				File preocessFile = new File(Environment.getExternalStorageDirectory(),threadID + ".txt");
				if (preocessFile.exists()) {
					FileInputStream fis = new FileInputStream(preocessFile);
					BufferedReader br = new BufferedReader(
							new InputStreamReader(fis));
					int sizeLength = Integer.parseInt(br.readLine());
					startIndex += sizeLength;
					//从上次下载的位置显示进度条
					currentLength+=sizeLength;
					pb.setProgress(currentLength);
					//通知主线程，刷新文本
					handler.sendEmptyMessage(1);
					fis.close();
				}
				url = new URL(path);
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setRequestMethod("GET");
				conn.setConnectTimeout(5000);
				conn.setReadTimeout(5000);
				// 设定本线程请求数据的范围
				conn.setRequestProperty("Range", "bytes=" + startIndex + "-"
						+ endIndex);
				// 请求部分数据，响应码是206
				if (conn.getResponseCode() == 206) {
					File file = new File(Environment.getExternalStorageDirectory(),getFileFromPath(path));
					// 生成临时文件
					RandomAccessFile raf = new RandomAccessFile(file, "rwd");
					//设置每个线程的下载的起始位置，防止覆盖。
					raf.seek(startIndex);
					// 获得响应流,流里面只有1/3的原文件数据
					InputStream is = conn.getInputStream();
					byte[] b = new byte[1024];
					int len = 0;
					int total = 0;
					while ((len = is.read(b)) != -1) {
						// 将当前线程的下载进度存放在一个以自己的线程号命名的子文件中
						RandomAccessFile ra = new RandomAccessFile(
								preocessFile, "rwd");
						total += len;
						raf.write(b, 0, len);
						ra.write((total + "").getBytes());
						//每次读取数据流之后，将当前的进度显示在进度条上
						currentLength+=len;
						pb.setProgress(currentLength);
						//通知主线程，刷新文本
						handler.sendEmptyMessage(1);
						System.out.println(threadID+"下载了："+total);
						ra.close();

					}
					raf.close();
					System.out.println("线程" + threadID + "下载完毕");
					
					finishCount++;
					synchronized (path) {
						if (finishCount == threadCount) {
							for (int i = 0; i < threadCount; i++) {
								File f = new File(Environment.getExternalStorageDirectory(),i + ".txt");
								f.delete();
							}
							finishCount = 0;
						}

					}

				}

			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
