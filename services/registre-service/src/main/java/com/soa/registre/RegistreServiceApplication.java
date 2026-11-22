package com.soa.registre;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class RegistreServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegistreServiceApplication.class, args);
    }
}
