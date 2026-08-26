package com.chaiduniya.billing.domain

import com.chaiduniya.billing.data.UserRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessPolicyTest {
    @Test
    fun superUserHasEveryPermission() {
        AppPermission.entries.forEach { permission ->
            assertTrue(AccessPolicy.allows(UserRole.SUPER_USER, permission))
        }
    }

    @Test
    fun adminCannotManageUsersOrOwnership() {
        assertTrue(AccessPolicy.allows(UserRole.ADMIN, AppPermission.MANAGE_PRODUCTS))
        assertTrue(AccessPolicy.allows(UserRole.ADMIN, AppPermission.VIEW_ALL_SALES))
        assertFalse(AccessPolicy.allows(UserRole.ADMIN, AppPermission.MANAGE_USERS))
        assertFalse(AccessPolicy.allows(UserRole.ADMIN, AppPermission.MANAGE_SHOP))
    }

    @Test
    fun employeeCanOnlyCreateBills() {
        assertTrue(AccessPolicy.allows(UserRole.EMPLOYEE, AppPermission.CREATE_BILL))
        AppPermission.entries.filterNot { it == AppPermission.CREATE_BILL }.forEach { permission ->
            assertFalse(AccessPolicy.allows(UserRole.EMPLOYEE, permission))
        }
    }
}
