package com.clozeblur.core.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/**
 * @author clozeblur
 * <html>项目启动时,通过http请求与配置中心服务端交互,获取最新配置</html>
 */
class RemoteConfig {

    private static final Logger logger = LoggerFactory.getLogger(RemoteConfig.class);

    private String env;

    private String prePath;

    private int connectTimeout;

    private int readTimeout;

    RemoteConfig(String prePath){
        this.prePath = prePath;
    }

    RemoteConfig initConfig(String env,int connectTimeout,int readTimeout){
        this.env = env;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        return this;
    }

    String getConfigData(String uri, String configBranch) throws Exception {
        HttpURLConnection connection = null;
        String configUri = uri + "/" + env + "/" + configBranch;
        try {
            URL url = new URL(prePath+configUri);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(readTimeout);
            connection.setConnectTimeout(connectTimeout);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            //支持gzip
            connection.setRequestProperty("Accept-Encoding", "gzip");
            // 指定类型为二进制流
            // connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.connect();
            boolean isGziped="gzip".equals(connection.getContentEncoding());
            // 先写，后读
            try (InputStream is = isGziped?new GZIPInputStream(connection.getInputStream()):connection.getInputStream();
                 ByteArrayOutputStream bao = new ByteArrayOutputStream(2048 * 3)) {
                int len;
                byte[] buffer = new byte[2048];
                while ((len = is.read(buffer)) > 0) {
                    bao.write(buffer, 0, len);
                }
                String jsonData = new String(bao.toByteArray(), StandardCharsets.UTF_8);
                // 将获得的配置信息json字符串写入一个本地暂存properties文件中
                String filePath = System.getProperty("java.io.tmpdir") +
                        "/config-temp" + "/" + uri + "-" + env + ".properties";
                try {
                    FileWriter fileWriter = new FileWriter(filePath);
                    PrintWriter out = new PrintWriter(fileWriter);
                    out.write(jsonData);
                    out.close();
                    fileWriter.close();
                } catch (IOException e) {
                    logger.error("缓存配置文件写入失败: {}", configUri);
                }
                // 直接用utf-8或者根据content-type推断
                return jsonData;
            }
        } catch (Exception e) {
            logger.warn("config-server获取数据失败,开始读取本地缓存配置");
            String suffix = uri + "-" + env + ".properties";
            String propertiesPath = System.getProperty("java.io.tmpdir") + "/config-temp" + "/" + suffix;
            File propertiesFile = new File(propertiesPath);
            if (propertiesFile.exists()){
                String jsonData = "";
                FileReader fileReader = new FileReader(propertiesPath);
                char[] cbuf = new char[64];
                int hasRead;
                while((hasRead = fileReader.read(cbuf)) > -1) {
                    jsonData += new String(cbuf, 0, hasRead);
                }
                return jsonData;
            } else {
                logger.error("本地无此缓存配置文件: {}", suffix);
                throw new FileNotFoundException("本地无此缓存配置文件: " + suffix);
            }
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    logger.error("http连接断开错误");
                }
            }
        }
    }

}
