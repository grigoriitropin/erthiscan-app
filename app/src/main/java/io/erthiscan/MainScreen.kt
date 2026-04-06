package io.erthiscan

import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.erthiscan.api.ScanResponse
import io.erthiscan.company.CompaniesScreen
import io.erthiscan.company.page.CompanyPage
import io.erthiscan.profile.ProfileScreen
import io.erthiscan.scan.ProductSheet
import io.erthiscan.scan.ScanScreen
import io.erthiscan.scan.TabBar

enum class CompanySource { LIST, SCAN, PROFILE }

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(1) }
    var openCompanyId by remember { mutableStateOf<Int?>(null) }
    var companySource by remember { mutableStateOf(CompanySource.LIST) }
    var savedScanResult by remember { mutableStateOf<ScanResponse?>(null) }
    var profileShowReports by remember { mutableStateOf(false) }
    val vibrator = (LocalContext.current.getSystemService(VibratorManager::class.java))
        .defaultVibrator

    BackHandler(enabled = openCompanyId != null) {
        openCompanyId = null
    }

    if (openCompanyId != null) {
        CompanyPage(
            companyId = openCompanyId!!,
            onBack = { openCompanyId = null }
        )
        return
    }

    // Show saved ProductSheet after returning from CompanyPage
    if (savedScanResult != null && selectedTab == 1) {
        val result = savedScanResult!!
        ProductSheet(
            productName = result.product.name,
            companyName = result.company.name,
            companyId = result.company.id,
            ethicalScore = result.company.ethicalScore,
            hasReports = result.company.reportCount > 0,
            openFactsUrl = result.product.openFactsUrl,
            barcode = result.product.barcode,
            onDismiss = { savedScanResult = null },
            onViewCompany = {
                companySource = CompanySource.SCAN
                openCompanyId = it
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (selectedTab) {
            0 -> CompaniesScreen(onCompanyClick = {
                companySource = CompanySource.LIST
                openCompanyId = it
            })
            1 -> ScanScreen(onViewCompany = { scanResult, companyId ->
                savedScanResult = scanResult
                companySource = CompanySource.SCAN
                openCompanyId = companyId
            })
            2 -> ProfileScreen(
                showReports = profileShowReports,
                onShowReportsChange = { profileShowReports = it },
                onCompanyClick = {
                    companySource = CompanySource.PROFILE
                    openCompanyId = it
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            TabBar(selectedTab = selectedTab, onTabSelected = {
                vibrator.vibrate(VibrationEffect.createOneShot(30, 50))
                selectedTab = it
            })
        }
    }
}
