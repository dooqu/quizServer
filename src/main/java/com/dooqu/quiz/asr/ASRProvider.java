package com.dooqu.quiz.asr;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import okhttp3.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 语音听写流式 WebAPI 接口调用示例 接口文档（必看）：https://doc.xfyun.cn/rest_api/语音听写（流式版）.html
 * webapi 听写服务参考帖子（必看）：http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=38947&extra=
 * 语音听写流式WebAPI 服务，热词使用方式：登陆开放平台https://www.xfyun.cn/后，找到控制台--我的应用---语音听写---个性化热词，上传热词
 * 注意：热词只能在识别的时候会增加热词的识别权重，需要注意的是增加相应词条的识别率，但并不是绝对的，具体效果以您测试为准。
 * 错误码链接：https://www.xfyun.cn/document/error-code （code返回错误码时必看）
 * 语音听写流式WebAPI 服务，方言或小语种试用方法：登陆开放平台https://www.xfyun.cn/后，在控制台--语音听写（流式）--方言/语种处添加
 * 添加后会显示该方言/语种的参数值
 *
 * @author iflytek
 */

public class ASRProvider extends WebSocketListener {
    private static final String hostUrl = "https://iat-api.xfyun.cn/v2/iat"; //中英文，http url 不支持解析 ws/wss schema
    // private static final String hostUrl = "https://iat-niche-api.xfyun.cn/v2/iat";//小语种
    private static final String apiKey = "19930b368545a7263ff7cfa5725b96f7"; //在控制台-我的应用-语音听写（流式版）获取
    private static final String apiSecret = "e1487fbc534e8d29fbba98ee23055b87"; //在控制台-我的应用-语音听写（流式版）获取
    private static final String appid = "5e65f0f0"; //在控制台-我的应用获取
    public static final int StatusFirstFrame = 0;
    public static final int StatusContinueFrame = 1;
    public static final int StatusLastFrame = 2;
    public static final Gson json = new Gson();
    Decoder decoder = new Decoder();
    WebSocket webSocket;
    public boolean connected;
    public volatile boolean firstFrame;

    // 开始时间
    private static Date dateBegin = new Date();
    // 结束时间
    private static Date dateEnd = new Date();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");

    public void newSession() {
        if (webSocket != null) {
            webSocket.close(1001, "server close.");
        }
        String authUrl = null;
        try {
            authUrl = ASRProvider.getAuthUrl(hostUrl, apiKey, apiSecret);
        } catch (Exception ex) {
        }
        firstFrame = true;
        OkHttpClient client = new OkHttpClient.Builder().build();
        //将url中的 schema http://和https://分别替换为ws:// 和 wss://
        String url = authUrl.toString().replace("http://", "ws://").replace("https://", "wss://");
        //System.out.println(url);
        Request request = new Request.Builder().url(url).build();
        webSocket = client.newWebSocket(request, this);
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        super.onOpen(webSocket, response);
        this.webSocket = webSocket;
        connected = true;
        firstFrame = true;
        onASRReady();
    }

    protected void onASRReady() {
        System.out.println("ASR start");
    }

    protected void onASRStop() {
        System.out.println("ASR stop");
    }

    public boolean isConnected() {
        return webSocket != null && this.connected;
    }


    public void sendFirstPCMFrame(byte[] buffer, int length) {
        if (isConnected() == false) {
            return;
        }
        System.out.println("讯飞接口第一帧数据发送");
        JsonObject frame = new JsonObject();
        JsonObject business = new JsonObject();  //第一帧必须发送
        JsonObject common = new JsonObject();  //第一帧必须发送
        JsonObject data = new JsonObject();  //每一帧都要发送

        // 填充common
        common.addProperty("app_id", appid);
        //填充business
        business.addProperty("language", "zh_cn");
        business.addProperty("domain", "iat");
        business.addProperty("accent", "mandarin");//中文方言请在控制台添加试用，添加后即展示相应参数值
        //business.addProperty("nunum", 0);
        //business.addProperty("ptt", 0);//标点符号
        //business.addProperty("rlang", "zh-hk"); // zh-cn :简体中文（默认值）zh-hk :繁体香港(若未授权不生效，在控制台可免费开通)
        //business.addProperty("vinfo", 1);
        business.addProperty("dwa", "wpgs");//动态修正(若未授权不生效，在控制台可免费开通)
        //business.addProperty("nbest", 5);// 句子多候选(若未授权不生效，在控制台可免费开通)
        //business.addProperty("wbest", 3);// 词级多候选(若未授权不生效，在控制台可免费开通)
        //填充data
        data.addProperty("status", StatusFirstFrame);
        data.addProperty("format", "audio/L16;rate=16000");
        data.addProperty("encoding", "raw");
        data.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, length)));
        //填充frame
        frame.add("common", common);
        frame.add("business", business);
        frame.add("data", data);
        webSocket.send(frame.toString());
    }

    public void sendPCMFrame(byte[] buffer, int size) {
        if (firstFrame) {
            firstFrame = false;
            sendFirstPCMFrame(buffer, size);
        } else {
            sendIntermediatePCMFrame(buffer, size);
        }
    }


    public void sendIntermediatePCMFrame(byte[] buffer, int size) {
        if (isConnected() == false) {
            return;
        }
        JsonObject frame1 = new JsonObject();
        JsonObject data1 = new JsonObject();
        data1.addProperty("status", StatusContinueFrame);
        data1.addProperty("format", "audio/L16;rate=16000");
        data1.addProperty("encoding", "raw");
        data1.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, size)));
        frame1.add("data", data1);
        webSocket.send(frame1.toString());
    }


    public void sendVadFrame() {
        if (isConnected() == false) {
            return;
        }
        JsonObject frame2 = new JsonObject();
        JsonObject data2 = new JsonObject();
        data2.addProperty("status", StatusLastFrame);
        data2.addProperty("audio", "");
        data2.addProperty("format", "audio/L16;rate=16000");
        data2.addProperty("encoding", "raw");
        frame2.add("data", data2);
        webSocket.send(frame2.toString());
        System.out.println("sendlast");
    }


    @Override
    public void onMessage(WebSocket webSocket, String text) {
        super.onMessage(webSocket, text);
        System.out.println(text);

        ResponseData resp = json.fromJson(text, ResponseData.class);
        if (resp != null) {
            if (resp.getCode() != 0) {
                System.out.println("code=>" + resp.getCode() + " error=>" + resp.getMessage() + " sid=" + resp.getSid());
                System.out.println("错误码查询链接：https://www.xfyun.cn/document/error-code");
                return;
            }
            if (resp.getData() != null) {
                if (resp.getData().getResult() != null) {
                    Text te = resp.getData().getResult().getText();
                    //System.out.println(te.toString());
                    try {
                        decoder.decode(te);
                        System.out.println("中间识别结果 ==》" + decoder.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (resp.getData().getStatus() == 2) {
                    // todo  resp.data.status ==2 说明数据全部返回完毕，可以关闭连接，释放资源
                    System.out.println("session end ");
                    dateEnd = new Date();
                    System.out.println(sdf.format(dateBegin) + "开始");
                    System.out.println(sdf.format(dateEnd) + "结束");
                    System.out.println("耗时:" + (dateEnd.getTime() - dateBegin.getTime()) + "ms");
                    System.out.println("最终识别结果 ==》" + decoder.toString());
                    System.out.println("本次识别sid ==》" + resp.getSid());
                    onASRResult(true, decoder.toString());
                    decoder.discard();
                    connected = false;
                    //webSocket.close(1006, "");
                } else {
                    // todo 根据返回的数据处理
                }
            }
        }
    }


    protected void onASRResult(boolean success, String resultString) {
        System.out.println("onResult:" + resultString);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        connected = false;
        super.onFailure(webSocket, t, response);
        System.out.println("onFailure");
        try {
            if (null != response) {
                int code = response.code();
                System.out.println("onFailure code:" + code);
                System.out.println("onFailure body:" + response.body().string());
                if (101 != code) {
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        onASRResult(false, null);
    }


    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        super.onClosed(webSocket, code, reason);
        connected = false;
        System.out.println("onClosed" + code);
    }


    public void close(int code, String message) {
        if (webSocket != null) {
            webSocket.close(code, message);
        }
    }

    public static void main1(String[] args) throws Exception {
        // 构建鉴权url
        String authUrl = getAuthUrl(hostUrl, apiKey, apiSecret);
        OkHttpClient client = new OkHttpClient.Builder().build();
        //将url中的 schema http://和https://分别替换为ws:// 和 wss://
        String url = authUrl.toString().replace("http://", "ws://").replace("https://", "wss://");
        //System.out.println(url);
        Request request = new Request.Builder().url(url).build();
        // System.out.println(client.newCall(request).execute());
        //System.out.println("url===>" + url);
        WebSocket webSocket = client.newWebSocket(request, new ASRProvider());
    }

    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        StringBuilder builder = new StringBuilder("host: ").append(url.getHost()).append("\n").//
                append("date: ").append(date).append("\n").//
                append("GET ").append(url.getPath()).append(" HTTP/1.1");
        //System.out.println(builder);
        Charset charset = Charset.forName("UTF-8");
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
        String sha = Base64.getEncoder().encodeToString(hexDigits);

        //System.out.println(sha);
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
        //System.out.println(authorization);
        HttpUrl httpUrl = HttpUrl.parse("https://" + url.getHost() + url.getPath()).newBuilder().//
                addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(charset))).//
                addQueryParameter("date", date).//
                addQueryParameter("host", url.getHost()).//
                build();
        return httpUrl.toString();
    }

    public static class ResponseData {
        private int code;
        private String message;
        private String sid;
        private Data data;

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return this.message;
        }

        public String getSid() {
            return sid;
        }

        public Data getData() {
            return data;
        }
    }

    public static class Data {
        private int status;
        private Result result;

        public int getStatus() {
            return status;
        }

        public Result getResult() {
            return result;
        }
    }

    public static class Result {
        int bg;
        int ed;
        String pgs;
        int[] rg;
        int sn;
        Ws[] ws;
        boolean ls;
        JsonObject vad;

        public Text getText() {
            Text text = new Text();
            StringBuilder sb = new StringBuilder();
            for (Ws ws : this.ws) {
                sb.append(ws.cw[0].w);
            }
            text.sn = this.sn;
            text.text = sb.toString();
            text.sn = this.sn;
            text.rg = this.rg;
            text.pgs = this.pgs;
            text.bg = this.bg;
            text.ed = this.ed;
            text.ls = this.ls;
            text.vad = this.vad == null ? null : this.vad;
            return text;
        }
    }

    public static class Ws {
        Cw[] cw;
        int bg;
        int ed;
    }

    public static class Cw {
        int sc;
        String w;
    }

    public static class Text {
        int sn;
        int bg;
        int ed;
        String text;
        String pgs;
        int[] rg;
        boolean deleted;
        boolean ls;
        JsonObject vad;

        @Override
        public String toString() {
            return "Text{" +
                    "bg=" + bg +
                    ", ed=" + ed +
                    ", ls=" + ls +
                    ", sn=" + sn +
                    ", text='" + text + '\'' +
                    ", pgs=" + pgs +
                    ", rg=" + Arrays.toString(rg) +
                    ", deleted=" + deleted +
                    ", vad=" + (vad == null ? "null" : vad.getAsJsonArray("ws").toString()) +
                    '}';
        }
    }

    //解析返回数据，仅供参考
    public static class Decoder {
        private Text[] texts;
        private int defc = 10;

        public Decoder() {
            this.texts = new Text[this.defc];
        }

        public synchronized void decode(Text text) {
            if (text.sn >= this.defc) {
                this.resize();
            }
            if ("rpl".equals(text.pgs)) {
                for (int i = text.rg[0]; i <= text.rg[1]; i++) {
                    this.texts[i].deleted = true;
                }
            }
            this.texts[text.sn] = text;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Text t : this.texts) {
                if (t != null && !t.deleted) {
                    sb.append(t.text);
                }
            }
            return sb.toString();
        }

        public void resize() {
            int oc = this.defc;
            this.defc <<= 1;
            Text[] old = this.texts;
            this.texts = new Text[this.defc];
            for (int i = 0; i < oc; i++) {
                this.texts[i] = old[i];
            }
        }

        public void discard() {
            for (int i = 0; i < this.texts.length; i++) {
                this.texts[i] = null;
            }
        }
    }
}