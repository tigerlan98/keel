package io.github.keel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * Keel 应用入口。本模块只负责装配与启动，不包含业务逻辑。
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class KeelApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeelApplication.class, args);
    }
}
