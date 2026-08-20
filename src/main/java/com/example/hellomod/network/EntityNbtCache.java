package com.example.hellomod.network;

import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端缓存：存储从服务端接收到的实体 NBT 数据。
 * 数据带有时间戳，超过一定时间后视为过期需要重新请求。
 */
public class EntityNbtCache {

    private static final Map<Integer, CacheEntry> cache = new ConcurrentHashMap<>();

    /** 缓存有效期（毫秒），500ms 后过期需要重新请求 */
    private static final long CACHE_EXPIRE_MS = 500;

    /** 请求冷却（毫秒），避免短时间内频繁发包 */
    private static final long REQUEST_COOLDOWN_MS = 200;

    private static long lastRequestTime = 0;

    private static record CacheEntry(NbtCompound nbt, long timestamp) {}

    public static void put(int entityId, NbtCompound nbt) {
        cache.put(entityId, new CacheEntry(nbt, System.currentTimeMillis()));
    }

    /**
     * 获取缓存的 NBT 数据，如果过期返回 null。
     */
    @Nullable
    public static NbtCompound get(int entityId) {
        CacheEntry entry = cache.get(entityId);
        if (entry == null) return null;
        if (System.currentTimeMillis() - entry.timestamp > CACHE_EXPIRE_MS) {
            cache.remove(entityId);
            return null;
        }
        return entry.nbt;
    }

    /**
     * 检查是否可以发送新的请求（冷却判断）。
     */
    public static boolean canRequest() {
        return System.currentTimeMillis() - lastRequestTime >= REQUEST_COOLDOWN_MS;
    }

    /**
     * 标记已发送请求。
     */
    public static void markRequested() {
        lastRequestTime = System.currentTimeMillis();
    }

    /**
     * 清除所有缓存。
     */
    public static void clear() {
        cache.clear();
    }
}
