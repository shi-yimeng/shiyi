package cc.shiyi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableCaching // 开启缓存注解功能
@EnableTransactionManagement // 开启事务管理
@EnableScheduling // 开启定时任务
@EnableAsync // 开启异步方法执行
@Slf4j
public class ShiyiBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShiyiBackendApplication.class, args);
        log.info("Shiyi Backend server started");
    }
}
