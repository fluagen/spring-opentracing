package com.edgelab.opentracing.mdc;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.noop.NoopScopeManager.NoopScope;

public class DiagnosticContextScopeManager implements ScopeManager {

    public static final String TRACE_CONTEXT = "traceCtxt";
    public static final String TRACE_ID = "traceID";
    public static final String SPAN_ID = "spanID";

    final ThreadLocal<DiagnosticContextScope> tlsScope = new ThreadLocal<>();

    @Override
    @Deprecated
    public Scope activate(Span span, boolean finishSpanOnClose) {
        // skip the NoopScope optimization (deprecated method)
        return new DiagnosticContextScope(this, span, finishSpanOnClose);
    }

    @Override
    public Scope activate(Span span) {
        DiagnosticContextScope currentScope = tlsScope.get();

        // avoid creating more duplicated scopes in ThreadLocal if the span is already activated
        // similar optimization like CurrentTraceContext.maybeScope(TraceContext currentSpan) in Brave
        if (currentScope != null && currentScope.span() == span) {
            return NoopScope.INSTANCE;
        }

        return new DiagnosticContextScope(this, span);
    }

    @Override
    @Deprecated
    public Scope active() {
        return tlsScope.get();
    }

    @Override
    public Span activeSpan() {
        DiagnosticContextScope scope = tlsScope.get();
        return scope == null ? null : scope.span();
    }

}
