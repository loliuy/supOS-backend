package com.supos.adpter.nodered.util;

import java.util.UUID;

public class IDGenerator {

    public static String generate() {
        // 生成UUID并计算哈希值
        UUID uuid = UUID.randomUUID();
        long hash = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
        String id = Long.toHexString(hash).replaceAll("-", "");
        if (id.length() < 16) {
            return uuid.toString().replaceAll("-", "").substring(0, 16);
        }
        return id.substring(0, 16);

    }
}
