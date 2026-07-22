package cc.shiyi.service.impl;

import cc.shiyi.service.KugouMusicService;
import cc.shiyi.vo.KugouMusicVO;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class KugouMusicServiceImpl implements KugouMusicService {

    private static final String KUGOU_SEARCH_URL = "https://songsearch.kugou.com/song_search_v2";
    private static final String KUGOU_DETAIL_URL = "https://wwwapi.kugou.com/yy/index.php";

    @Override
    public List<KugouMusicVO> search(String keyword, int page, int pageSize) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            String encodedKeyword = URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8).replace("+", "%20");
            String urlStr = KUGOU_SEARCH_URL + "?keyword=" + encodedKeyword + 
                    "&page=" + page + "&pagesize=" + pageSize + 
                    "&userid=-1&clientver=&platform=WebFilter&tag=em&filter=2&iscorrection=1&privilege_filter=0";

            String response = doGet(urlStr);
            if (response == null || response.isEmpty()) {
                log.warn("[Kugou] search returned empty response");
                return Collections.emptyList();
            }

            JSONObject json = JSONObject.parseObject(response);
            JSONObject data = json.getJSONObject("data");
            if (data == null) {
                log.warn("[Kugou] search data is null");
                return Collections.emptyList();
            }

            JSONArray lists = data.getJSONArray("lists");
            if (lists == null || lists.isEmpty()) {
                return Collections.emptyList();
            }

            List<KugouMusicVO> result = new ArrayList<>();
            for (int i = 0; i < lists.size(); i++) {
                JSONObject item = lists.getJSONObject(i);
                KugouMusicVO vo = parseSearchItem(item);
                if (vo != null) {
                    result.add(vo);
                }
            }
            return result;

        } catch (Exception e) {
            log.error("[Kugou] search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private KugouMusicVO parseSearchItem(JSONObject item) {
        try {
            String songName = item.getString("SongName");
            String singerName = item.getString("SingerName");
            String albumName = item.getString("AlbumName");
            Integer duration = item.getInteger("Duration");
            String hash = item.getString("FileHash");
            String albumId = item.getString("AlbumID");
            String imageMin = item.getString("ImageMin");

            if (songName == null || songName.trim().isEmpty()) {
                return null;
            }

            return KugouMusicVO.builder()
                    .title(songName.trim())
                    .artist(singerName != null ? singerName.trim() : "")
                    .album(albumName != null ? albumName.trim() : "")
                    .duration(duration)
                    .coverImage(imageMin)
                    .hash(hash)
                    .albumId(albumId)
                    .build();

        } catch (Exception e) {
            log.warn("[Kugou] parse search item failed: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public KugouMusicVO getPlayUrl(String hash, String albumId) {
        if (hash == null || hash.trim().isEmpty()) {
            log.warn("[Kugou] hash is null");
            return null;
        }

        try {
            String urlStr = String.format("%s?r=play/getdata&hash=%s&album_id=%s&dfid=2d8f5b1f9a3a391f0b245e9d7b8d8d8d&mid=2d8f5b1f9a3a391f0b245e9d7b8d8d8d",
                    KUGOU_DETAIL_URL, hash, albumId != null ? albumId : "");

            String response = doGet(urlStr);
            if (response == null || response.isEmpty()) {
                log.warn("[Kugou] getPlayUrl returned empty response");
                return null;
            }

            JSONObject json = JSONObject.parseObject(response);
            Integer status = json.getInteger("status");
            Integer errCode = json.getInteger("err_code");
            
            if (errCode != null && errCode != 0) {
                log.warn("[Kugou] getPlayUrl err_code: {}", errCode);
                if (errCode == 30020) {
                    log.warn("[Kugou] 歌曲需要付费，尝试其他方法");
                    return tryAlternativeApi(hash, albumId);
                }
                return null;
            }

            if (status == null || status != 1) {
                log.warn("[Kugou] getPlayUrl status: {}", status);
                return null;
            }

            JSONObject data = json.getJSONObject("data");
            if (data == null) {
                log.warn("[Kugou] getPlayUrl data is null");
                return null;
            }

            String audioUrl = data.getString("play_url");
            if (audioUrl == null || audioUrl.trim().isEmpty()) {
                log.warn("[Kugou] play_url is empty, trying alternative");
                return tryAlternativeApi(hash, albumId);
            }

            return parseDetailItem(data, hash, albumId);

        } catch (Exception e) {
            log.error("[Kugou] getPlayUrl failed: {}", e.getMessage());
            return null;
        }
    }

    private KugouMusicVO tryAlternativeApi(String hash, String albumId) {
        try {
            String urlStr = "https://m.kugou.com/app/i/getSongInfo.php?cmd=playInfo&hash=" + hash + 
                    (albumId != null && !albumId.isEmpty() ? "&album_id=" + albumId : "");
            
            String response = doGet(urlStr);
            if (response == null || response.isEmpty()) {
                return null;
            }

            JSONObject json = JSONObject.parseObject(response);
            Integer status = json.getInteger("status");
            if (status == null || status != 1) {
                return null;
            }

            String audioUrl = json.getString("url");
            String lyrics = json.getString("lyrics");
            String title = json.getString("song_name");
            String artist = json.getString("author_name");
            String album = json.getString("album_name");
            Integer duration = json.getInteger("duration");
            String img = json.getString("imgUrl");

            if (audioUrl == null || audioUrl.trim().isEmpty()) {
                return null;
            }

            return KugouMusicVO.builder()
                    .title(title != null ? title.trim() : "")
                    .artist(artist != null ? artist.trim() : "")
                    .album(album != null ? album.trim() : "")
                    .duration(duration)
                    .coverImage(img)
                    .musicUrl(audioUrl)
                    .lyricUrl(lyrics)
                    .hash(hash)
                    .albumId(albumId)
                    .build();

        } catch (Exception e) {
            log.warn("[Kugou] alternative API failed: {}", e.getMessage());
            return null;
        }
    }

    private KugouMusicVO parseDetailItem(JSONObject data, String hash, String albumId) {
        try {
            String audioUrl = data.getString("play_url");
            String lyrics = data.getString("lyrics");
            String title = data.getString("song_name");
            String artist = data.getString("author_name");
            String album = data.getString("album_name");
            Integer duration = data.getInteger("duration");
            String img = data.getString("img");

            return KugouMusicVO.builder()
                    .title(title != null ? title.trim() : "")
                    .artist(artist != null ? artist.trim() : "")
                    .album(album != null ? album.trim() : "")
                    .duration(duration)
                    .coverImage(img)
                    .musicUrl(audioUrl)
                    .lyricUrl(lyrics)
                    .hash(hash)
                    .albumId(albumId)
                    .build();

        } catch (Exception e) {
            log.warn("[Kugou] parse detail item failed: {}", e.getMessage());
            return null;
        }
    }

    private String doGet(String urlStr) {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Referer", "https://www.kugou.com/");
            conn.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.warn("[Kugou] HTTP response code: {}", responseCode);
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();

        } catch (Exception e) {
            log.error("[Kugou] HTTP request failed: {}", e.getMessage());
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
