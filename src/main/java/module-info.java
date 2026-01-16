module com.wangtianfeng.learning.jdk.javafx.sign {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.commons.codec;
    requires java.net.http;
    requires java.security.jgss;
    requires jdk.crypto.ec;
    provides javafx.application.Preloader
            with com.wangtianfeng.learning.jdk.javafx.sign.AppPreloader;

    opens com.wangtianfeng.learning.jdk.javafx.sign to javafx.fxml;
    exports com.wangtianfeng.learning.jdk.javafx.sign;
}