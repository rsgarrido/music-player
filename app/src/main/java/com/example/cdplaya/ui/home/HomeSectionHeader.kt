package com.example.cdplaya.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.cdplaya.ui.AppShellTypography

@Composable
fun HomeSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        style = AppShellTypography.SectionTitle,
        color = MaterialTheme.colorScheme.onBackground
    )
}
