package com.lhf.usercenter.common.utils;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class VerificationCodeUtil {
    private static final ConcurrentHashMap<String, String> codes = new ConcurrentHashMap<>();

    /**
     * 生成随机验证码
     *
     * @return 生成的验证码
     */
    public static String generateCode() {
        Random random = new Random();
        //存放生成验证码的数据，一共62位
        String identificationCode = "1234567890abcdefghijklmnopqrstuvwxwzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        //存放生成的验证码
        StringBuilder verifyCode = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            int a = random.nextInt(62);//随机生成0-61之间的数，提供索引位置
            verifyCode.append(identificationCode.charAt(a));//用get 和提供的索引找到相应位置的数据给变量
        }
        return verifyCode.toString();
    }

    /**
     * 存储验证码
     *
     * @param obj  验证方式（可以是邮箱、手机号）
     * @param code 验证码
     */
    public static void storeCode(String obj, String code) {
        codes.put(obj, code);
    }

    /**
     * 校验验证码
     *
     * @param obj  验证方式（可以是邮箱、手机号）
     * @param code 验证码
     * @return 校验是否成功
     */
    public static boolean verifyCode(String obj, String code) {
        return code.equals(codes.get(obj));
    }
}
