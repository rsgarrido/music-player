package com.example.cdplaya.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cdplaya.R
import com.example.cdplaya.ui.AppShellIconButton
import com.example.cdplaya.ui.AppShellAccent

@Composable
fun LibrarySortDropdown(
    selectedOption: LibrarySortOption,
    options: List<LibrarySortOption>,
    onOptionSelected: (LibrarySortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val selectedTitle = selectedOption.displayTitle()

    Box(modifier = modifier) {
        AppShellIconButton(
            onClick = {
                isExpanded = true
            },
            imageVector = Icons.AutoMirrored.Filled.Sort,
            contentDescription = "Sort by $selectedTitle",
            accented = true
        )

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = {
                isExpanded = false
            }
        ) {
            options.forEach { option ->
                val optionTitle = option.displayTitle()
                DropdownMenuItem(
                    text = {
                        Text(text = optionTitle)
                    },
                    leadingIcon = {
                        if (selectedOption == option) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = AppShellAccent
                            )
                        }
                    },
                    onClick = {
                        onOptionSelected(option)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LibrarySortOption.displayTitle(): String {
    return if (this == LibrarySortOption.DATE_ADDED) {
        stringResource(R.string.sort_date_added)
    } else {
        title
    }
}
