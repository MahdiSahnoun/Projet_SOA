package com.donsang.dons.client;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.stereotype.Component;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.transport.HeadersAwareSenderWebServiceConnection;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;

import java.io.IOException;
import java.io.UncheckedIOException;

@Component
public class SoapTracingClientInterceptor implements ClientInterceptor {

    private static final String SPAN_KEY = SoapTracingClientInterceptor.class.getName() + ".span";
    private static final String SCOPE_KEY = SoapTracingClientInterceptor.class.getName() + ".scope";

    private final Tracer tracer;
    private final Propagator propagator;

    public SoapTracingClientInterceptor(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Override
    public boolean handleRequest(MessageContext messageContext) {
        Span span = tracer.nextSpan().name("registre SOAP").start();
        messageContext.setProperty(SPAN_KEY, span);
        messageContext.setProperty(SCOPE_KEY, tracer.withSpan(span));
        try {
            TransportContext transportContext = TransportContextHolder.getTransportContext();
            if (transportContext != null
                    && transportContext.getConnection() instanceof HeadersAwareSenderWebServiceConnection connection) {
                propagator.inject(span.context(), connection, (carrier, key, value) -> {
                    try {
                        carrier.addRequestHeader(key, value);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        } catch (UncheckedIOException e) {
            finish(messageContext, e);
            throw e;
        }
        return true;
    }

    @Override
    public boolean handleResponse(MessageContext messageContext) {
        finish(messageContext, null);
        return true;
    }

    @Override
    public boolean handleFault(MessageContext messageContext) {
        finish(messageContext, new IllegalStateException("SOAP fault returned by registre-service"));
        return true;
    }

    @Override
    public void afterCompletion(MessageContext messageContext, Exception exception) {
        finish(messageContext, exception);
    }

    private static void finish(MessageContext messageContext, Throwable error) {
        Object scope = messageContext.getProperty(SCOPE_KEY);
        if (!(scope instanceof Tracer.SpanInScope spanInScope)) {
            return;
        }
        Object storedSpan = messageContext.getProperty(SPAN_KEY);
        if (storedSpan instanceof Span span) {
            if (error != null) {
                span.error(error);
            }
            span.end();
        }
        spanInScope.close();
        messageContext.setProperty(SCOPE_KEY, null);
        messageContext.setProperty(SPAN_KEY, null);
    }
}
