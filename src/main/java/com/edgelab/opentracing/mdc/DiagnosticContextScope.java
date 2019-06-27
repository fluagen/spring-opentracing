package com.edgelab.opentracing.mdc;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import org.slf4j.MDC;

import static com.edgelab.opentracing.mdc.DiagnosticContextScopeManager.SPAN_ID;
import static com.edgelab.opentracing.mdc.DiagnosticContextScopeManager.TRACE_CONTEXT;
import static com.edgelab.opentracing.mdc.DiagnosticContextScopeManager.TRACE_ID;

public class DiagnosticContextScope implements Scope {

    private final DiagnosticContextScopeManager scopeManager;
    private final Span wrapped;
    private final boolean finishOnClose;
    private final DiagnosticContextScope toRestore;

    DiagnosticContextScope(DiagnosticContextScopeManager scopeManager, Span wrapped) {
        this(scopeManager, wrapped, false);
    }

    DiagnosticContextScope(DiagnosticContextScopeManager scopeManager, Span wrapped, boolean finishOnClose) {
        this.scopeManager = scopeManager;
        this.wrapped = wrapped;
        this.finishOnClose = finishOnClose;
        this.toRestore = scopeManager.getTlsScope().get();
        this.scopeManager.getTlsScope().set(this);

        injectMdc(wrapped.context());
    }

    @Override
    public void close() {
        if (scopeManager.getTlsScope().get() != this) {
            // This shouldn't happen if users call methods in the expected order. Bail out.
            return;
        }

        if (finishOnClose) {
            wrapped.finish();
        }

        // restore the previous scope
        scopeManager.getTlsScope().set(toRestore);

        // and inject back the old MDC values
        if (toRestore != null && toRestore.wrapped != null) {
            injectMdc(toRestore.wrapped.context());
        }
    }

    @Override
    public Span span() {
        return wrapped;
    }

    private void injectMdc(SpanContext context) {
        mdcReplace(TRACE_ID, context.toTraceId());
        mdcReplace(SPAN_ID, context.toSpanId());
        mdcReplace(TRACE_CONTEXT, context.toString());

        context.baggageItems().forEach(e -> mdcReplace(e.getKey(), e.getValue()));
    }

    private void mdcReplace(String key, String value) {
        if (value != null) {
            MDC.put(key, value);
        } else {
            MDC.remove(key);
        }
    }

}
