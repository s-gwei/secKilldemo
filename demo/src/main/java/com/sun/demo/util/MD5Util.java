package com.sun.demo.util;

import com.sun.demo.config.RedisConfig;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.core.RedisTemplate;
import redis.clients.jedis.Jedis;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MD5Util {
    public static String md5(String src){

        return DigestUtils.md5Hex(src);
    }

    private static final String salt="1a2b3c4d";

    //第一次加密，对密码前后加字符
    public static String inputPassToFromPass(String inputPass){
        String str = "" +salt.charAt(0)+salt.charAt(2)+inputPass+salt.charAt(5)+salt.charAt(4);
        return md5(str);
    }

    //第二次加密
    public static String formPassToDBPass(String formPass,String salt){
        String str = "" +salt.charAt(0)+salt.charAt(2)+formPass+salt.charAt(5)+salt.charAt(4);
        return md5(str);
    }

    public static String inputPassToDBPass(String inputPass,String salt){
        String fromPass = inputPassToFromPass(inputPass);
        String dbPass = formPassToDBPass(fromPass, salt);
        return dbPass;
    }

    public static void main(String[] args) {
        // d3b1294a61a07da9b49b6e22b2cbd7f9
        System.out.println(inputPassToFromPass("123456"));
        System.out.println(formPassToDBPass("d3b1294a61a07da9b49b6e22b2cbd7f9","1a2b3c4d"));
//        System.out.println(formPassToDBPass(inputPassToFromPass("123456"),salt));
        System.out.println(inputPassToDBPass("123456","1a2b3c4d"));
        getMd5("18071739944",1);

    }

    public static  String getMd5(String userid, Integer id) {
        String md5;
        try {
            Random r = new Random();
            StringBuilder sb = new StringBuilder(16);
            sb.append(r.nextInt(99999999)).append(r.nextInt(99999999));
            int len = sb.length();
            if (len < 16) {
                for (int i = 0; i < 16 - len; i++) {
                    sb.append("0");
                }
            }
            String salt = sb.toString();
            md5 = MD5Util.formPassToDBPass(userid, salt);
            Jedis jedis = new Jedis("192.168.200.251", 6379);
            jedis.set(userid +""+ id, md5);
        } catch (Exception e) {
            e.printStackTrace();
            return "获取md5失败: " + e.getMessage();
        }
        return "获取md5信息为: " + md5;
    }
}
