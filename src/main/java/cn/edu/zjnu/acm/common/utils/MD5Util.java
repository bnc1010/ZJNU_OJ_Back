package cn.edu.zjnu.acm.common.utils;

import java.security.MessageDigest;

public class MD5Util {
    private static String go(String dataStr){
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(dataStr.getBytes("UTF8"));
            byte s[] = m.digest();
            String result = "";
            for (int i = 0; i < s.length; i++) {
                result += Integer.toHexString((0x000000FF & s[i]) | 0xFFFFFF00).substring(6);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    public static String encrypt(String dataStr, String slat) {
        return go(dataStr + slat);
    }

    public static String encrypt(String dataStr){
        return go(dataStr);
    }
}
