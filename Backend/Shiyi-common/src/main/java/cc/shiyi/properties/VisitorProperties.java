package cc.shiyi.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "shiyi.visitor")
@Data
public class VisitorProperties {
    private String verifyCode;
}
