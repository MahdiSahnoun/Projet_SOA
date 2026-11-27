package com.donsang.dons.config;

import brave.Tracing;
import brave.grpc.GrpcTracing;
import io.grpc.ClientInterceptor;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcTracingConfig {

    @Bean
    @GrpcGlobalClientInterceptor
    ClientInterceptor grpcTracingClientInterceptor(Tracing tracing) {
        return GrpcTracing.create(tracing).newClientInterceptor();
    }
}
