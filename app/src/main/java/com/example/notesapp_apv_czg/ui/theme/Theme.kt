package com.example.notesapp_apv_czg.ui.theme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NotesAppAPVCZGTheme(
    content: @Composable () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current

    LaunchedEffect(context) {
        ThemeManager.load(context)
    }

    AnimatedContent(
        targetState = Pair(ThemeManager.getCurrentScheme().name, darkTheme),
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "theme_transition"
    ) { (_, isDark) ->
        MaterialTheme(
            colorScheme = ThemeManager.getColorScheme(isDark),
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
