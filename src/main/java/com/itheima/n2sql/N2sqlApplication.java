package com.itheima.n2sql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

// 暂时排除数据源自动配置，因为我们使用动态数据源管理，不需要 Spring 自动创建数据源
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class N2sqlApplication {

    public static void main(String[] args) {
        SpringApplication.run(N2sqlApplication.class, args);
    }

}
