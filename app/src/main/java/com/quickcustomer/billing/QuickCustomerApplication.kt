package com.quickcustomer.billing

import android.app.Application
import com.quickcustomer.billing.data.AppDatabase
import com.quickcustomer.billing.data.BillingRepository

class QuickCustomerApplication : Application() {
    val repository: BillingRepository by lazy {
        BillingRepository(AppDatabase.get(this))
    }
}
