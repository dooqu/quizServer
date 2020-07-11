package com.dooqu.quiz.asr;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ASRIntent {
    private static class ASRResult {
        public String indexString;
        public String matchString;

        public ASRResult(String indexString, String matchString) {
            this.indexString = indexString;
            this.matchString = matchString;
        }
    }
    static Pattern[] patterns = {Pattern.compile("(选择|选|选项|第)(?<indexString>[1-5a-eA-E一二三四五])(个|项)?")
            , Pattern.compile("(?<indexString>最后一个)"), Pattern.compile("(?<indexString>[1-5a-eA-E一二三四五])")};
    static HashMap<String, Integer> indexMaps = new HashMap<>();

    static {
        indexMaps.put("1", 0);
        indexMaps.put("2", 1);
        indexMaps.put("3", 2);
        indexMaps.put("4", 3);
        indexMaps.put("5", 4);
        indexMaps.put("a", 0);
        indexMaps.put("b", 1);
        indexMaps.put("c", 2);
        indexMaps.put("d", 3);
        indexMaps.put("e", 4);
        indexMaps.put("A", 0);
        indexMaps.put("B", 1);
        indexMaps.put("C", 2);
        indexMaps.put("D", 3);
        indexMaps.put("E", 4);
        indexMaps.put("一", 0);
        indexMaps.put("二", 1);
        indexMaps.put("三", 2);
        indexMaps.put("四", 3);
        indexMaps.put("五", 4);
        indexMaps.put("最后一个", 4);
        indexMaps.put("倒数第一个", 4);
        indexMaps.put("倒数第二个", 3);
        indexMaps.put("倒数第三个", 2);
        indexMaps.put("倒数第四个", 1);
        indexMaps.put("倒数第一个", 0);
    }

    protected int action;
    protected String actionString;
    protected String matchedString;

    public ASRIntent() {
    }

    public int getAction() {
        return action;
    }

    protected void setAction(int code) {
        action = code;
    }

    public String getActionString() {
        return this.actionString;
    }

    protected void setActionString(String actionString) {
        this.actionString = actionString;
    }

    public String getMatchedString() {
        return matchedString;
    }

    protected void setMatchedString (String matchedString){
        this.matchedString = matchedString;
    }

    public static ASRIntent parse(String text) {
        ASRIntent intent = new ASRIntent();
        intent.setAction(-1);
        intent.setActionString(text);
        ASRResult asrResult = matchASRStringIndex(text);
        if(asrResult != null) {
            intent.setAction(matchASRIntIndex(asrResult));
            intent.setMatchedString(asrResult.matchString);
        }
        return intent;
    }

    protected static ASRResult matchASRStringIndex(String userInput) {
        //Pattern pattern = Pattern.compile("(选择|选)?(第)?(?<index>[1-5a-eA-E一二三四五]{1})(个|选项)?");
        String indexString = null;
        String matchString = null;
        for (int i = 0; i < patterns.length; i++) {
            Matcher matcher = patterns[i].matcher(userInput);
            while (matcher.find()) {
                indexString = matcher.group("indexString");
                matchString = matcher.group(0);
                //System.out.println("index=" + matcher.group("indexString"));
            }
            if (indexString != null) {
                return new ASRResult(indexString, matchString);
            }
        }
        return null;
    }

    protected static int matchASRIntIndex(ASRResult asrResult) {
        if (asrResult == null) {
            return -1;
        }
        if (indexMaps.containsKey(asrResult.indexString)) {
            return indexMaps.get(asrResult.indexString);
        }
        return -1;
    }
}
