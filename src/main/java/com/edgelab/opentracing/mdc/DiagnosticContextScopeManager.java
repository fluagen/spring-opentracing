package com.edgelab.opentracing.mdc;

import io.jaegertracing.internal.JaegerSpanContext;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class DiagnosticContextScopeManager implements ScopeManager {

    public static final String TRACE_ID = "traceID";
    public static final String SPAN_ID = "spanID";
    public static final String PARENT_SPAN_ID = "parentSpanID";

    @NonNull
    private final ScopeManager scopeManager;

    @Override
    @Deprecated
    public Scope activate(Span span, boolean finishSpanOnClose) {
        // activate scope
        Scope scope = scopeManager.activate(span, finishSpanOnClose);
        Map<String, String> context = createContext(scope.span());

        return new DiagnosticContextScope(scope, context);
    }

    @Override
    public Scope activate(Span span) {
        // activate scope
        Scope scope = scopeManager.activate(span);
        Map<String, String> context = createContext(span);

        return new DiagnosticContextScope(scope, context);
    }

    @Override
    @Deprecated
    public Scope active() {
        return scopeManager.active();
    }

    @Override
    public Span activeSpan() {
        return scopeManager.activeSpan();
    }

    private Map<String, String> createContext(Span span) {
        SpanContext context = span.context();

        Map<String, String> map = new HashMap<>();
        context.baggageItems().forEach(e -> map.put(e.getKey(), e.getValue()));

        // here we rely on the toString() implementation of the SpanContext
        // which prints trace id, span id, parent span id in a single block
        map.put(TRACE_ID, context.toTraceId());
        map.put(SPAN_ID, context.toSpanId());

        if (context instanceof JaegerSpanContext) {
            JaegerSpanContext jaegerContext = (JaegerSpanContext) context;
            map.put(PARENT_SPAN_ID, String.valueOf(jaegerContext.getParentId()));
        }

        return map;
    }

    public static class DiagnosticContextScope implements Scope {

        private final Scope scope;
        private final Map<String, String> previous = new HashMap<>();

        DiagnosticContextScope(Scope scope, Map<String, String> context) {
            this.scope = scope;

            // initialize MDC
            for (Map.Entry<String, String> entry : context.entrySet()) {
                previous.put(entry.getKey(), MDC.get(entry.getKey()));
                mdcReplace(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public void close() {
            scope.close();

            // restore previous context
            for (Map.Entry<String, String> entry : previous.entrySet()) {
                mdcReplace(entry.getKey(), entry.getValue());
            }
        }

        @Override
        @Deprecated
        public Span span() {
            return scope.span();
        }

        private static void mdcReplace(String key, String value) {
            if (value != null) {
                MDC.put(key, value);
            } else {
                MDC.remove(key);
            }
        }

    }

}
