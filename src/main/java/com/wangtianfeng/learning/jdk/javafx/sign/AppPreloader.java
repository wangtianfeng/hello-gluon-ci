package com.wangtianfeng.learning.jdk.javafx.sign;

import javafx.application.Preloader;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class AppPreloader extends Preloader {

    private Stage splashStage;

    @Override
    public void start(Stage stage) {
        this.splashStage = stage;
        StackPane root = new StackPane(new ImageView(new Image("sign.png")), new ProgressIndicator());
        stage.setTitle("签名工具");
        stage.setScene(new Scene(root, 600, 400));
        stage.centerOnScreen();
        stage.setOnShown(e -> {
            stage.setAlwaysOnTop(true);
            stage.toFront();
            stage.requestFocus();
        });
        stage.show();
    }

    @Override
    public void handleProgressNotification(ProgressNotification info) {
        // 更新进度（0.0-1.0）
        ((ProgressIndicator) splashStage.getScene().getRoot().getChildrenUnmodifiable().get(1))
                .setProgress(info.getProgress());
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification info) {
        if (info.getType() == StateChangeNotification.Type.BEFORE_START) {
            splashStage.hide();
        }
    }
}