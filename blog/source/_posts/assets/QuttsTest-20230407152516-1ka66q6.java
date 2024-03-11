package ai.guiji.live.manager;

import ai.guiji.live.manager.entity.CustomTTSReq;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class QuttsTest {
    static List<String> symbol = Arrays.asList("；", ";", "!", "！", "?", "？", ",", "，", ".", "。", "、", ":");
    static ExecutorService executorService = Executors.newFixedThreadPool(100);
    static long endDate = DateUtil.tomorrow().toTimestamp().getTime();
    static List<CustomTTSReq> ttsSpeaker = new ArrayList<>();

    static {
        ttsSpeaker.add(new CustomTTSReq("263", "", 7, -4, 0, false));
        ttsSpeaker.add(new CustomTTSReq("297", "", 6, 10, 1, false));
        ttsSpeaker.add(new CustomTTSReq("235", "", 4, 9, 1, false));
        ttsSpeaker.add(new CustomTTSReq("289", "", 20, 6, 1, false));
        ttsSpeaker.add(new CustomTTSReq("290", "", 20, 6, 1, false));
//        卖货小姐姐 263  语速7  音量-4 语调0
//        瑟雨 297   语速6  音量10  语调1
//        知性学姐 235 语速4  音量9  语调1
//        家传 289 语速20  音量6 语调1
//        潮东 290 语速20  音量6 语调1
    }

    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {
            executorService.submit(getRunnable());
        }
    }


    private static Runnable getRunnable() {
        return () -> {
            Stopwatch stopwatch = Stopwatch.createStarted();
            String str = readJsonFile("manager/ttsText30.json");

            JSONArray jsonArray = JSON.parseArray(str);

            List<JSONObject> jsonObjects = new ArrayList<>();
            CustomTTSReq tts = ttsSpeaker.get(RandomUtil.randomInt(5));

            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                JSONObject jsonObject1 = new JSONObject();
//                jsonObject1.fluentPut("reqId", jsonObject.getString("reqId"));
                jsonObject1.fluentPut("reqId", IdUtil.fastSimpleUUID());
                JSONObject customTTSReq = jsonObject.getJSONObject("customTTSReq");
//                customTTSReq.put("speedRate", tts.getSpeedRate());
//                customTTSReq.put("ttsSpeaker", tts.getTtsSpeaker());
//                customTTSReq.put("volume", tts.getVolume());
//                customTTSReq.put("pitch", tts.getPitch());
                List<String> stringList = Arrays.asList(customTTSReq.getString("text").split(""));
                Collections.shuffle(stringList);
                String join = CollectionUtil.join(stringList, "");
                customTTSReq.fluentPut("text", join);
                while (symbol.contains(customTTSReq.getString("text").substring(0, 1))) {
                    customTTSReq.fluentPut("text", customTTSReq.getString("text").substring(1));
                }
                jsonObject1.fluentPut("customTTSReq", customTTSReq);
                jsonObject1.fluentPut("engineTypeCode", jsonObject.getInteger("engineTypeCode"));
                jsonObject1.fluentPut("audioUrl", jsonObject.getString("audioUrl"));
                jsonObjects.add(jsonObject1);
            }
            String body = HttpRequest.post("http://localhost:8089/live-manager/material/bathAsyncSynthesisAndChangeVoice2?token=43eb1e3f0c3b4715af4e027f84f7da15")
                    .body(JSONObject.toJSONString(jsonObjects))
                    .execute().body();
//            String body = HttpRequest.post("https://meta.guiji.ai/live-manager/material/bathAsyncSynthesisAndChangeVoice?token=43eb1e3f0c3b4715af4e027f84f7da15")
//                    .body(JSONObject.toJSONString(jsonObjects))
//                    .execute().body();
//            String body = HttpRequest.post("https://prevshow.guiji.ai/live-manager/material/bathAsyncSynthesisAndChangeVoice?token=43eb1e3f0c3b4715af4e027f84f7da15")
//                    .body(JSONObject.toJSONString(jsonObjects))
//                    .execute().body();
            JSONObject jsonObject = JSONObject.parseObject(body);
            if (jsonObject.getBoolean("success") == false) {
                System.err.println(jsonObject);
                return;
            }
            String taskId = jsonObject.getString("data");
            int res = 0;
            while (res == 0) {
                int j = 0;
                String body1 = HttpRequest.get("http://localhost:8089/live-manager/material/bathAsyncQueryAndChangeVoice?token=43eb1e3f0c3b4715af4e027f84f7da15&taskId="+jsonObject.getString("data"))
                        .execute().body();
//            String body1 = HttpRequest.get("https://meta.guiji.ai/live-manager/material/bathAsyncQueryAndChangeVoice?token=43eb1e3f0c3b4715af4e027f84f7da15&taskId="+jsonObject.getString("data"))
//                    .execute().body();


//                String body1 = HttpRequest.get("https://prevshow.guiji.ai/live-manager/material/bathAsyncQueryAndChangeVoice??token=43eb1e3f0c3b4715af4e027f84f7da15&taskId=" + taskId)
//                        .execute().body();
                JSONObject jsonObject1 = JSONObject.parseObject(body1);
                JSONArray data = jsonObject1.getJSONArray("data");
                for (int i = 0; i < data.size(); i++) {
                    JSONObject jsonObject2 = data.getJSONObject(i);
                    if (!Strings.isNullOrEmpty(jsonObject2.getString("url")) || !Strings.isNullOrEmpty(jsonObject2.getString("reason"))) {
                        j++;
                    }
                }
                if (j == data.size()) {
                    res = 1;
                }
                ThreadUtil.safeSleep(5000);
                System.out.println("taskID:" + taskId + ",合成查询中,大小" + data.size() + "有结果：" + j);
            }
            System.out.println("" + "合成成功,耗时：" + stopwatch.elapsed(TimeUnit.MILLISECONDS));

            if (System.currentTimeMillis() < endDate) {
                System.out.println("再提交一个任务");
                executorService.submit(getRunnable());
            }
        };

    }

    private static String readJsonFile(String fileName) {
        String jsonStr = "";
        try {
            File jsonFile = new File(fileName);
            FileReader fileReader = new FileReader(jsonFile);
            Reader reader = new InputStreamReader(new FileInputStream(jsonFile), "utf-8");
            int ch = 0;
            StringBuffer sb = new StringBuffer();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            jsonStr = sb.toString();
            return jsonStr;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
