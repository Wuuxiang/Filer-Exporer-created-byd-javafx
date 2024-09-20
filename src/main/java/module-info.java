module org.example.test1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires javafx.base;
    requires javafx.swing;


    opens org.example.test1 to javafx.fxml;
    exports org.example.test1;
}