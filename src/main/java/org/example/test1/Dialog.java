package org.example.test1;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.File;

public class Dialog {
    public static void showInfoDialog(String tip,String title){
        Alert alert = new Alert(Alert.AlertType.INFORMATION, tip, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
    public static void showErrDialog(String tip,String title){
        Alert alert = new Alert(Alert.AlertType.ERROR, tip, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
    public static Alert showConfDialog(File file){
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认打开文件");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("你确定要打开文件：" + file.getName() + " 吗？");
        return confirmAlert;
    }
}
