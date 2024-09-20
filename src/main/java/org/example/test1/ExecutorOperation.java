package org.example.test1;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import static org.example.test1.Dialog.showErrDialog;

public class ExecutorOperation {
    public static void shutdown(ExecutorService executorService, ScheduledExecutorService scheduler) {
        executorService.shutdown();
        scheduler.shutdown();
    }
    // 使用系统默认应用打开文件的方法
    public static void openFileWithDefaultApp(File file) {

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                showErrDialog("无法打开文件", "文件打开失败: " + e.getMessage());
            }
        } else {
            showErrDialog("不支持的操作", "当前系统不支持打开文件");
        }
    }
    //搜索进程
    public static void searchDirRecursive(File dir, String keyword, List<File> results) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        File[] files = dir.listFiles();
        System.out.println("正在搜索中");

        if (files != null) {
            for (File file : files) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                if(file.isHidden()){
                    continue;
                }
                //匹配：
                if(file.isDirectory()){
                    if(file.getName().contains(keyword)){
                        results.add(file);
                    }
                    searchDirRecursive(file,keyword,results);
                }else {
                    if(file.getName().contains(keyword)){
                        results.add(file);
                    }
                }
            }
        }
    }
}
