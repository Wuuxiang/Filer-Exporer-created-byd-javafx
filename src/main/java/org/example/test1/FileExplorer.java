package org.example.test1;

import javafx.application.Application;
import javafx.application.Platform;

import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.example.test1.Dialog.*;
import static org.example.test1.ExecutorOperation.openFileWithDefaultApp;
import static org.example.test1.FileOperation.*;
import static org.example.test1.imageOperation.*;

import java.util.concurrent.*;


public class FileExplorer extends Application {
    //磁盘选择器
    private static ComboBox<File> driSelector = new ComboBox<>();
    //查看方法选择器
    private static ComboBox<String> checkSelect = new ComboBox<>();
    //树状目录
    private static TreeView<File> treeView = new TreeView<>();
    //剪切框
    private static File clipboardFile = null;
    //剪切文件原本所在树状节点
    private static TreeItem<File> originPlace;
    //是否进行剪切
    private static boolean isCut = false;
    //多线性池搜索
    private static ExecutorService executorService = Executors.newFixedThreadPool(4); // 创建线程池
    //左右屏
    private static SplitPane splitPane = new SplitPane();
    //搜索计时器
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static ArrayList<DriveTreeView> treeViews = new ArrayList<>();
    private static Button picShowBt = new Button("大图标查看");
    private static Button tailShowBt = new Button("详细信息查看");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage mainStage) {
        //容器初始化
        mainStage.setTitle("类Windows简易文件管理器");
        BorderPane root = new BorderPane();//主屏
        BorderPane UIpane = new BorderPane();//功能选项屏
        HBox buttonBox = new HBox(10);//功能选项容器
        Scene scene = new Scene(root, 1100, 750);

        //容器自定义

        Button searchButton = new Button("在本磁盘搜索");
        TextField searchField = new TextField();
        searchField.setPromptText("输入关键字搜索文件");
        checkSelect.setPromptText("选择查看方式");

        //容器功能配置
        driSelector.getItems().addAll(File.listRoots());
        driSelector.setValue(new File("D:\\"));
        checkSelect.getItems().addAll(picShowBt.getText(), tailShowBt.getText());
        checkSelect.setValue(picShowBt.getText());

        splitPane.getItems().addAll(treeView, new BorderPane());  // 初始状态下，右边为详细信息视图
        buttonBox.getChildren().addAll(checkSelect, searchField, searchButton, driSelector); //添加添加全部添加
        UIpane.setCenter(buttonBox);
        root.setTop(UIpane);
        root.setCenter(splitPane);
        mainStage.setScene(scene);

        //显示初始化
        loadDrive(driSelector.getValue());

        //对磁盘选取器赋予功能(✔ 打勾勾)
        driSelector.setOnAction(e -> loadDrive(driSelector.getValue()));
        checkSelect.setOnAction(event -> {
            String selectOption = checkSelect.getValue();
            if (selectOption.equals("大图标查看")) {
                picShowBt.fire();
            } else if (selectOption.equals("详细信息查看")) {
                tailShowBt.fire();
            }
        });

        // 点击 "详细信息查看" 按钮事件(✔ 打勾勾)
        tailShowBt.setOnAction(e -> {
            //赋予左边的盘右键点击功能
            TreeItem<File> selectItem = treeView.getSelectionModel().getSelectedItem();
            if (selectItem != null) {
                File dir = selectItem.getValue();
                File[] files = dir.listFiles();
                if (files != null) {
                    List<File> list = new ArrayList<>();
                    if (files != null) {
                        list.addAll(List.of(files));
                    }
                    displaySearchResults(list);
                }
            }
        });

        // 点击 "大图标查看" 按钮事件(✔ 打勾勾)
        picShowBt.setOnAction(e -> {
            FlowPane iconViewPane = new FlowPane();
            iconViewPane.setHgap(20);
            iconViewPane.setVgap(20);
            ScrollPane picWall = new ScrollPane(iconViewPane);
            picWall.setFitToWidth(true);  // 自适应适应宽度
            picWall.setFitToHeight(true);  // 自适应高度

            // 遍历文件目录并添加图标
            TreeItem<File> selectedItem = treeView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                //也就是说如果点击的是文件夹的话就展开文件夹就可以了
                File directory = selectedItem.getValue();
                File[] files = directory.listFiles();
                Label label;
                if (files != null) {

                    MenuItem pasteItem = new MenuItem("粘贴");
                    ContextMenu pastMenu = new ContextMenu();
                    pastMenu.getItems().add(pasteItem);
                    //粘贴

                    pasteItem.setOnAction(event -> {
                        File target = treeView.getSelectionModel().getSelectedItem().getValue();
                        if (target.isDirectory() && clipboardFile != null) {
                            File destFile = new File(target, clipboardFile.getName());
                            try {
                                if (isCut) {
                                    if (clipboardFile.renameTo(destFile)) {
                                        showInfoDialog("剪切成功", "成功");
                                        clipboardFile = destFile;
                                        updateTree(treeView.getSelectionModel().getSelectedItem(), clipboardFile, originPlace);
                                        iconViewPane.getChildren().add(creatFileLabel(clipboardFile));

                                        clipboardFile = null;
                                        isCut = false;

                                    } else showInfoDialog("剪切失败", "失败");

                                } else {
                                    copyDirectory(clipboardFile.toPath(), destFile.toPath());
                                    if (updateTree(treeView.getSelectionModel().getSelectedItem(), clipboardFile, originPlace)) {
                                        showInfoDialog("复制成功", "成功");
                                        iconViewPane.getChildren().add(creatFileLabel(clipboardFile));

                                    } else {
                                        showInfoDialog("复制失败,文件名已存在", "失败");
                                    }
                                }

                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                        } else System.out.println("这可不是文件夹，不能复制");
                    });
                    pasteItem.setDisable(clipboardFile == null);

                    picWall.setOnContextMenuRequested(event -> {
                        if (clipboardFile != null) {
                            pastMenu.show(picWall, event.getScreenX(), event.getScreenY());
                        }
                    });

                    if (!directory.isDirectory()) {
                        //不是文件夹
                        iconViewPane.getChildren().add(creatFileLabel(directory));
                    } else {
                        //是文件夹
                        for (File file : files) {
                            if (file.isHidden()) {
                                continue;
                            }
                            label = creatFileLabel(file);
                            iconViewPane.getChildren().add(label);
                            if (file.isDirectory()) {
                                //配置文件夹图标
                                label.setOnMouseClicked(event -> {
                                    if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                                        TreeItem<File> folderItem = findTreeItem(selectedItem, file);
                                        if (folderItem != null) {
                                            // 选中文件夹对应的 TreeItem
                                            treeView.getSelectionModel().select(folderItem);
                                            // 触发按钮点击事件
                                            picShowBt.fire();
                                        }
                                    }
                                });
                            }
                            // 为每个 label 添加右键菜单
                            ContextMenu contextMenu = new ContextMenu();
                            MenuItem copyItem = new MenuItem("复制");
                            MenuItem cutItem = new MenuItem("剪切");
                            contextMenu.getItems().addAll(copyItem, cutItem);

                            // 添加事件处理
                            copyItem.setOnAction(event -> {
                                clipboardFile = file;
                                originPlace = null;
                                isCut = false;

                            });
                            cutItem.setOnAction(event -> {
                                clipboardFile = file;
                                originPlace = treeView.getSelectionModel().getSelectedItem();
                                isCut = true;

                            });
                            Label lastLabel = label;
                            label.setOnContextMenuRequested(event -> {
                                Platform.runLater(() -> {
                                    if (pastMenu.isShowing()) {
                                        pastMenu.hide();
                                    }
                                });
                                if (!pastMenu.isShowing()) {
                                    contextMenu.show(lastLabel, event.getScreenX(), event.getScreenY());
                                }
                            });
                        }
                    }

                    splitPane.getItems().set(1, picWall);
                    // 如果点击的地方不在右键菜单中，隐藏菜单
                    splitPane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                        if (pastMenu.isShowing()) {
                            pastMenu.hide();
                        }
                    });
                } else {
                    splitPane.getItems().set(1, new TextArea("(这不是文件夹/文件夹为空)"));
                }
            }

        });

        // 搜索按钮的事件处理(✔ 打勾勾)
        searchButton.setOnAction(e -> {
            String keyword = searchField.getText().trim();
            if (!keyword.isEmpty()) {
                searchFiles(keyword, driSelector, executorService, scheduler); //搜！
            } else {
                showInfoDialog("请输入搜索关键字!", "!");
            }
        });

        //回车快捷搜索(✔ 打勾勾)
        searchField.setOnKeyPressed(event -> {
            // 检查是否回车
            if (event.getCode() == KeyCode.ENTER) {
                searchButton.fire();
            }
        });
        // 设置关闭方式
        mainStage.setOnCloseRequest(event -> {
            System.exit(0);
        });

        mainStage.show();
    }

    public static void loadDrive(File drive) {
        if (drive.exists() && drive.isDirectory()) {

            // 检查是否已经有对应的磁盘 TreeView
            for (DriveTreeView driveTreeView : treeViews) {
                if (driveTreeView.getDrive().equals(drive)) {
                    // 如果已经存在，直接使用现有的 TreeView
                    treeView = driveTreeView.getTreeView();
                    splitPane.getItems().set(1, new BorderPane()); // 加载 UI
                    splitPane.getItems().set(0, treeView);
                    return;
                }
            }

            // 如果不存在，则创建新的 TreeView
            TreeItem<File> rootItem = new TreeItem<>(drive);
            rootItem.setExpanded(true);
            buildFileTree(drive, rootItem);

            TreeView<File> newTreeView = new TreeView<>(rootItem);
            treeView = newTreeView; // 更新当前的 treeView

            // 将新创建的 TreeView 存入 treeViews 列表
            treeViews.add(new DriveTreeView(drive, newTreeView));

            // 为树状目录添加右键选项卡(✔ 打勾勾)
            treeView.setCellFactory(tv -> {

                //被右键的文件（悲）
                TreeCell<File> cell = new TreeCell<File>() {
                    @Override
                    protected void updateItem(File item, boolean empty) {
                        super.updateItem(item, empty);
                        //继承老的重写自定义
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setText(item.getName());

                            // 保留树节点已有的图标
                            TreeItem<File> treeItem = getTreeItem();
                            if (treeItem != null) {
                                setGraphic(treeItem.getGraphic());  // 重新设置图标
                            }
                        }
                    }
                };

                cell.setOnMouseClicked(event -> {
                    //是不是鼠标右键（sec是第二个）
                    if (!cell.isEmpty() && event.getButton() == MouseButton.SECONDARY) {
                        ContextMenu Menu = new ContextMenu();
                        MenuItem cutItem = new MenuItem("剪切");
                        MenuItem copyItem = new MenuItem("复制");
                        MenuItem pasteItem = new MenuItem("粘贴");

                        // 剪切
                        cutItem.setOnAction(e -> {
                            clipboardFile = cell.getItem();//从老家抽离的 文件
                            originPlace = cell.getTreeItem();//老家( 当不是父母家 )
                            isCut = true;
                        });

                        // 复制操作
                        copyItem.setOnAction(e -> {
                            clipboardFile = cell.getItem();//同上
                            originPlace = null;//不告诉详细地址，怕顺着网线砍（在刷新上找不到老家，直接跳过删除）
                            isCut = false;//防止上一次操作是剪切
                        });

                        pasteItem.setOnAction(e -> {

                            File target = cell.getItem();  // 当前右键点击的目标文件夹

                            if (target.isDirectory() && clipboardFile != null) {
                                //新建文件放在target 的子目录下
                                File destFile = new File(target, clipboardFile.getName());
                                try {
                                    if (isCut) {
                                        if (clipboardFile.renameTo(destFile)) {
                                            showInfoDialog("剪切成功", "成功");
                                            clipboardFile = destFile;
                                            // 刷新界面
                                            updateTree(cell.getTreeItem(), clipboardFile, originPlace);

                                            clipboardFile = null;
                                            isCut = false;

                                        } else showInfoDialog("剪切失败", "失败");

                                    } else {
                                        copyDirectory(clipboardFile.toPath(), destFile.toPath());
                                        //刷新界面
                                        if (updateTree(cell.getTreeItem(), clipboardFile, originPlace)) {
                                            showInfoDialog("复制成功", "成功");
                                        } else {
                                            showErrDialog("复制失败，文件名已存在", "失败");
                                        }

                                    }
                                } catch (IOException ioException) {
                                    ioException.printStackTrace();
                                }
                            } else System.out.println("这可不是文件夹，不能复制");

                        });

                        // 根据是否有文件在剪贴板上决定是否启用粘贴选项
                        pasteItem.setDisable(clipboardFile == null);

                        Menu.getItems().addAll(cutItem, copyItem, pasteItem);
                        cell.setContextMenu(Menu);
                    }
                    //赋予双击打开功能
                    else if (!cell.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                        File file = treeView.getSelectionModel().getSelectedItem().getValue();
                        if (!file.isDirectory()) {
                            Alert conf = showConfDialog(file);
                            Optional<ButtonType> result = conf.showAndWait();
                            if (result.isPresent() && result.get() == ButtonType.OK)
                                openFileWithDefaultApp(file);
                        } else {
                            //文件夹是要展示子目录滴
                            if (checkSelect.getValue().equals("大图标查看")) {
                                picShowBt.fire();
                            } else if (checkSelect.getValue().equals("详细信息查看")) {
                                tailShowBt.fire();
                            }
                        }
                    }
                });

                return cell;
            });

            splitPane.getItems().set(1, new BorderPane()); // 加载 UI
            splitPane.getItems().set(0, treeView);
        }
    }

    public static void displaySearchResults(List<File> results) {
        //设置监听：
        //目的：支持文件的粘贴复制剪切功能
        ListView<String> resultList = new ListView<>();
        if (results != null && !results.isEmpty()) {
            for (File file : results) {
                resultList.getItems().add(file.getAbsolutePath());
            }
            //实现跨界面复制剪切粘贴
            resultList.setCellFactory(lv -> {
                ListCell<String> cell = new ListCell<>();
                cell.textProperty().bind(cell.itemProperty());

                cell.setOnMouseClicked(event -> {
                    if (!cell.isEmpty() && event.getButton() == MouseButton.SECONDARY) {
                        ContextMenu menu = new ContextMenu();
                        MenuItem cutItem = new MenuItem("剪切");
                        MenuItem copyItem = new MenuItem("复制");
                        MenuItem pasteItem = new MenuItem("粘贴");


                        cutItem.setOnAction(e -> {
                            clipboardFile = getFileByName(results, cell.getItem());
                            originPlace = treeView.getSelectionModel().getSelectedItem();//老家:存文件的节点
                            isCut = true;
                        });

                        copyItem.setOnAction(e -> {
                            clipboardFile = getFileByName(results, cell.getItem());
                            originPlace = null;
                            isCut = false;
                        });

                        pasteItem.setOnAction(e -> {
                            File target = results.getFirst().getParentFile();
                            if (target.isDirectory() && clipboardFile != null) {
                                File destFile = new File(target, clipboardFile.getName());
                                try {
                                    if (isCut) {
                                        if (clipboardFile.renameTo(destFile)) {
                                            showInfoDialog("剪切成功", "成功");
                                            clipboardFile = destFile;
                                            updateTree(treeView.getSelectionModel().getSelectedItem().getParent(), clipboardFile, originPlace);
                                            resultList.getItems().add(clipboardFile.getAbsolutePath());

                                            clipboardFile = null;
                                            isCut = false;

                                        } else showInfoDialog("剪切失败", "失败");

                                    } else {
                                        copyDirectory(clipboardFile.toPath(), destFile.toPath());
                                        if (updateTree(treeView.getSelectionModel().getSelectedItem().getParent(), clipboardFile, originPlace)) {
                                            showInfoDialog("复制成功", "成功");
                                            resultList.getItems().add(clipboardFile.getAbsolutePath());

                                        } else {
                                            showInfoDialog("复制失败,文件名已存在", "失败");
                                        }
                                    }

                                } catch (IOException ioException) {
                                    ioException.printStackTrace();
                                }
                            } else System.out.println("这可不是文件夹，不能复制");
                        });
                        pasteItem.setDisable(clipboardFile == null);
                        menu.getItems().addAll(cutItem, copyItem, pasteItem);
                        cell.setContextMenu(menu);
                    }
                });

                return cell;
            });

            resultList.setOnMouseClicked(event -> {
                String selectedFilePath = resultList.getSelectionModel().getSelectedItem();
                if (selectedFilePath != null) {
                    File selectedFile = new File(selectedFilePath);
                    highlight(selectedFile, treeView);
                }
            });

        } else {
            resultList.getItems().add("(该文件夹没有文件)");
        }

        splitPane.getItems().set(1, resultList);
    }

    // 关闭搜索进程
    @Override
    public void stop() {
        ExecutorOperation.shutdown(executorService, scheduler);
    }


}

