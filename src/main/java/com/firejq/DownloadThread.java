package com.firejq;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 *
 * @author <a href="mailto:firejq@outlook.com">firejq</a>
 */
public class DownloadThread  extends Thread {

	private String targetUrl;
	// 当前线程的下载位置
	private int startPos;
	// 定义当前线程负责下载的文件大小
	private int currentPartSize;
	// 当前线程需要下载的文件块
	private RandomAccessFile currentPart;
	// 定义已经该线程已下载的字节数
	private int finishedSize;

	public int getFinishedSize() {
		return finishedSize;
	}

	public DownloadThread(String targetUrl,
						  int startPos,
						  int currentPartSize,
						  RandomAccessFile currentPart) {
		this.targetUrl = targetUrl;
		this.startPos = startPos;
		this.currentPartSize = currentPartSize;
		this.currentPart = currentPart;
	}

	@Override
	public void run() {
		try {
			URL url = new URL(this.targetUrl);
			HttpURLConnection conn = (HttpURLConnection)url
					.openConnection();
			conn.setConnectTimeout(5 * 1000);
			conn.setRequestMethod("GET");
			conn.setRequestProperty(
					"Accept",
					"image/gif, image/jpeg, image/pjpeg, image/pjpeg, " +
							"application/x-shockwave-flash, " +
							"application/xaml+xml, " +
							"application/vnd.ms-xpsdocument, " +
							"application/x-ms-xbap, " +
							"application/x-ms-application, " +
							"application/vnd.ms-excel, " +
							"application/vnd.ms-powerpoint, " +
							"application/msword, */*");
			conn.setRequestProperty("Accept-Language", "zh-CN");
			conn.setRequestProperty("Charset", "UTF-8");
			InputStream inStream = conn.getInputStream();
			// 跳过startPos个字节，表明该线程只下载自己负责哪部分文件。
			inStream.skip(this.startPos);
			byte[] buffer = new byte[1024];
			int hasRead = 0;
			// 读取网络数据，并写入本地文件
			while (finishedSize < currentPartSize
					&& (hasRead = inStream.read(buffer)) != -1) {
				currentPart.write(buffer, 0, hasRead);
				// 累计该线程下载的总大小
				finishedSize += hasRead;
			}
			currentPart.close();
			inStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
