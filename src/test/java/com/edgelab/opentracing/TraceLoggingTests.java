package com.edgelab.opentracing;

import com.edgelab.opentracing.TraceLoggingTests.TestController;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
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

import static com.edgelab.opentracing.mdc.DiagnosticContextScopeManager.SPAN_ID;
import static com.edgelab.opentracing.mdc.DiagnosticContextScopeManager.TRACE_CONTEXT;
import static com.edgelab.opentracing.mdc.DiagnosticContextScopeManager.TRACE_ID;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = {TestController.class}, webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.application.name: toto-api",
    "jaeger.remote-controlled-sampler.url: 169.254.1.1:5778/sampling",
    "logging.pattern.level: %5p [%X{traceCtxt:-}/%X{x-root-caller:-}]"
})
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
                assertMdc();

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

    @Test
    public void makeSureMdcDataAreRemovedAfterClosingSpan() {
        Span span = GlobalTracer.get().buildSpan("shortLivedSpan").start();
        span.setBaggageItem(BAGGAGE_KEY, BAGGAGE_VALUE);

        try (Scope scope = GlobalTracer.get().activateSpan(span)) {
            assertMdc();

        } finally {
            span.finish();
        }

        refuteMdc();
    }

    @Test
    public void makeSureWrappedContextBaggageAndContextToRestoreBaggageAreNotMixedUp() {
        Span span1 = GlobalTracer.get().buildSpan("span1").start();
        span1.setBaggageItem("item1", "abc");

        Span span2 = GlobalTracer.get().buildSpan("span2").start();
        span2.setBaggageItem("item2", "123");

        try (Scope scope1 = GlobalTracer.get().activateSpan(span1)) {
            assertThat(MDC.get("item1")).isEqualTo("abc");
            assertThat(MDC.get("item2")).isNull();

            try (Scope scope2 = GlobalTracer.get().activateSpan(span2)) {
                assertThat(MDC.get("item1")).isNull();
                assertThat(MDC.get("item2")).isEqualTo("123");
            }

            assertThat(MDC.get("item1")).isEqualTo("abc");
            assertThat(MDC.get("item2")).isNull();

        } finally {
            span1.finish();
            span2.finish();
        }
    }

    private static void assertMdc() {
        assertThat(MDC.get(TRACE_CONTEXT)).isNotEmpty();
        assertThat(MDC.get(TRACE_ID)).isNotEmpty();
        assertThat(MDC.get(SPAN_ID)).isNotEmpty();
        assertThat(MDC.get(BAGGAGE_KEY)).isEqualTo(BAGGAGE_VALUE);
    }

    private static void refuteMdc() {
        assertThat(MDC.get(TRACE_CONTEXT)).isNull();
        assertThat(MDC.get(TRACE_ID)).isNull();
        assertThat(MDC.get(SPAN_ID)).isNull();
        assertThat(MDC.get(BAGGAGE_KEY)).isNull();
    }

}
