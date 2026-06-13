package com.xcodez.springaivoicecanvas.asr;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XunfeiSignUtil {

    private static final Logger log = LoggerFactory.getLogger(XunfeiSignUtil.class);
    private static final String HOST = "iat.xf-yun.com";
    private static final String PATH = "/v1";
    private static final String REQUEST_LINE = "GET " + PATH + " HTTP/1.1";
    private static final String WS_URL = "wss://" + HOST + PATH;

    public static String buildSignedUrl(String apiKey, String apiSecret) throws Exception {
        // 必须用英文 Locale，否则中文系统上月份会变成"五月"而不是"May"
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = sdf.format(new Date());

        log.info("讯飞签名 date={}", date);

        // 1. signature_origin
        String signatureOrigin = "host: " + HOST + "\ndate: " + date + "\n" + REQUEST_LINE;
        log.info("讯飞签名 signature_origin=\n{}", signatureOrigin);

        // 2. HMAC-SHA256
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] shaBytes = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));

        // 3. base64
        String signature = Base64.getEncoder().encodeToString(shaBytes);
        log.info("讯飞签名 signature={}", signature);

        // 4. authorization_origin
        String authOrigin = "api_key=\"" + apiKey + "\", algorithm=\"hmac-sha256\", " +
                "headers=\"host date request-line\", signature=\"" + signature + "\"";
        log.info("讯飞签名 authOrigin={}", authOrigin);

        // 5. authorization
        String authorization = Base64.getEncoder().encodeToString(authOrigin.getBytes(StandardCharsets.UTF_8));

        // Java URLEncoder 把空格编码成 +，讯飞需要 %20
        String url = WS_URL + "?authorization=" + encodeUrl(authorization) +
                "&date=" + encodeUrl(date) +
                "&host=" + encodeUrl(HOST);
        log.info("讯飞签名最终 URL (前80字符) = {}", url.substring(0, Math.min(80, url.length())) + "...");
        return url;
    }

    /** URLEncoder 替代：空格 → %20 而不是 + */
    private static String encodeUrl(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
