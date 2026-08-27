package com.chaiduniya.billing.domain

import com.chaiduniya.billing.data.UserRole

enum class AppPermission {
    CREATE_BILL,
    VIEW_ALL_SALES,
    CANCEL_COMPLETED_BILL,
    MANAGE_PRODUCTS,
    MANAGE_USERS,
    MANAGE_SHOP,
    CONFIGURE_PRINTER
}

object AccessPolicy {
    fun allows(role: UserRole, permission: AppPermission): Boolean = when (role) {
        UserRole.SUPER_USER -> true
        UserRole.ADMIN -> permission in setOf(
            AppPermission.CREATE_BILL,
            AppPermission.VIEW_ALL_SALES,
            AppPermission.CANCEL_COMPLETED_BILL,
            AppPermission.MANAGE_PRODUCTS,
            AppPermission.CONFIGURE_PRINTER
        )
        UserRole.EMPLOYEE -> permission == AppPermission.CREATE_BILL
    }
}
