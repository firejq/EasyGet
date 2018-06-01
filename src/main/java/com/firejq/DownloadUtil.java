package com.firejq;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 *
 * @author <a href="mailto:firejq@outlook.com">firejq</a>
 */
public class DownloadUtil {
	// 进度记录文件后缀名
	private static final String RECORD_FILE_SUFFIX = ".record";

	// 临时下载文件后缀名
	private static final String TEMP_FILE_SUFFIX = ".tmp";

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

			int eachPartSize = this.targetSize / this.threadNum + 1;
			int [] completeRates = new int[this.threadNum];
			// 检查是否是恢复下载
			if (!this.isRestoration()) {
				// 首次下载：先设置文件大小
				try (RandomAccessFile file = new RandomAccessFile(
						this.savePath + this.targetName
								+ TEMP_FILE_SUFFIX,
						"rw")) {
					file.setLength(this.targetSize);
				}
			} else {
				// 恢复下载：获取每个线程的已下载大小
				completeRates = this.getRecordCompleteRate();
			}

			for (int i = 0; i < threadNum; i++) {
				// 计算每个线程的下载的开始位置
				int startPos = i * eachPartSize + completeRates[i];
				// 每个线程使用一个 RandomAccessFile 进行下载
				RandomAccessFile currentPart = new RandomAccessFile(
						this.savePath + this.targetName
								+ TEMP_FILE_SUFFIX,
						"rw");
				// 定位该线程的下载位置
				currentPart.seek(startPos);
				// 创建下载线程
				downloadThreads[i] = new DownloadThread(targetUrl,
														startPos,
														eachPartSize,
														currentPart);
				// 启动下载线程
				downloadThreads[i].start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 检查是否是恢复下载
	 * @return
	 */
	private boolean isRestoration() {
		File file = new File(this.savePath
									 + this.targetName
									 + RECORD_FILE_SUFFIX);
		return file.exists();
	}

	/**
	 * 获取记录文件中的每个线程的已下载大小
	 * 逐行读取.record文件，封装成数组返回
	 * @return
	 */
	private int [] getRecordCompleteRate() {
		int [] rates = new int[this.threadNum];
		try (BufferedReader fReader = new BufferedReader(
				new FileReader(this.savePath + this.targetName
									   + RECORD_FILE_SUFFIX))) {
			String s;
			int i = 0;
			while ((s = fReader.readLine()) != null && i <= this.threadNum) {
				rates[i++] = Integer.parseInt(s);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return rates;
	}

	/**
	 * 获取下载的完成百分比
	 * @return
	 */
	public double getCompleteRate() {
		// 统计多个线程已经下载的总大小
		int sumSize = 0;
		for (int i = 0; i < threadNum; i++) {
			sumSize += downloadThreads[i].getFinishedSize();
		}
		// 返回已经完成的百分比
		return Double.parseDouble(
				String.format("%.2f", sumSize * 1.0 / this.targetSize));
	}

	/**
	 * 记录每个线程已下载的大小
	 * @return
	 */
	public void recordCompleteRate() {
		// 将已经完成的大小记录到文件中
		try (FileWriter fWriter = new FileWriter(
				new File(this.savePath + this.targetName + RECORD_FILE_SUFFIX)))
		{
			for (int i = 0; i < threadNum; i++) {
				fWriter.write(String.valueOf(downloadThreads[i]
													 .getFinishedSize()) + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 删除临时下载文件的后缀名
	 */
	public void changeFileName() {
		new File(this.getSavePath()
						 + this.getTargetName()
						 + TEMP_FILE_SUFFIX)
				.renameTo(new File(this.getSavePath()
										   + this.getTargetName()));
	}

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
}

