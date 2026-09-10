package com.quickcustomer.billing.domain

import com.quickcustomer.billing.data.UserRole
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
        assertTrue(AccessPolicy.allows(UserRole.ADMIN, AppPermission.CANCEL_COMPLETED_BILL))
        assertFalse(AccessPolicy.allows(UserRole.ADMIN, AppPermission.MANAGE_USERS))
        assertFalse(AccessPolicy.allows(UserRole.ADMIN, AppPermission.MANAGE_SHOP))
    }

    @Test
    fun employeeCanBillAddExpensesAndViewStock() {
        assertTrue(AccessPolicy.allows(UserRole.EMPLOYEE, AppPermission.CREATE_BILL))
        assertTrue(AccessPolicy.allows(UserRole.EMPLOYEE, AppPermission.ADD_EXPENSE))
        assertTrue(AccessPolicy.allows(UserRole.EMPLOYEE, AppPermission.VIEW_EXPENSES))
        assertTrue(AccessPolicy.allows(UserRole.EMPLOYEE, AppPermission.VIEW_INVENTORY))
        AppPermission.entries.filterNot {
            it in setOf(AppPermission.CREATE_BILL, AppPermission.ADD_EXPENSE, AppPermission.VIEW_EXPENSES, AppPermission.VIEW_INVENTORY)
        }.forEach { permission ->
            assertFalse(AccessPolicy.allows(UserRole.EMPLOYEE, permission))
        }
    }
}
