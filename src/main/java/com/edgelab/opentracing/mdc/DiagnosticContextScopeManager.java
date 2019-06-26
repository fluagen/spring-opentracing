package com.edgelab.opentracing.mdc;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;

public class DiagnosticContextScopeManager implements ScopeManager {

    public static final String TRACE_CONTEXT = "traceCtxt";
    public static final String TRACE_ID = "traceID";
    public static final String SPAN_ID = "spanID";

    final ThreadLocal<DiagnosticContextScope> tlsScope = new ThreadLocal<>();

    @Override
    @Deprecated
    public Scope activate(Span span, boolean finishSpanOnClose) {
        return new DiagnosticContextScope(this, span, finishSpanOnClose);
    }

    @Override
    public Scope activate(Span span) {
        Scope currentScope = tlsScope.get();
        if (currentScope != null && currentScope.span() == span) {
            return currentScope;
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
        Scope scope = tlsScope.get();
        return scope == null ? null : scope.span();
    }

}
