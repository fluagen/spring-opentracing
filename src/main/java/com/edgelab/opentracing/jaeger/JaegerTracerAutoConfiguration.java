package com.edgelab.opentracing.jaeger;

import com.edgelab.opentracing.mdc.DiagnosticContextScopeManager;
import io.jaegertracing.Configuration.SenderConfiguration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.RemoteReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.internal.samplers.HttpSamplingManager;
import io.jaegertracing.internal.samplers.RemoteControlledSampler;
import io.jaegertracing.spi.Reporter;
import io.jaegertracing.spi.Sampler;
import io.jaegertracing.spi.Sender;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.tracer.configuration.TracerAutoConfiguration;
import io.opentracing.util.ThreadLocalScopeManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureBefore(TracerAutoConfiguration.class)
@EnableConfigurationProperties(JaegerProperties.class)
@RequiredArgsConstructor
public class JaegerTracerAutoConfiguration {

    private final JaegerProperties properties;

    @Bean
    public Tracer tracer() {
        Sender sender = new SenderConfiguration()
            .withAgentHost(properties.getAgentHost())
            .withAgentPort(properties.getAgentPort())
            .getSender();

        Reporter reporter = new RemoteReporter.Builder()
            .withSender(sender)
            .withFlushInterval(properties.getFlushInterval())
            .withMaxQueueSize(properties.getMaxQueueSize())
            .build();

        Sampler sampler = new RemoteControlledSampler.Builder(properties.getServiceName())
            .withInitialSampler(new ConstSampler(true))
            .withSamplingManager(new HttpSamplingManager(properties.getSamplingUrl()))
            .build();

        return new JaegerTracer.Builder(properties.getServiceName())
            .withScopeManager(new DiagnosticContextScopeManager(new ThreadLocalScopeManager()))
            .withReporter(reporter)
            .withSampler(sampler)
            .build();
    }

}
