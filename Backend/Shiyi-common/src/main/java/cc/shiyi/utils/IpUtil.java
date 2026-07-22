package cc.shiyi.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class IpUtil {
    public static final String IP_API = "http://ip-api.com/json/";
    public static final String LANGUAGE = "zh-CN";

    private static final ConcurrentHashMap<String, GeoCacheEntry> GEO_CACHE = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 12L * 60L * 60L * 1000L;

    private static class GeoCacheEntry {
        final Map<String, String> geo;
        final long expireAt;
        GeoCacheEntry(Map<String, String> geo) {
            this.geo = geo;
            this.expireAt = System.currentTimeMillis() + CACHE_TTL_MS;
        }
        boolean isExpired() { return System.currentTimeMillis() > expireAt; }
    }

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("CF-Connecting-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("True-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Ali-CDN-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    public static Map<String, String> getGeoInfo(String ip){
        Map<String, String> result = new HashMap<>();
        if (ip == null || ip.isEmpty()) return result;

        if (isPrivateOrLocalIp(ip)) return result;

        GeoCacheEntry cached = GEO_CACHE.get(ip);
        if (cached != null && !cached.isExpired()) {
            return new HashMap<>(cached.geo);
        }

        Map<String,String> params = new HashMap<>();
        params.put("lang",LANGUAGE);
        String doneGet = null;
        try {
            doneGet = HttpClientUtil.doGet(IP_API + ip, params);
        } catch (Exception e) {
            return result;
        }

        if (doneGet == null || doneGet.isEmpty()) {
            GEO_CACHE.put(ip, new GeoCacheEntry(result));
            return result;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> jsonMap = mapper.readValue(doneGet, Map.class);

            result.put("country", (String) jsonMap.getOrDefault("country", ""));
            result.put("province", stripAdminSuffix((String) jsonMap.getOrDefault("regionName", "")));
            result.put("city", stripAdminSuffix((String) jsonMap.getOrDefault("city", "")));
            result.put("latitude", String.valueOf(jsonMap.getOrDefault("lat", "")));
            result.put("longitude", String.valueOf(jsonMap.getOrDefault("lon", "")));

        } catch (Exception e) {
        }
        GEO_CACHE.put(ip, new GeoCacheEntry(new HashMap<>(result)));
        return result;
    }

    private static boolean isPrivateOrLocalIp(String ip) {
        if (ip == null) return false;
        return ip.startsWith("127.") || ip.startsWith("10.") || ip.startsWith("192.168.")
                || ip.startsWith("172.16.") || ip.startsWith("172.17.") || ip.startsWith("172.18.")
                || ip.startsWith("172.19.") || ip.startsWith("172.2") || ip.startsWith("172.30.")
                || ip.startsWith("172.31.") || "0:0:0:0:0:0:0:1".equals(ip)
                || "localhost".equalsIgnoreCase(ip);
    }

    private static String stripAdminSuffix(String name) {
        if (name == null || name.isEmpty()) return name;
        name = name.replaceAll("壮族自治区|维吾尔自治区|回族自治区|自治区|特别行政区", "");
        if (name.length() > 1 && (name.endsWith("省") || name.endsWith("市"))) {
            name = name.substring(0, name.length() - 1);
        }
        return name;
    }
}
