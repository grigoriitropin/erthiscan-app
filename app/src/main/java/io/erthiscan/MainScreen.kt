package io.erthiscan

import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.erthiscan.company.CompaniesScreen
import io.erthiscan.profile.ProfileScreen
import io.erthiscan.scan.ScanScreen
import io.erthiscan.scan.TabBar

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(1) }
    val vibrator = (LocalContext.current.getSystemService(VibratorManager::class.java))
        .defaultVibrator

    Box(modifier = Modifier.fillMaxSize()) {
        when (selectedTab) {
            0 -> CompaniesScreen()
            1 -> ScanScreen()
            2 -> ProfileScreen()
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
