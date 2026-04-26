package io.erthiscan.company.page

import io.erthiscan.R
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CreateReportScreen(
    companyName: String,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    vm: CreateReportViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(state.submitted) { if (state.submitted) onSubmitted() }

    BackHandler { onBack() }

    val title = when {
        vm.editReportId != null -> stringResource(R.string.edit_report)
        vm.parentId != null -> stringResource(R.string.challenge_report)
        else -> stringResource(R.string.add_report)
    }

    Column(Modifier.fillMaxSize().background(colorScheme.background).systemBarsPadding()) {
        Text(title, color = colorScheme.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp))
        Text(companyName, color = colorScheme.onSurfaceVariant, fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(16.dp))

        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            BasicTextField(
                value = state.text,
                onValueChange = { vm.onText(it) },
                textStyle = TextStyle(color = colorScheme.onSurface, fontSize = 16.sp),
                cursorBrush = SolidColor(colorScheme.primary),
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .clip(RoundedCornerShape(16.dp)).background(colorScheme.surfaceContainerHigh).padding(16.dp),
                decorationBox = { inner ->
                    if (state.text.isEmpty()) Text(stringResource(R.string.write_report_hint),
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 16.sp)
                    inner()
                }
            )
            Spacer(Modifier.height(8.dp))
            Text("${state.text.length}/150", color = colorScheme.onSurfaceVariant, fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End))
            Spacer(Modifier.height(8.dp))

            BasicTextField(
                value = state.source,
                onValueChange = { vm.onSource(it) },
                textStyle = TextStyle(color = colorScheme.onSurface, fontSize = 16.sp),
                cursorBrush = SolidColor(colorScheme.primary),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)).background(colorScheme.surfaceContainerHigh).padding(16.dp),
                decorationBox = { inner ->
                    if (state.source.isEmpty()) Text(stringResource(R.string.source_url_hint),
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 16.sp)
                    inner()
                }
            )

            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it.asComposableString(), color = colorScheme.error, fontSize = 14.sp)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { vm.submit() },
                enabled = !state.submitting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                ),
            ) {
                Text(if (state.submitting) stringResource(R.string.submitting) else stringResource(R.string.submit),
                    fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}