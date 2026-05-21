package com.ddev.TindaKart;

/**
 * Contact and credit details shown in the debt customer popup.
 */
public record DebtCustomerProfile(
        String fullName,
        String phone,
        String email,
        int creditScore,
        String address,
        String notes) {
}
