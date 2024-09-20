package org.example.test1;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;
import java.awt.Graphics2D;

import static org.example.test1.Dialog.showConfDialog;
import static org.example.test1.ExecutorOperation.openFileWithDefaultApp;

public class imageOperation {
    public static String getImagePath(File file){
        String filetype = file.getName().toLowerCase();
        String path ;
        if(file.isDirectory()){
            path = "folderPic.png";
        }
        else if (filetype.endsWith(".bmp")) {
            path = "BMPpic.png";
        } else if (filetype.endsWith(".ppt") || filetype.endsWith(".pptx")) {
            path ="pptPic.png";
        } else if (filetype.endsWith(".txt")) {
            path ="txtPic.png";
        } else if (filetype.endsWith(".doc") || filetype.endsWith(".docx")) {
            path ="wordPic.png";
        } else if (filetype.endsWith(".xls") || filetype.endsWith(".xlsx")) {
            path ="xcelPic.png";
        } else if(filetype.endsWith(".rar")){
            path ="rarPic.png";
        }else if(filetype.endsWith(".zip")){
            path ="zipPic.png";
        } else {
            path ="otherPic.png";
        }
        return path;
    }
    public static ImageView createImageView(String imagePath) {
        Image image;
        try {
            image = new Image(imageOperation.class.getResourceAsStream("image/"+imagePath));
        } catch (NullPointerException e) {
            // 如果图片加载失败，抛出异常
            System.err.println("图片文件未找到: " + imagePath);
            image = null;
        }
        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(32);  // 设置大图标高度
        imageView.setFitWidth(32);   // 设置大图标宽度
        imageView.setPreserveRatio(true);  // 保持图片比例
        return imageView;
    }
    public static Label creatFileLabel(File file) {

        Label fileLabel = new Label();

        try{
            Icon swingIcon = FileSystemView.getFileSystemView().getSystemIcon(file);
            if (swingIcon != null) {
                java.awt.Image awtImage = ((ImageIcon) swingIcon).getImage();

                // 如果是多分辨率图标，选择其中一个进行处理
                if (awtImage instanceof java.awt.image.MultiResolutionImage) {
                    awtImage = ((java.awt.image.MultiResolutionImage) awtImage).getResolutionVariant(32, 32);
                }

                // 将 AWT Image 转换为 BufferedImage
                BufferedImage bufferedImage = new BufferedImage(
                        awtImage.getWidth(null), awtImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);

                Graphics2D g = bufferedImage.createGraphics();
                g.drawImage(awtImage, 0, 0, null);
                g.dispose();

                Image fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
                ImageView imageView = new ImageView(fxImage);
                fileLabel = new Label(file.getName(),imageView);
            }
        }catch (Exception e){
            ImageView imageView = createImageView(getImagePath(file));
            fileLabel = new Label(file.getName(),imageView);
        }
            if(!file.isDirectory()){
                fileLabel.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                        Alert conf = showConfDialog(file);
                        Optional<ButtonType> result = conf.showAndWait();
                        if(result.isPresent() && result.get() == ButtonType.OK)
                            openFileWithDefaultApp(file);
                    }
                });
            }
            return fileLabel;


    }

}
