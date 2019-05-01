package com.edgelab.opentracing.jaeger;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "jaeger")
@Validated
@Data
class JaegerProperties {

    private String agentHost = "localhost";

    private Integer agentPort = 6831;

    private String samplingUrl;

    private String serviceName;

}
