package com.donsang.inventaire.config;

import brave.Tracing;
import brave.grpc.GrpcTracing;
import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcTracingConfig {

    @Bean
    @GrpcGlobalServerInterceptor
    ServerInterceptor grpcTracingServerInterceptor(Tracing tracing) {
        return GrpcTracing.create(tracing).newServerInterceptor();
    }
}
