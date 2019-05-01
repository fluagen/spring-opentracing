package com.edgelab.opentracing.mdc;

import io.opentracing.Span;
import io.opentracing.SpanContext;

import java.util.HashMap;
import java.util.Map;

public final class TracedDiagnosticContext {

    static final String TRACE_ID = "trace-id";

    Map<String, String> create(Span span) {
        SpanContext context = span.context();

        Map<String, String> map = new HashMap<>();
        context.baggageItems().forEach(e -> map.put(e.getKey(), e.getValue()));
        map.put(TRACE_ID, context.toString());

        return map;
    }

}
