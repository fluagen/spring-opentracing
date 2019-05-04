package com.edgelab.opentracing;

import com.edgelab.opentracing.TraceLoggingTests.TestController;
import io.opentracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = {TestController.class}, webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = {"spring.application.name: toto-api", "logging.pattern.level: %5p [%X{trace-id:-}/%X{x-root-caller:-}]"})
@RunWith(SpringRunner.class)
@Slf4j
public class TraceLoggingTests {

    private static final String BAGGAGE_KEY = "x-root-caller";
    private static final String BAGGAGE_VALUE = "titi";

    @LocalServerPort
    private int port;

    @RestController
    @EnableAutoConfiguration
    public static class TestController {

        @Autowired
        private Tracer tracer;

        private static final Scheduler CACHED_SCHEDULER = Schedulers.parallel();

        @GetMapping
        public Flux<Integer> tracedFlux() {
            return Flux.range(1, 10)
                .flatMap(this::doubleMono);
        }

        private Mono<Integer> doubleMono(Integer x) {
            return Mono.fromSupplier(() -> {
                assertThat(tracer.activeSpan()).isNotNull();
                assertThat(tracer.activeSpan().getBaggageItem(BAGGAGE_KEY)).isEqualTo(BAGGAGE_VALUE);

                log.info("You should see logs with tracing info for element '{}'", x);
                return x * 2;
            }).subscribeOn(CACHED_SCHEDULER);
        }

    }

    @Test
    public void testControllerTracing() {
        String url = format("http://localhost:%s", port);

        Long count = WebClient.create().get().uri(url)
            .header("uberctx-" + BAGGAGE_KEY, BAGGAGE_VALUE) // jaeger specific header prefix
            .retrieve()
            .bodyToFlux(Integer.class)
            .count()
            .block();

        assertThat(count).isEqualTo(10);
    }

}
