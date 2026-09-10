package com.quickcustomer.billing

import android.app.Application
import com.quickcustomer.billing.data.AppDatabase
import com.quickcustomer.billing.data.BillingRepository
import com.quickcustomer.billing.sync.DriveSyncManager

class QuickCustomerApplication : Application() {
    val repository: BillingRepository by lazy {
        BillingRepository(AppDatabase.get(this))
    }

    val driveSyncManager: DriveSyncManager by lazy {
        DriveSyncManager(this, repository)
    }
}
