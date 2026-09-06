package com.ddev.tindakart;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class TenantContextService {
    private static final String CURRENT_VENDOR_ID = "CURRENT_VENDOR_ID";
    private static final String CURRENT_STORE_ID = "CURRENT_STORE_ID";

    private final TenantAccessService tenantAccessService;

    public TenantContextService(TenantAccessService tenantAccessService) {
        this.tenantAccessService = tenantAccessService;
    }

    public AuthController.ContextView resolve(HttpServletRequest request, Authentication authentication) {
        HttpSession session = request.getSession(false);
        if (session == null) return new AuthController.ContextView(null, null);
        Long vendorId = (Long) session.getAttribute(CURRENT_VENDOR_ID);
        Long storeId = (Long) session.getAttribute(CURRENT_STORE_ID);
        if (vendorId == null && storeId == null) return new AuthController.ContextView(null, null);
        if (vendorId == null || storeId == null || !hasAccess(authentication, vendorId, storeId)) {
            session.removeAttribute(CURRENT_VENDOR_ID);
            session.removeAttribute(CURRENT_STORE_ID);
            throw new AccessDeniedException("The saved vendor/store context is no longer available");
        }
        return new AuthController.ContextView(vendorId, storeId);
    }

    public AuthController.ContextView set(HttpServletRequest request, Authentication authentication, Long vendorId, Long storeId) {
        if (!hasAccess(authentication, vendorId, storeId)) {
            throw new AccessDeniedException("You do not have access to this vendor/store context");
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(CURRENT_VENDOR_ID, vendorId);
        session.setAttribute(CURRENT_STORE_ID, storeId);
        return new AuthController.ContextView(vendorId, storeId);
    }

    private boolean hasAccess(Authentication authentication, Long vendorId, Long storeId) {
        return tenantAccessService.storeBelongsToVendor(vendorId, storeId)
                && (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))
                || tenantAccessService.hasStoreAccess(authentication, storeId));
    }
}
