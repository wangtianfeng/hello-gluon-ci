package com.wangtianfeng.learning.jdk.javafx.sign;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.layout.Region;
import org.apache.commons.codec.digest.DigestUtils;

import javax.net.ssl.*;

public class SignController {

    private static final String MODE_RELAY = "relay";

    // Open-API 模式默认值
    private static final String APP_ID_OPEN_API = "26cc7dde6da54475971c291e1b77fedc"; // test环境的测试账号
    private static final String APP_SECRET_OPEN_API = "d00e4e544f2344d9a213e4e24737351d";

    // Relay 模式默认值
    private static final String APP_ID_RELAY = "EWBj6rJE4xN7jXKLLlYBbbnvnvXpWnWWhGGe/DUeJdKk+GxvNMWfa6Ok3KOotjm9";
    private static final String URL_RELAY = "https://test-relay.netease.com/api/platform/oauth2/token";
    private static final String APP_SECRET_RELAY = "9mjtoOK3kO6afWMNvxG+kCdsmNBV6XcgpjMYdgPgPZVQQFTWPaBxOlsvNkuXMqMj";
    private static final String GRANT_TYPE_RELAY = "client_credentials";
    private static final Pattern pattern = Pattern.compile(
            "^.*\"access_token\"\\s*:\\s*\"([^\"]+)\".*?\"token_type\"\\s*:\\s*\"([^\"]+)\".*?$", Pattern.DOTALL);

    private ToggleGroup modeGroup;
    @FXML
    private TextField appIdField;
    @FXML
    private TextField appSecretField;
    @FXML
    private TextField urlField;
    @FXML
    private TextField grantTypeField;
    @FXML
    private Label urlLabel;
    @FXML
    private Label tokenLabel;
    @FXML
    private Label signLabel;
    @FXML
    private TextArea signArea;
    @FXML
    private RadioButton openApiRadio;
    @FXML
    private RadioButton relayRadio;

    @FXML
    public void initialize() {
        // 创建ToggleGroup并绑定到RadioButton
        modeGroup = new ToggleGroup();
        openApiRadio.setToggleGroup(modeGroup);
        relayRadio.setToggleGroup(modeGroup);

        // 监听模式切换
        modeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateMode(newVal.getUserData().toString());
            }
        });
        openApiRadio.setSelected(true);
        // 解决gluon打包报错Property "prefRowCount" does not exist or is read-only.
        signArea.setPrefRowCount(4);
    }

    private void updateMode(String mode) {
        boolean isRelay = MODE_RELAY.equals(mode);
        // 更新默认值
        if (isRelay) {
            urlField.setText(URL_RELAY);
            appIdField.setText(APP_ID_RELAY);
            appSecretField.setText(APP_SECRET_RELAY);
            grantTypeField.setText(GRANT_TYPE_RELAY);
            signLabel.setText("TOKEN");
        } else {
            appIdField.setText(APP_ID_OPEN_API);
            appSecretField.setText(APP_SECRET_OPEN_API);
            signLabel.setText("SIGN");
        }

        // 显示/隐藏token行
        urlLabel.setVisible(isRelay);
        urlField.setVisible(isRelay);
        tokenLabel.setVisible(isRelay);
        grantTypeField.setVisible(isRelay);

        signArea.clear();
    }

    @FXML
    private void handleCalculate() {
        String mode = modeGroup.getSelectedToggle().getUserData().toString();
        String appId = appIdField.getText();
        String appSecret = appSecretField.getText();
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            showAlert("提示", "请输入APP_ID或APP_SECRET", Alert.AlertType.ERROR);
            return;
        }
        String result;
        if (MODE_RELAY.equals(mode)) {
            String url = urlField.getText();
            String grantType = grantTypeField.getText();
            if (url == null || url.isBlank() || grantType == null || grantType.isBlank()) {
                showAlert("提示", "请输入URL或GRANT_TYPE", Alert.AlertType.ERROR);
                return;
            }
            try {
                result = sendPostRequest(url, appId, appSecret, grantType);
            } catch (Exception e) {
                e.printStackTrace();
                result = e.getMessage();
            }
        } else {
            result = getOpenApiSign(appId, appSecret);
        }

        signArea.setText(result);
    }

    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // 自动调整弹窗大小
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        alert.showAndWait();
    }

    @FXML
    private void handleClear() {
        urlField.clear();
        appIdField.clear();
        appSecretField.clear();
        grantTypeField.clear();
        signArea.clear();
    }

    @FXML
    private void handleReset() {
        String mode = modeGroup.getSelectedToggle().getUserData().toString();
        updateMode(mode);
        signArea.clear();
    }

    private String getOpenApiSign(String appId, String appSecret) {
        TreeMap<String, Object> paramMap = new TreeMap<>();
        paramMap.put("appId", appId);
        paramMap.put("appSecret", appSecret);
        Long timestamp = System.currentTimeMillis();
        String appNonce = UUID.randomUUID().toString();
        paramMap.put("timestamp", timestamp);
        paramMap.put("nonce", appNonce);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append('&');
        }
        sb.deleteCharAt(sb.length() - 1);
        String params = sb.toString();
        String currSign = DigestUtils.md5Hex(params);
        return "appId: " + appId + "\ntimestamp: " + timestamp + "\nsign: " + currSign + "\nnonce: " + appNonce;
    }

    private String sendPostRequest(String url, String appId, String appSecret, String grantType) throws Exception {
        String response = sendPostFormUrlEncodeRequest(url, appId, appSecret, grantType);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            String accessToken = matcher.group(1);
            String tokenType = matcher.group(2);
            return "Authorization: " + tokenType + " " + accessToken;
        }
        return null;
    }

    // 解决SSL问题
    private String sendPostFormUrlEncodeRequest(String url, String appId, String appSecret, String grantType) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return null; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        }}, new java.security.SecureRandom());
        HttpClient client = HttpClient.newBuilder().sslContext(sslContext).connectTimeout(Duration.ofSeconds(10)).build();
        String requestBody = String.format("client_id=%s&client_secret=%s&grant_type=%s"
                , URLEncoder.encode(appId, StandardCharsets.UTF_8)
                , URLEncoder.encode(appSecret, StandardCharsets.UTF_8)
                , URLEncoder.encode(grantType, StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private String sendPostFormDataRequest(String url, String appId, String appSecret, String grantType) throws IOException {
        // 生成随机边界字符串
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        disableSSLVerification();
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);

        // 发送请求数据
        try (OutputStream output = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"client_id\"\r\n\r\n");
            writer.append(appId).append("\r\n").flush();
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"client_secret\"\r\n\r\n");
            writer.append(appSecret).append("\r\n").flush();
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"grant_type\"\r\n\r\n");
            writer.append(grantType).append("\r\n").flush();
            // 结束边界
            writer.append("--").append(boundary).append("--\r\n");
            writer.flush();
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        return response.toString();
    }

    public static void disableSSLVerification() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {return null;}
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };

            // Install the all-trusting trust manager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}