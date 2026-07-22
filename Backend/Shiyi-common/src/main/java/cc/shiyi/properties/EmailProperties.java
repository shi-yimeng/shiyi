package cc.shiyi.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "shiyi.email")
@Data
public class EmailProperties {
    /**
     * 邮箱服务器邮箱
     */
    private String personal;

    private String from;
}
