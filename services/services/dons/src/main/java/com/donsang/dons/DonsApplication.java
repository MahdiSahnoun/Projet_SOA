package com.donsang.dons;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * Service Dons : point d'entrée REST de la plateforme.
 * Il orchestre les appels vers Inventaire (gRPC) et Registre (SOAP).
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class) // l'authentification passe par le JWT
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class DonsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DonsApplication.class, args);
    }

    /** Horloge injectée : rend les règles de date testables. */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
