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
	// Ҫ������Դ�ķ�������ַ
	String path = "http://192.168.80.1:8080/download/apache-tomcat-6.0.26.exe";
	int finishCount = 0;
	int threadCount = 3;
	int currentLength;

	// �������Url�л�ȡ�ļ���
	public String getFileFromPath(String path) {
		int index = path.lastIndexOf("/");
		return path.substring(index + 1);
	}


	Handler handler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			//�ѱ����ĳ�long�������ڼ������ļ�ʱ�����׳���
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
						// ������ݵĴ�С
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
		int startIndex; // ÿ���߳����ص���ʼλ��
		int endIndex; // ÿ���߳����صĽ���λ��
		int threadID; // �̵߳�ID��

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
					//���ϴ����ص�λ����ʾ������
					currentLength+=sizeLength;
					pb.setProgress(currentLength);
					//֪ͨ���̣߳�ˢ���ı�
					handler.sendEmptyMessage(1);
					fis.close();
				}
				url = new URL(path);
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setRequestMethod("GET");
				conn.setConnectTimeout(5000);
				conn.setReadTimeout(5000);
				// �趨���߳��������ݵķ�Χ
				conn.setRequestProperty("Range", "bytes=" + startIndex + "-"
						+ endIndex);
				// ���󲿷����ݣ���Ӧ����206
				if (conn.getResponseCode() == 206) {
					File file = new File(Environment.getExternalStorageDirectory(),getFileFromPath(path));
					// ������ʱ�ļ�
					RandomAccessFile raf = new RandomAccessFile(file, "rwd");
					//����ÿ���̵߳����ص���ʼλ�ã���ֹ���ǡ�
					raf.seek(startIndex);
					// �����Ӧ��,������ֻ��1/3��ԭ�ļ�����
					InputStream is = conn.getInputStream();
					byte[] b = new byte[1024];
					int len = 0;
					int total = 0;
					while ((len = is.read(b)) != -1) {
						// ����ǰ�̵߳����ؽ��ȴ����һ�����Լ����̺߳����������ļ���
						RandomAccessFile ra = new RandomAccessFile(
								preocessFile, "rwd");
						total += len;
						raf.write(b, 0, len);
						ra.write((total + "").getBytes());
						//ÿ�ζ�ȡ������֮�󣬽���ǰ�Ľ�����ʾ�ڽ�������
						currentLength+=len;
						pb.setProgress(currentLength);
						//֪ͨ���̣߳�ˢ���ı�
						handler.sendEmptyMessage(1);
						System.out.println(threadID+"�����ˣ�"+total);
						ra.close();

					}
					raf.close();
					System.out.println("�߳�" + threadID + "�������");
					
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
