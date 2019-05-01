package com.edgelab.opentracing.mdc;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class DiagnosticContextScopeManager implements ScopeManager {

    @NonNull
    private final ScopeManager scopeManager;

    @NonNull
    private final TracedDiagnosticContext tracedDiagnosticContext;

    @Override
    public Scope activate(Span span, boolean finishSpanOnClose) {
        // Activate scope
        Scope scope = scopeManager.activate(span, finishSpanOnClose);
        Map<String, String> context = tracedDiagnosticContext.create(scope.span());

        // Return wrapper
        return new DiagnosticContextScope(scope, context);
    }

    @Override
    public Scope active() {
        return scopeManager.active();
    }

    public static class DiagnosticContextScope implements Scope {

        private final Scope scope;
        private final Map<String, String> previous = new HashMap<>();

        DiagnosticContextScope(Scope scope, Map<String, String> context) {
            this.scope = scope;

            // Initialize MDC
            for (Map.Entry<String, String> entry : context.entrySet()) {
                this.previous.put(entry.getKey(), MDC.get(entry.getKey()));
                mdcReplace(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public void close() {
            scope.close();

            // Restore previous context
            for (Map.Entry<String, String> entry : previous.entrySet()) {
                mdcReplace(entry.getKey(), entry.getValue());
            }
        }

        @Override
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
