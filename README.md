# spring-opentracing

Jaeger-based opentracing integration on top of [io.opentracing.contrib.opentracing-spring-cloud-starter](https://github.com/opentracing-contrib/java-spring-cloud)

Apart from what's already provided by `opentracing-spring-cloud-starter`, this library just injects the tracing information in the logs (via MDC) in the similar way as [Spring Cloud Sleuth](https://github.com/spring-cloud/spring-cloud-sleuth) does.

## How to use it with minimal efforts

Just add the following dependency in your pom.xml:

```xml
<dependency>
  <groupId>com.edgelab</groupId>
  <artifactId>spring-opentracing</artifactId>
  <version>0.1.0</version>
</dependency>
```

And configure the remote reporter:

```yaml
jaeger:
  agent-host: 169.254.1.1
  agent-port: 6831
  sampling-url: 169.254.1.1:5778/sampling
  service-name: toto
```

### In case you want to skip some custom urls:

```yaml
opentracing:
  spring:
    web:
      skip-pattern: /manage/.*
```

### In case you want to display your custom baggage items in the logs:

You could put the following configuration in the `bootstrap.yml`,
so that if you set a baggage item with key: `client-in-the-baggage`, it will be displayed in the logs.

```yaml
logging:
  pattern:
    level: '%5p [%X{trace-id:-}/%X{client-in-the-baggage:-}]'
```
