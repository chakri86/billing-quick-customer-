package com.chaiduniya.billing

import android.app.Application
import com.chaiduniya.billing.data.AppDatabase
import com.chaiduniya.billing.data.BillingRepository

class ChaiDuniyaApplication : Application() {
    val repository: BillingRepository by lazy {
        BillingRepository(AppDatabase.get(this))
    }
}
