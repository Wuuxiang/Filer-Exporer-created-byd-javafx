package org.example.test1;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.example.test1.Dialog.showErrDialog;
import static org.example.test1.ExecutorOperation.searchDirRecursive;
import static org.example.test1.FileExplorer.displaySearchResults;
import static org.example.test1.FileExplorer.treeViews;
import static org.example.test1.imageOperation.createImageView;
import static org.example.test1.imageOperation.getImagePath;

public class FileOperation {
    // 文件复制
    public static void copyFile(Path source, Path destination) throws IOException {
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS);
    }
    // 目录复制（递归复制）
    public static void copyDirectory(Path source, Path destination) throws IOException {
        if (Files.isDirectory(source)) {
            if (!Files.exists(destination)) {
                Files.createDirectories(destination);
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(source)) {
                for (Path entry : stream) {
                    copyDirectory(entry, destination.resolve(entry.getFileName()));
                }
            }
        } else {
            copyFile(source, destination);
        }
    }
    //展示搜索文件的结果

    //搜索跳转
    static void highlight(File file, TreeView<File> treeView) {
        TreeItem<File> treeItem = findTreeItem(treeView.getRoot(), file);
        if (treeItem != null) {
            treeView.getSelectionModel().select(treeItem); // 高亮选中文件
            treeView.scrollTo(treeView.getRow(treeItem)); // 滚动到该位置
        }
    }
    //回溯树
    public static TreeItem<File> findTreeItem(TreeItem<File> root, File targetDir) {
        if (root.getValue().equals(targetDir)) {
            return root;
        }
        for (TreeItem<File> child : root.getChildren()) {
            TreeItem<File> found = findTreeItem(child, targetDir);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public static File getFileByName(List<File> list,String fileName) {
        for(File file : list){
            if(file.getAbsolutePath().equals(fileName)){
                return file;
            }
        }
        return null;
    }
    public static void searchFiles(String keyword, ComboBox<File> driSelector, ExecutorService executorService,ScheduledExecutorService scheduler) {
        // 清空右侧区域
        File rootDirectory = new File(String.valueOf(driSelector.getValue())); // 指定搜索的根目录

        // 创建一个用于显示“正在搜索”的弹窗
        Alert searchingAlert = new Alert(Alert.AlertType.INFORMATION, "正在搜索，请稍候...", ButtonType.CANCEL);
        searchingAlert.setTitle("搜索中");
        searchingAlert.setHeaderText(null);
        // 禁用点击关闭窗口的功能，直到任务完成
        Stage stage = (Stage) searchingAlert.getDialogPane().getScene().getWindow();
        stage.setOnCloseRequest(event -> event.consume());

        // 在主线程中显示弹窗
        Platform.runLater(searchingAlert::show);

        Future<List<File>> future = executorService.submit(() -> {
            List<File> results = new ArrayList<>();
            searchDirRecursive(rootDirectory, keyword, results);
            return results;
        });

        // 设置弹窗取消按钮的操作
        searchingAlert.resultProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == ButtonType.CANCEL) {
                future.cancel(true); // 取消搜索任务
                Platform.runLater(() -> {
                    searchingAlert.close();
                });
            }
        });

        // 定时10s，超时后取消搜索任务
        scheduler.schedule(() -> {
            if (!future.isDone()) {
                future.cancel(true);
                Platform.runLater(() -> {
                    searchingAlert.close();
                    showErrDialog("搜索超时", "搜索失败");
                });
            }
        }, 10, TimeUnit.SECONDS);

        executorService.submit(() -> {
            try {
                List<File> results = future.get();
                if (results.isEmpty()) {
                    Platform.runLater(() -> {
                        searchingAlert.close();
                        showErrDialog("没有找到此文件!", "搜索失败");
                    });
                } else {
                    Platform.runLater(() -> {
                        searchingAlert.close();
                        displaySearchResults(results);
                    });
                }
            } catch (InterruptedException | ExecutionException e) {
                // 处理异常
                e.printStackTrace();
            } catch (CancellationException e) {
                // 任务被取消
                searchingAlert.close();
                showErrDialog("搜索任务被取消!","搜索失败");
            }
        });
    }

    //构建树状目录
    public static void buildFileTree(File directory, TreeItem<File> parentItem) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isHidden()) continue;
                TreeItem<File> treeItem = new TreeItem<>(file) {
                    @Override
                    public String toString() {
                        return file.getName(); // 只显示文件名
                    }
                };
                if (file.isDirectory()) {
                    treeItem.setGraphic(createImageView(getImagePath(file)));
                    if (file.listFiles() != null && file.listFiles().length > 0)
                        treeItem.getChildren().add(new TreeItem<>(new File("Loading...")));
                    treeItem.addEventHandler(TreeItem.branchExpandedEvent(), event -> {
                        if (treeItem.getChildren().size() == 1 && "Loading...".equals(treeItem.getChildren().get(0).getValue().getName())) {
                            treeItem.getChildren().clear();
                            buildFileTree(file, treeItem);
                        }
                    });
                } else {
                    ImageView fileIcon = createImageView(getImagePath(file));
                    treeItem.setGraphic(fileIcon);
                }
                parentItem.getChildren().add(treeItem);
            }
        }
    }

    public static boolean updateTree(TreeItem<File> livePlace,File clipboardFile,TreeItem<File> originPlace) {
        //步骤： (剪切)删除原节点，(复制)增加新节点，重绘制界面

        //删
        if (originPlace != null) {//若为空就是采用了复制
            TreeItem<File> parentItem = originPlace.getParent();
            if (parentItem != null) {
                parentItem.getChildren().remove(originPlace);
            }// else :删除磁盘(磁盘没🐎)
        }

        //加
        if (livePlace != null) {
            TreeItem<File> newItem = new TreeItem<>(clipboardFile);
            newItem.setGraphic(createImageView(getImagePath(clipboardFile)));

            ObservableList<TreeItem<File>> list = livePlace.getChildren();
            //文件名已存在判断：
            for(TreeItem<File> file : list){
                if(file.getValue().getName().equals(clipboardFile.getName())){
                    return false;
                }
            }
            livePlace.getChildren().add(newItem);
            livePlace.setExpanded(true); // 展开目标文件夹
            if(clipboardFile.isDirectory()){
                //重绘子目录的文件
                buildFileTree(clipboardFile,newItem);
            }
            return true;
        }
        return false;
    }
    //算长度
    public static String getFileLength(File file) {
        long length = file.length(); // 获取文件的字节大小

        // 定义各单位的大小
        final long KILOBYTE = 1024;
        final long MEGABYTE = KILOBYTE * 1024;
        final long GIGABYTE = MEGABYTE * 1024;
        final long TERABYTE = GIGABYTE * 1024;

        // 根据文件大小选择合适的单位
        if (length >= TERABYTE) {
            return String.format("%.2f TB", (double) length / TERABYTE);
        } else if (length >= GIGABYTE) {
            return String.format("%.2f GB", (double) length / GIGABYTE);
        } else if (length >= MEGABYTE) {
            return String.format("%.2f MB", (double) length / MEGABYTE);
        } else if (length >= KILOBYTE) {
            return String.format("%.2f KB", (double) length / KILOBYTE);
        } else {
            return length + " Bytes"; // 小于 1KB 的文件直接显示字节数
        }
    }
}
