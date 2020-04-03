package com.dooqu.quiz.skills;

import com.dooqu.quiz.common.Client;
import com.dooqu.quiz.common.Skill;
import com.dooqu.quiz.utils.StreamUtils;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.sound.sampled.AudioInputStream;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class XiaoiceTTSSkill extends Skill {
    private final static String URL_PATH = "http://xylink.aic.msxiaobing.com/api/platform/Reply";
    private final static String KEY_SUBSCRIPTION = "bc2c2003ad7342d7afd7d3c48f28abad";
    private final static String MSG_ID = "f5ff4f16fb90d07eb9475b5d9b582967ad09e3a7b875a62a26f02ffec1b37c2dff4ab5684fc620ee";
    private final static String TIMESTAMP = "300";

    String messageText;
    OkHttpClient client;
    Call requestCall;

    public XiaoiceTTSSkill(String messageText, Client... sessions) {
        super(sessions);
        this.messageText = messageText;
    }


    @Override
    protected boolean onStart() {
        fetchJsonResponseFromXiaoice();
        return true;
    }

    @Override
    protected void onStop() {
        if (requestCall != null) {
            requestCall.cancel();
        }
    }

    protected boolean fetchJsonResponseFromXiaoice() {
        String jsonData = null;
        try {
            jsonData = createPostString(messageText, "20");
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        String signature = SHA512(jsonData + KEY_SUBSCRIPTION + TIMESTAMP);
        client = new OkHttpClient();
        Request apiRequest = new Request.Builder().url(URL_PATH).header("subscription-key", KEY_SUBSCRIPTION).header("timestamp", "300").header("signature", signature).post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonData)).build();
        requestCall = client.newCall(apiRequest);
        requestCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                System.out.println(e.toString());
                stopRunning();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String string = response.body().string();
                    JSONArray jsonArray = null;
                    try {
                        jsonArray = new JSONArray(string);
                        JSONObject o1 = jsonArray.getJSONObject(0);
                        String url = o1.getJSONObject("content").getString("audioUrl");
                        System.out.println(url);
                        downloadVoiceAudio(url);
                        return;
                    } catch (JSONException ex) {
                    }
                }
                //如果下载失败，关闭服务
                stopRunning();
            }
        });
        return true;
    }

    protected void downloadVoiceAudio(String audioUrl) {
        Request downloadRequest = new Request.Builder().url(audioUrl).build();
        requestCall = client.newCall(downloadRequest);
        requestCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                System.out.println("download audio failed:" + e.toString());
                stopRunning();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                InputStream downloadInputStream = response.body().byteStream();
                setContentSize(false, response.body().contentLength());
                try {
                    byte[] buffer = new byte[160 * 8];
                    int perBytesReaded = 0;
                    int bytesTotal = 0;
                    do {
                        perBytesReaded = downloadInputStream.read(buffer, 0, buffer.length);
                        if (perBytesReaded > 0) {
                            bytesTotal += perBytesReaded;
                            produceSkillData(buffer, 0, perBytesReaded);
                        }
                    } while (isRunning() && perBytesReaded != -1);
                    setContentSize(true, bytesTotal);
                    System.out.println("All download data have been writed to the piped buffer");
                } catch (IOException ex) {
                } finally {
                    StreamUtils.safeClose(downloadInputStream);
                }
            }
        });
    }


    private static String SHA512(final String strText) {
        return SHA(strText, "SHA-512");
    }

    private static String SHA(final String strText, final String strType) {
        String strResult = null;

        if (strText != null && strText.length() > 0) {
            try {
                MessageDigest messageDigest = MessageDigest.getInstance(strType);
                messageDigest.update(strText.getBytes());
                byte byteBuffer[] = messageDigest.digest();
                StringBuffer strHexString = new StringBuffer();
                for (int i = 0; i < byteBuffer.length; i++) {
                    String hex = Integer.toHexString(0xff & byteBuffer[i]);
                    if (hex.length() == 1) {
                        strHexString.append('0');
                    }
                    strHexString.append(hex);
                }
                strResult = strHexString.toString();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return strResult;
    }


    private String createPostString(String text, String speedStr) throws JSONException {
        JSONObject itemObject = new JSONObject();
        itemObject.put("senderId", "11111");
        itemObject.put("senderNickname", "Dilly");
        JSONObject contentObject = new JSONObject();

        contentObject.put("text", text);

        JSONObject medataObject = new JSONObject();
        medataObject.put("ReadContent", "true");
        medataObject.put("SpeechRate", speedStr);
        contentObject.put("Metadata", medataObject);

        itemObject.put("content", contentObject);
        itemObject.put("msgId", MSG_ID);
        itemObject.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return itemObject.toString();
    }
}
