package org.example.identity.api;

import org.example.identity.api.model.User;
import java.util.Date;

public class VipUtil {
    private VipUtil() {}

    public static boolean isActiveVip(User user) {
        return user != null
                && user.getVipType() != null
                && user.getVipType() == 1
                && user.getVipExpireTime() != null
                && user.getVipExpireTime().after(new Date());
    }

    public static Date calculateNewExpireTime(Date currentExpireTime, int couponTypeDays) {
        Date now = new Date();
        Date baseTime;
        if (currentExpireTime != null && currentExpireTime.after(now)) {
            baseTime = currentExpireTime;
        } else {
            baseTime = now;
        }
        return new Date(baseTime.getTime() + (long) couponTypeDays * 24 * 3600 * 1000);
    }
}
