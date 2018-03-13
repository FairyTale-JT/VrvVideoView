package com.jt.face;

/**
 * @author mao
 * @version v1.0
 * @date 2016/9/2 10:39
 * @des 时间格式转换
 */
public class RegularExpress {
    private static final int HOUR = 60 * 60 * 1000;
    private static final int MINUTE = 60 * 1000;
    private static final int SECOND = 1000;

    private RegularExpress() {
    }
    /**
     * 将int类型的毫秒时间值转化为形如这样格式的字符串: 02:30:23,2小时,30分钟,23秒
     *
     * @param duration 毫秒时间值
     * @return 转化好的时间字符串, 形如 02:30:23
     */
    public static String parseDuration(long duration) {
        long hour = duration / HOUR;
        long min = duration % HOUR / MINUTE;
        long sec = duration % HOUR % MINUTE / SECOND;
        if (hour == 0) {
            return String.format("%02d : %02d", min, sec);
        }
        return String.format("%02d : %02d : %02d", hour, min, sec);
    }
}
