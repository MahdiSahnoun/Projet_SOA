package com.donsang.dons.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

/** WebServiceTemplate configuré avec les classes JAXB générées depuis registre.xsd. */
@Configuration
public class RegistreClientConfig {

    @Bean
    Jaxb2Marshaller registreMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("com.donsang.registre.v1");
        return marshaller;
    }

    @Bean
    WebServiceTemplate registreTemplate(Jaxb2Marshaller registreMarshaller,
                                        ClientInterceptor registreTracingInterceptor,
                                        @Value("${registre.url}") String url,
                                        @Value("${registre.timeout-ms:2000}") long timeoutMs) {
        WebServiceTemplate template = new WebServiceTemplate(registreMarshaller);
        template.setDefaultUri(url);
        // Timeouts explicites : sans eux, un Registre bloqué bloquerait aussi Dons.
        HttpUrlConnectionMessageSender transport = new HttpUrlConnectionMessageSender();
        transport.setConnectionTimeout(Duration.ofMillis(timeoutMs));
        transport.setReadTimeout(Duration.ofMillis(timeoutMs));
        template.setMessageSender(transport);
        template.setInterceptors(new ClientInterceptor[]{registreTracingInterceptor});
        return template;
    }
}
