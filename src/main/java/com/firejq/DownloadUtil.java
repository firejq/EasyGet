package com.firejq;

import java.io.File;
import java.io.IOException;
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
public class DownloadUtil {
	// 目标资源的 URL
	private String targetUrl;

	// 目标资源的总大小（字节）
	private int targetSize;

	// 目标资源名
	private String targetName;

	// 本地保存路径
	private String savePath;

	// 启用线程数量
	private int threadNum;

	// 下载线程的对象数组
	private DownloadThread [] downloadThreads;

	public String getTargetUrl() {
		return targetUrl;
	}

	public int getTargetSize() {
		return targetSize;
	}

	public String getTargetName() {
		return targetName;
	}

	public String getSavePath() {
		return savePath;
	}

	public int getThreadNum() {
		return threadNum;
	}

	public DownloadUtil(String targetUrl, String savePath, int threadNum) {
		this.targetUrl = targetUrl;
		this.threadNum = threadNum;
		this.savePath = savePath.endsWith(File.separator) ?
						savePath : savePath + File.separator;
		this.downloadThreads = new DownloadThread[threadNum];
	}

	/**
	 * 实现下载逻辑
	 */
	public void download() {
		try {
			URL url = new URL(this.targetUrl);
			String [] targetPath = url.getFile().split("/");
			this.targetName = targetPath[targetPath.length - 1];
			System.out.println();

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5000);
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
			conn.setRequestProperty("Charset", "UTF-8");
			conn.setRequestProperty("Connection", "Keep-Alive");
			this.targetSize = conn.getContentLength();
			if (this.targetSize <= 0) {
				throw new RuntimeException("目标资源异常");
			}

			System.out.println(this.savePath + this.targetName);
			System.out.println("File size: " + this.targetSize / 1024 + " Kb");
			conn.disconnect();

			int currentPartSize = this.targetSize / this.threadNum + 1;
			try (RandomAccessFile file = new RandomAccessFile(
					this.savePath + this.targetName  + ".tmp",
					"rw")) {
				file.setLength(this.targetSize);
			}
			for (int i = 0; i < threadNum; i++) {
				// 计算每个线程的下载的开始位置
				int startPos = i * currentPartSize;
				// 每个线程使用一个 RandomAccessFile 进行下载
				RandomAccessFile currentPart = new RandomAccessFile(
						this.savePath + this.targetName  + ".tmp",
						"rw");
				// 定位该线程的下载位置
				currentPart.seek(startPos);
				// 创建下载线程
				downloadThreads[i] = new DownloadThread(startPos,
														currentPartSize,
														currentPart);
				// 启动下载线程
				downloadThreads[i].start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取下载的完成百分比
	 * @return
	 */
	public double getCompleteRate() {
		// 统计多条线程已经下载的总大小
		int sumSize = 0;
		for (int i = 0; i < threadNum; i++) {
			sumSize += downloadThreads[i].getFinishedSize();
		}
		// 返回已经完成的百分比
		return Double.parseDouble(
				String.format("%.2f", sumSize * 1.0 / this.targetSize));
	}

	/**
	 * 下载线程实现
	 */
	private class DownloadThread extends Thread {
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

		DownloadThread(int startPos, int currentPartSize,
					   RandomAccessFile currentPart) {
			this.startPos = startPos;
			this.currentPartSize = currentPartSize;
			this.currentPart = currentPart;
		}

		@Override
		public void run() {
			try {
				URL url = new URL(DownloadUtil.this.targetUrl);
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

	/**
	 * 程序入口
	 * @param args 目标URL 本地路径 线程数
	 */
	public static void main(String [] args) {
		// todo 参数校验
		String target = args[0];
		String path = args[1];
		int threadNum = Integer.parseInt(args[2]);
		// 初始化DownUtil对象
		DownloadUtil downUtil = new DownloadUtil(target, path, threadNum);
		// 开始下载
		downUtil.download();
		new Thread(() -> {
			double processRate;
			while((processRate = downUtil.getCompleteRate()) < 1) {
				// 每隔0.1秒查询一次任务的完成进度
				System.out.println("已完成：" + processRate * 100 + "%");
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			new File(downUtil.getSavePath()
							 + downUtil.getTargetName()
							 + ".tmp")
					.renameTo(new File(downUtil.getSavePath()
											   + downUtil.getTargetName()));
		}).start();
	}
}

