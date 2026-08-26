package com.chaiduniya.billing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chaiduniya.billing.ui.BillingApp
import com.chaiduniya.billing.ui.BillingViewModel
import com.chaiduniya.billing.ui.ChaiDuniyaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChaiDuniyaTheme {
                val billingViewModel: BillingViewModel = viewModel()
                BillingApp(billingViewModel)
            }
        }
    }
}
