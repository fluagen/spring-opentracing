package com.edgelab.opentracing;

import com.edgelab.opentracing.TraceLoggingTests.TestController;
import com.edgelab.opentracing.jaeger.TracingAutoConfiguration;
import io.opentracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.List;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpMethod.GET;

@SpringBootTest(classes = {TracingAutoConfiguration.class, TestController.class}, webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = {"jaeger.service-name: toto", "logging.pattern.level: %5p [%X{trace-id:-}/%X{x-root-caller:-}]"})
@RunWith(SpringRunner.class)
@Slf4j
public class TraceLoggingTests {

    private static final String BAGGAGE_KEY = "x-root-caller";
    private static final String BAGGAGE_VALUE = "toto";

    @LocalServerPort
    private int port;

    @RestController
    @EnableAutoConfiguration
    public static class TestController {

        static final String URI = "/traced-flux";

        @Autowired
        private Tracer tracer;

        private static final Scheduler CACHED_SCHEDULER = Schedulers.parallel();

        @GetMapping(URI)
        public Flux<Integer> tracedFlux() {
            return Flux.range(1, 10)
                .flatMap(this::doubleMono);
        }

        private Mono<Integer> doubleMono(Integer x) {
            return Mono.fromSupplier(() -> {
                assertThat(tracer.activeSpan()).isNotNull();
                assertThat(tracer.activeSpan().getBaggageItem(BAGGAGE_KEY)).isEqualTo(BAGGAGE_VALUE);

                log.info("You should see logs with tracing info");
                return x * 2;
            }).subscribeOn(CACHED_SCHEDULER);
        }

    }

    @Test
    public void testControllerTracing() {
        String url = format("http://localhost:%s%s", port, TestController.URI);
        ResponseEntity<List> response = new RestTemplate().exchange(url, GET, new HttpEntity<>(withHeader()), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(10);
    }

    private HttpHeaders withHeader() {
        return new HttpHeaders() {{
            // jaeger specific header prefix
            set("uberctx-" + BAGGAGE_KEY, BAGGAGE_VALUE);
        }};
    }

}
