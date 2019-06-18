package com.edgelab.opentracing.mdc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Copied from Spring Cloud Sleuth
 * To be able to display default tracing info in the logs
 */
public class TraceEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "defaultProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> map = new HashMap<>();

        // display trace context by default
        map.put("logging.pattern.level", "%5p [%X{" + DiagnosticContextScopeManager.TRACE_CONTEXT + ":-}]");

        addOrReplace(environment.getPropertySources(), map);
    }

    private void addOrReplace(MutablePropertySources propertySources, Map<String, Object> map) {
        MapPropertySource target = null;

        if (propertySources.contains(PROPERTY_SOURCE_NAME)) {
            PropertySource<?> source = propertySources.get(PROPERTY_SOURCE_NAME);
            if (source instanceof MapPropertySource) {
                target = (MapPropertySource) source;

                for (Entry<String, Object> e : map.entrySet()) {
                    if (!target.containsProperty(e.getKey())) {
                        target.getSource().put(e.getKey(), e.getValue());
                    }
                }
            }
        }

        if (target == null) {
            target = new MapPropertySource(PROPERTY_SOURCE_NAME, map);
        }

        if (!propertySources.contains(PROPERTY_SOURCE_NAME)) {
            propertySources.addLast(target);
        }
    }

}
