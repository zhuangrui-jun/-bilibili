package com.zr.bili.utils;

import cn.hutool.core.util.RandomUtil;

/**
 * 验证码工具类
 */
public class CodeUtil {
    // 生成6位数字验证码
    public static String generate6DigitCode() {
        return RandomUtil.randomNumbers(6);
    }
}