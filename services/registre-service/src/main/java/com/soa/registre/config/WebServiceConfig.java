package com.soa.registre.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/**
 * Configuration Spring-WS.
 *
 * - MessageDispatcherServlet mappé sur /ws/* : le client Dons envoie ses requêtes
 *   sur http://localhost:8083/ws (propriété registre.url côté Dons)
 * - DefaultWsdl11Definition nommée "registre" → WSDL disponible sur /ws/registre.wsdl
 * - Namespace = targetNamespace du registre.xsd partagé avec Dons
 */
@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            ApplicationContext applicationContext) {

        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "registre")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema registreSchema) {
        DefaultWsdl11Definition wsdl = new DefaultWsdl11Definition();
        wsdl.setPortTypeName("RegistrePort");
        wsdl.setLocationUri("/ws");
        wsdl.setTargetNamespace("http://donsang.com/registre/v1");
        wsdl.setSchema(registreSchema);
        return wsdl;
    }

    @Bean
    public XsdSchema registreSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/registre.xsd"));
    }
}
