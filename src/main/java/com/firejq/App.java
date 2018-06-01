package com.firejq;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 *
 * @author <a href="mailto:firejq@outlook.com">firejq</a>
 */
public class App {
	/**
	 * 程序入口
	 * @param args 目标URL 本地路径 线程数
	 */
	public static void main(String [] args) {
		int threadNum = 4; // 默认值为 4
		if (args.length >= 3) {
			threadNum = Integer.parseInt(args[2]);
		}
		if (args.length >= 2) {
			String target = args[0];
			String path = args[1];
			// 初始化DownUtil对象
			DownloadUtil downUtil = new DownloadUtil(target, path, threadNum);
			// 开始下载
			downUtil.download();
			// 每隔0.1秒查询一次任务的完成进度，输出显示并记录
			new CompleteRateThread(downUtil).start();
		}
	}
}
