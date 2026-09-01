package com.quickcustomer.billing.domain

import com.quickcustomer.billing.data.UserRole

enum class AppPermission {
    CREATE_BILL,
    VIEW_ALL_SALES,
    CANCEL_COMPLETED_BILL,
    MANAGE_PRODUCTS,
    MANAGE_USERS,
    MANAGE_SHOP,
    CONFIGURE_PRINTER,
    ADD_EXPENSE,
    VIEW_EXPENSES,
    APPROVE_EXPENSES,
    VIEW_INVENTORY,
    MANAGE_INVENTORY
}

object AccessPolicy {
    fun allows(role: UserRole, permission: AppPermission): Boolean = when (role) {
        UserRole.SUPER_USER -> true
        UserRole.ADMIN -> permission in setOf(
            AppPermission.CREATE_BILL,
            AppPermission.VIEW_ALL_SALES,
            AppPermission.CANCEL_COMPLETED_BILL,
            AppPermission.MANAGE_PRODUCTS,
            AppPermission.CONFIGURE_PRINTER,
            AppPermission.ADD_EXPENSE,
            AppPermission.VIEW_EXPENSES,
            AppPermission.APPROVE_EXPENSES,
            AppPermission.VIEW_INVENTORY,
            AppPermission.MANAGE_INVENTORY
        )
        UserRole.EMPLOYEE -> permission in setOf(
            AppPermission.CREATE_BILL,
            AppPermission.ADD_EXPENSE,
            AppPermission.VIEW_EXPENSES,
            AppPermission.VIEW_INVENTORY
        )
    }
}
