package com.firejq;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 *
 * @author <a href="mailto:firejq@outlook.com">firejq</a>
 */
public class CompleteRateThread extends Thread {
	private DownloadUtil downloadUtil;

	public CompleteRateThread(DownloadUtil downloadUtil) {
		this.downloadUtil = downloadUtil;
	}

	@Override
	public void run() {
		double processRate;
		// 每隔 1 秒查询一次任务的完成进度并记录每个线程的下载进度
		while((processRate = downloadUtil.getCompleteRate()) < 1) {
			System.out.println("已完成：" + processRate * 100 + "%");

			downloadUtil.recordCompleteRate();

			try {
				Thread.sleep(1000L);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// 下载完成，修改文件名（删掉tmp后缀）
		this.downloadUtil.changeFileName();
	}
}
