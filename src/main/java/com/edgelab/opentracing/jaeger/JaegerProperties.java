package com.edgelab.opentracing.jaeger;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "jaeger")
@Validated
@Data
class JaegerProperties {

    private final ConstSampler constSampler = new ConstSampler();
    private final ProbabilisticSampler probabilisticSampler = new ProbabilisticSampler();
    private final RateLimitingSampler rateLimitingSampler = new RateLimitingSampler();
    private final RemoteControlledSampler remoteControlledSampler = new RemoteControlledSampler();

    private String agentHost = "localhost";

    private Integer agentPort = 6831;

    private Integer flushInterval = 10000;

    private Integer maxQueueSize = 1000;

    private String samplingUrl;

    @Value("${spring.application.name}")
    private String serviceName;

    @Data
    static class ConstSampler {

        private Boolean decision;
    }

    @Data
    static class ProbabilisticSampler {

        private Double samplingRate;
    }

    @Data
    static class RateLimitingSampler {

        private Double maxTracesPerSecond;
    }

    @Data
    static class RemoteControlledSampler {

        /**
         * e.g. 169.254.1.1:5778/sampling
         */
        private String url;

        private Double samplingRate = 1.0;
    }

}
