package cc.shiyi.controller.blog;

import cc.shiyi.properties.WebsiteProperties;
import cc.shiyi.result.PageResult;
import cc.shiyi.service.ArticleService;
import cc.shiyi.service.SitemapService;
import cc.shiyi.vo.BlogArticleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 博客端 Sitemap 接口
 */
@Slf4j
@RestController("blogSitemapController")
@RequestMapping("/blog")
public class SitemapController {

    @Autowired
    private SitemapService sitemapService;


    /**
     * 动态生成站点地图 XML
     */
    @GetMapping(value = "/sitemap.xml", produces = "application/xml; charset=UTF-8")
    @Cacheable(value = "sitemap", key = "'xml'")
    public String sitemap() {
        String xml = sitemapService.generateSitemap();
        return xml;
    }
}
