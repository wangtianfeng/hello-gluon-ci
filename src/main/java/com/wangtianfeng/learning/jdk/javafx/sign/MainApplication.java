package com.wangtianfeng.learning.jdk.javafx.sign;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        System.out.println("start  : " + System.nanoTime());
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("sign.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 730, 400);
        // 解决gluon打包报错Unable to coerce resource:/com/wangtianfeng/learning/jdk/javafx/sign/style.css to class java.lang.String.
        // gluon官方样例代码也是这样写的
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        stage.setTitle("签名工具");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.setOnShown(e -> {
//            stage.setAlwaysOnTop(true);  // 设置窗口始终在最前面
            stage.toFront();
            stage.requestFocus();
        });
        System.out.println("show   : " + System.nanoTime());
        stage.show();
        stage.toFront();
        stage.requestFocus();
        System.out.println("running: " + System.nanoTime());
    }

    public static void main(String[] args) {
        // System.setProperty("javafx.preloader","com.wangtianfeng.learning.jdk.javafx.sign.AppPreloader");
        System.out.println("run    : " + System.nanoTime());
        launch();
        System.out.println("stop   : " + System.nanoTime());
    }
}