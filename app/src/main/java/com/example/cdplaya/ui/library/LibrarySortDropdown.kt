package com.example.cdplaya.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun LibrarySortDropdown(
    selectedOption: LibrarySortOption,
    options: List<LibrarySortOption>,
    onOptionSelected: (LibrarySortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = {
                isExpanded = true
            }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = "Sort by ${selectedOption.title}"
            )
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = {
                isExpanded = false
            }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(text = option.title)
                    },
                    leadingIcon = {
                        if (selectedOption == option) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected"
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
