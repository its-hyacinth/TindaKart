package com.ddev.tindakart;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class TenantContextService {
    private static final String CURRENT_STORE_ID = "CURRENT_STORE_ID";
    private final TenantAccessService tenantAccessService;

    public TenantContextService(TenantAccessService tenantAccessService) {
        this.tenantAccessService = tenantAccessService;
    }

    public AuthController.ContextView resolve(HttpServletRequest request, Authentication authentication) {
        HttpSession session = request.getSession(false);
        if (session == null) return new AuthController.ContextView(null);
        Long storeId = (Long) session.getAttribute(CURRENT_STORE_ID);
        if (storeId == null) return new AuthController.ContextView(null);
        if (!tenantAccessService.hasStoreAccess(authentication, storeId)) {
            session.removeAttribute(CURRENT_STORE_ID);
            throw new AccessDeniedException("The saved store context is no longer available");
        }
        return new AuthController.ContextView(storeId);
    }

    public AuthController.ContextView set(HttpServletRequest request, Authentication authentication, Long storeId) {
        if (!tenantAccessService.hasStoreAccess(authentication, storeId)) {
            throw new AccessDeniedException("You do not have access to this store context");
        }
        request.getSession(true).setAttribute(CURRENT_STORE_ID, storeId);
        return new AuthController.ContextView(storeId);
    }
}
