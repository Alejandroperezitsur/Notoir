package com.example.notesapp_apv_czg.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object ColorTokens {
    val success: Color
        @Composable get() = MaterialTheme.colorScheme.tertiary

    val warning: Color
        @Composable get() = MaterialTheme.colorScheme.tertiaryContainer

    val info: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    val danger: Color
        @Composable get() = MaterialTheme.colorScheme.error

    val accent: Color
        @Composable get() = MaterialTheme.colorScheme.secondary

    val taskCompleted: Color
        @Composable get() = MaterialTheme.colorScheme.secondary

    val favorite: Color
        @Composable get() = Color(0xFFFFD700)
}

