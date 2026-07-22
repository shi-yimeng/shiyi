package cc.shiyi.service.impl;

import cc.shiyi.properties.WebsiteProperties;
import cc.shiyi.result.PageResult;
import cc.shiyi.service.ArticleService;
import cc.shiyi.service.PersonalInfoService;
import cc.shiyi.service.RssFeedService;
import cc.shiyi.vo.BlogArticleDetailVO;
import cc.shiyi.vo.BlogArticleVO;
import cc.shiyi.vo.PersonalInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class RssFeedServiceImpl implements RssFeedService {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private PersonalInfoService personalInfoService;

    @Autowired
    private WebsiteProperties websiteProperties;

    private static final DateTimeFormatter RSS_DATE_FMT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss '+0800'", java.util.Locale.ENGLISH);

    /**
     * 生成 RSS 2.0 Feed XML
     */
    public String generateRssFeed() {
        String BLOG_BASE_URL = websiteProperties.getBlog();

        // 获取个人信息作为站点信息
        PersonalInfoVO info = personalInfoService.getPersonalInfo();
        String siteName = info != null && info.getNickname() != null ? info.getNickname() + "的博客" : "Shiyi Blog";
        String siteDescription = info != null && info.getDescription() != null ? info.getDescription() : "个人博客";

        // 获取最新20篇已发布文章
        PageResult pageResult = articleService.getPublishedPage(1, 20);
        @SuppressWarnings("unchecked")
        List<BlogArticleVO> articles = (List<BlogArticleVO>) pageResult.getRecords();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\">\n");
        xml.append("  <channel>\n");
        xml.append("    <title>").append(escapeXml(siteName)).append("</title>\n");
        xml.append("    <link>").append(BLOG_BASE_URL).append("</link>\n");
        xml.append("    <description>").append(escapeXml(siteDescription)).append("</description>\n");
        xml.append("    <language>zh-CN</language>\n");
        xml.append("    <lastBuildDate>").append(LocalDateTime.now().format(RSS_DATE_FMT)).append("</lastBuildDate>\n");
        xml.append("    <atom:link href=\"").append(BLOG_BASE_URL).append("/rss\" rel=\"self\" type=\"application/rss+xml\"/>\n");

        if (articles != null) {
            for (BlogArticleVO article : articles) {
                xml.append("    <item>\n");
                xml.append("      <title>").append(escapeXml(article.getTitle())).append("</title>\n");
                xml.append("      <link>").append(BLOG_BASE_URL).append("/article/").append(article.getSlug()).append("</link>\n");
                xml.append("      <guid isPermaLink=\"true\">").append(BLOG_BASE_URL).append("/article/").append(article.getSlug()).append("</guid>\n");
                if (article.getSummary() != null) {
                    xml.append("      <description>").append(escapeXml(article.getSummary())).append("</description>\n");
                }

                BlogArticleDetailVO detail = articleService.getBySlug(article.getSlug());
                String fullContent = detail != null
                        ? (detail.getContentHtml() != null && !detail.getContentHtml().isBlank()
                        ? detail.getContentHtml()
                        : detail.getContentMarkdown())
                        : null;
                if (fullContent != null && !fullContent.isBlank()) {
                    xml.append("      <content:encoded><![CDATA[")
                            .append(wrapCData(fullContent))
                            .append("]]></content:encoded>\n");
                }

                if (article.getCategoryName() != null) {
                    xml.append("      <category>").append(escapeXml(article.getCategoryName())).append("</category>\n");
                }
                if (article.getPublishTime() != null) {
                    xml.append("      <pubDate>").append(article.getPublishTime().format(RSS_DATE_FMT)).append("</pubDate>\n");
                }
                xml.append("    </item>\n");
            }
        }

        xml.append("  </channel>\n");
        xml.append("</rss>\n");
        return xml.toString();
    };

    /**
     * XML特殊字符转义
     */
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String wrapCData(String text) {
        if (text == null) return "";
        return text.replace("]]>", "]]]]><![CDATA[>");
    }
}
