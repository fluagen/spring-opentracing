# spring-opentracing

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/16c00adc8a25456598b501a8127576fa)](https://www.codacy.com/app/cji/spring-opentracing?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=edgelaboratories/spring-opentracing&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/16c00adc8a25456598b501a8127576fa)](https://www.codacy.com/app/cji/spring-opentracing?utm_source=github.com&utm_medium=referral&utm_content=edgelaboratories/spring-opentracing&utm_campaign=Badge_Coverage)

Jaeger-based opentracing integration on top of [io.opentracing.contrib.opentracing-spring-cloud-starter](https://github.com/opentracing-contrib/java-spring-cloud)

Apart from what's already provided by `opentracing-spring-cloud-starter`, this library just injects the tracing information in the logs (via MDC) in the similar way as [Spring Cloud Sleuth](https://github.com/spring-cloud/spring-cloud-sleuth) does.

## How to use it with minimal efforts

Just add the following dependency in your pom.xml.
You could find all the built versions on [Artifactory](https://intranet.edgelab.ch/artifactory/libs-release-local/com/edgelab/spring-opentracing).

```xml
<dependency>
  <groupId>com.edgelab</groupId>
  <artifactId>spring-opentracing</artifactId>
  <version>${spring-opentracing.version}</version>
</dependency>
```

And configure the remote reporter:

```yaml
jaeger:
  agent-host: 169.254.1.1
  agent-port: 6831
  remote-controlled-sampler.url: 169.254.1.1:5778/sampling
  service-name: toto
```

### In case you want to skip some custom urls

```yaml
opentracing:
  spring:
    web:
      skip-pattern: /manage/.*
```

### In case you want to display your custom baggage items in the logs

You could put the following configuration in the `bootstrap.yml`,
so that if you set a baggage item with key: `client-in-the-baggage`, it will be displayed in the logs.

```yaml
logging:
  pattern:
    level: '%5p [%X{traceCtxt:-}/%X{client-in-the-baggage:-}]'
```

### Using tracing from local machine
To use tracing on your local machine we need to run a local jaeger agent that gathers data and sends it to the jaeger collector.
```
docker run --rm -p 6831:6831 -e REPORTER_GRPC_HOST_PORT=jaeger-collector.service.consul:14250 -e LOG_LEVEL=debug -p 6831:6831/udp jaegertracing/jaeger-agent:1.19.2
```

We must also adjust the jaeger properties:
```yaml
jaeger:
  agent-host: localhost
  agent-port: 6831
  flush-interval: 1000
```
