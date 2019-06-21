package com.szeastroc.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by Tulane
 * 2019/6/10
 */
@Slf4j
@SpringBootApplication
@EnableZuulProxy
@EnableFeignClients({"com.szeastroc"})
@ComponentScan({"com.szeastroc"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        log.info("service start success");
    }
}
