package com.ddev.tindakart;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.bind.annotation.RequestMethod;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test-integration")
class ApiEndpointSmokeIntegrationTest {
    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{([^}:]+)(?::[^}]+)?}");

    @Autowired RequestMappingHandlerMapping handlerMapping;

    @Test
    void everyDeclaredApiRouteResolvesToAHandler() throws Exception {
        List<String> checked = new ArrayList<>();
        handlerMapping.getHandlerMethods().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> {
                    RequestMappingInfo mapping = entry.getKey();
                    if (mapping.getPatternValues().stream().noneMatch(path -> path.startsWith("/api"))) return;
                    for (String pattern : mapping.getPatternValues()) {
                        if (!pattern.startsWith("/api")) continue;
                        for (RequestMethod method : mapping.getMethodsCondition().getMethods()) {
                            try {
                                checked.add(method + " " + pattern);
                                String path = replacePathVariables(pattern);
                                MockHttpServletRequest servletRequest = new MockHttpServletRequest();
                                servletRequest.setMethod(method.name());
                                servletRequest.setRequestURI(path);
                                servletRequest.setPathInfo(path);
                                servletRequest.setContentType("application/json");
                                assertNotNull(handlerMapping.getHandler(servletRequest),
                                        method + " " + pattern + " did not resolve a handler");
                            } catch (Exception ex) {
                                throw new RouteSmokeFailure(method + " " + pattern, ex);
                            }
                        }
                    }
                });
        assertTrue(checked.size() >= 65, "Expected the API route inventory to include all declared handlers; checked " + checked.size());
    }

    private String replacePathVariables(String pattern) {
        Matcher matcher = PATH_VARIABLE.matcher(pattern);
        return matcher.replaceAll("1");
    }

    private static final class RouteSmokeFailure extends RuntimeException {
        private RouteSmokeFailure(String route, Exception cause) {
            super("API route smoke failed for " + route, cause);
        }
    }
}
