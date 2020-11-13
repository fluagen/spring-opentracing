package com.edgelab.opentracing.jaeger;

import com.edgelab.opentracing.mdc.DiagnosticContextScopeManager;
import io.jaegertracing.Configuration.SenderConfiguration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.RemoteReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.internal.samplers.HttpSamplingManager;
import io.jaegertracing.internal.samplers.ProbabilisticSampler;
import io.jaegertracing.internal.samplers.RateLimitingSampler;
import io.jaegertracing.internal.samplers.RemoteControlledSampler;
import io.jaegertracing.spi.Reporter;
import io.jaegertracing.spi.Sampler;
import io.jaegertracing.spi.Sender;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.tracer.configuration.TracerAutoConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@AutoConfigureBefore(TracerAutoConfiguration.class)
@EnableConfigurationProperties(JaegerProperties.class)
@RequiredArgsConstructor
@Slf4j
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

        return new JaegerTracer.Builder(properties.getServiceName())
            .withScopeManager(new DiagnosticContextScopeManager())
            .withReporter(reporter)
            .withSampler(sampler())
            .build();
    }

    @ConditionalOnMissingBean
    @Bean
    public Sampler sampler() {
        Boolean decision = properties.getConstSampler().getDecision();
        if (decision != null) {
            log.info("Use const sampler with '{}' decision", decision);
            return new ConstSampler(decision);
        }

        Double samplingRate = properties.getProbabilisticSampler().getSamplingRate();
        if (samplingRate != null) {
            log.info("Use probabilistic sampler with {} sampling rate", samplingRate);
            return new ProbabilisticSampler(samplingRate);
        }

        Double maxTracesPerSecond = properties.getRateLimitingSampler().getMaxTracesPerSecond();
        if (maxTracesPerSecond != null) {
            log.info("Use rate limiting sampler with {} max traces per second", maxTracesPerSecond);
            return new RateLimitingSampler(maxTracesPerSecond);
        }

        String url = properties.getRemoteControlledSampler().getUrl();
        if (url != null && url.length() > 0) {
            log.info("Use remote controller sampler on {}", url);

            return new RemoteControlledSampler.Builder(properties.getServiceName())
                .withSamplingManager(new HttpSamplingManager(url))
                .build();
        }

        // fallback to sample every trace
        log.info("Use fallback const sampler with 'true' decision");
        return new ConstSampler(true);
    }

}
