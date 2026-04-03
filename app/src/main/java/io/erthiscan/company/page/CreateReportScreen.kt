package io.erthiscan.company.page

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.erthiscan.api.ApiClient
import io.erthiscan.api.CreateReportRequest
import kotlinx.coroutines.launch

@Composable
fun CreateReportScreen(
    companyId: Int,
    companyName: String,
    parentId: Int? = null,
    onBack: () -> Unit,
    onSubmitted: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val activity = LocalContext.current as ComponentActivity
    val scope = activity.lifecycleScope
    var text by remember { mutableStateOf("") }
    var sourceUrl by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val title = if (parentId != null) "Challenge Report" else "Add Report"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                color = colorScheme.onBackground,
                fontSize = 24.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onBack() }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = companyName,
            color = colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            // Text area
            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= 150) text = it },
                textStyle = TextStyle(
                    color = colorScheme.onSurface,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.surfaceContainerHigh)
                    .padding(16.dp),
                decorationBox = { innerTextField ->
                    if (text.isEmpty()) {
                        Text(
                            text = "Write your report here...",
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${text.length}/150",
                color = colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Source URL
            BasicTextField(
                value = sourceUrl,
                onValueChange = { sourceUrl = it },
                textStyle = TextStyle(
                    color = colorScheme.onSurface,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(colorScheme.primary),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.surfaceContainerHigh)
                    .padding(16.dp),
                decorationBox = { innerTextField ->
                    if (sourceUrl.isEmpty()) {
                        Text(
                            text = "Source URL...",
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error!!,
                    color = colorScheme.error,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (text.isBlank()) {
                        error = "Report text is required"
                        return@Button
                    }
                    if (sourceUrl.isBlank()) {
                        error = "Source URL is required"
                        return@Button
                    }
                    submitting = true
                    error = null
                    scope.launch {
                        try {
                            ApiClient.api.createReport(
                                CreateReportRequest(
                                    companyId = companyId,
                                    text = text.trim(),
                                    sources = listOf(sourceUrl.trim()),
                                    parentId = parentId
                                )
                            )
                            onSubmitted()
                        } catch (e: Exception) {
                            Log.e("ErthiScan", "Failed to create report", e)
                            error = "Failed to submit report"
                            submitting = false
                        }
                    }
                },
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (submitting) "Submitting..." else "Submit",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
