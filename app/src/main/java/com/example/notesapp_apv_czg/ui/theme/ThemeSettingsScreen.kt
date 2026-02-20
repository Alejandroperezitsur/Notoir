package com.example.notesapp_apv_czg.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.notesapp_apv_czg.R

// Definir esquemas de colores predefinidos
data class ColorSchemeOption(
    val name: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val surface: Color,
    val background: Color
)

val predefinedSchemes = listOf(
    ColorSchemeOption(
        "Por defecto",
        primary = Color(0xFF6200EE),
        secondary = Color(0xFF03DAC6),
        tertiary = Color(0xFF6B38FB),
        surface = Color(0xFFFFFFFF),
        background = Color(0xFFF5F5F5)
    ),
    ColorSchemeOption(
        "Oscuro",
        primary = Color(0xFFD0BCFF),
        secondary = Color(0xFF9FD8E6),
        tertiary = Color(0xFFB4A0FF),
        surface = Color(0xFF1C1B1F),
        background = Color(0xFF000000)
    ),
    ColorSchemeOption(
        "Nature",
        primary = Color(0xFF4CAF50),
        secondary = Color(0xFF8BC34A),
        tertiary = Color(0xFF009688),
        surface = Color(0xFFF1F8E9),
        background = Color(0xFFE8F5E9)
    ),
    ColorSchemeOption(
        "Ocean",
        primary = Color(0xFF2196F3),
        secondary = Color(0xFF03A9F4),
        tertiary = Color(0xFF00BCD4),
        surface = Color(0xFFE3F2FD),
        background = Color(0xFFE1F5FE)
    ),
    ColorSchemeOption(
        "Sunset",
        primary = Color(0xFFFF9800),
        secondary = Color(0xFFFF5722),
        tertiary = Color(0xFFF44336),
        surface = Color(0xFFFFF3E0),
        background = Color(0xFFFBE9E7)
    ),
    ColorSchemeOption(
        "Royal Purple",
        primary = Color(0xFF673AB7),
        secondary = Color(0xFF9C27B0),
        tertiary = Color(0xFF7E57C2),
        surface = Color(0xFFEDE7F6),
        background = Color(0xFFF3E5F5)
    ),
    ColorSchemeOption(
        "Mint",
        primary = Color(0xFF00796B),
        secondary = Color(0xFF26A69A),
        tertiary = Color(0xFF00897B),
        surface = Color(0xFFE0F2F1),
        background = Color(0xFFE0F7FA)
    ),
    ColorSchemeOption(
        "Rose Gold",
        primary = Color(0xFFE91E63),
        secondary = Color(0xFFF06292),
        tertiary = Color(0xFFFF80AB),
        surface = Color(0xFFFCE4EC),
        background = Color(0xFFFFF0F4)
    ),
    ColorSchemeOption(
        "Autumn",
        primary = Color(0xFFE65100),
        secondary = Color(0xFFF57C00),
        tertiary = Color(0xFFFF9800),
        surface = Color(0xFFFFF3E0),
        background = Color(0xFFFFF8E1)
    ),
    ColorSchemeOption(
        "Nordic",
        primary = Color(0xFF37474F),
        secondary = Color(0xFF546E7A),
        tertiary = Color(0xFF455A64),
        surface = Color(0xFFECEFF1),
        background = Color(0xFFFAFAFA)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onNavigateUp: () -> Unit,
    currentScheme: ColorSchemeOption,
    onSchemeSelected: (ColorSchemeOption) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personalización del tema") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Elige un esquema de colores",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(predefinedSchemes) { scheme ->
                    ColorSchemePreview(
                        scheme = scheme,
                        isSelected = scheme == currentScheme,
                        onClick = { onSchemeSelected(scheme) }
                    )
                }
            }
        }
    }
}

@Composable
fun ColorSchemePreview(
    scheme: ColorSchemeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Nombre del esquema
            Text(
                text = scheme.name,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Preview de colores
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ColorPreviewDot(color = scheme.primary, modifier = Modifier.weight(1f))
                ColorPreviewDot(color = scheme.secondary, modifier = Modifier.weight(1f))
                ColorPreviewDot(color = scheme.tertiary, modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Preview del fondo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(scheme.background)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Seleccionado",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ColorPreviewDot(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(color)
    )
}
