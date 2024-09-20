package org.example.test1;

import javafx.scene.control.TreeView;

import java.io.File;

public class DriveTreeView {
    private File drive;
    private TreeView<File> treeView;

    public DriveTreeView(File drive, TreeView<File> treeView) {
        this.drive = drive;
        this.treeView = treeView;
    }

    public File getDrive() {
        return drive;
    }

    public TreeView<File> getTreeView() {
        return treeView;
    }
}

