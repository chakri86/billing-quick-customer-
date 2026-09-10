package com.quickcustomer.billing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quickcustomer.billing.ui.BillingApp
import com.quickcustomer.billing.ui.BillingViewModel
import com.quickcustomer.billing.ui.QuickCustomerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuickCustomerTheme {
                val billingViewModel: BillingViewModel = viewModel()
                BillingApp(billingViewModel)
            }
        }
    }
}
