package com.mountaincrab.logrhythm.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.logrhythm.data.model.StoolSystem
import com.mountaincrab.logrhythm.ui.components.BottomTabBar
import com.mountaincrab.logrhythm.ui.navigation.Screen
import com.mountaincrab.logrhythm.ui.theme.AppTheme
import com.mountaincrab.logrhythm.ui.theme.LocalAppPalette
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onTabSelect: (route: String) -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val palette = LocalAppPalette.current
    val theme by viewModel.appTheme.collectAsStateWithLifecycle()
    val stoolSystem by viewModel.stoolSystem.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
        ) {
            Text("Settings",
                fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
                color = MaterialTheme.colorScheme.onBackground)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
        ) {
            SectionLabel("Stool type system")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surfaceRaised)
                    .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                    .padding(12.dp),
            ) {
                StoolSystem.entries.forEachIndexed { i, system ->
                    StoolSystemRow(
                        system = system,
                        selected = system == stoolSystem,
                        onClick = { viewModel.setStoolSystem(system) },
                    )
                    if (i < StoolSystem.entries.size - 1) Divider(palette.borderSubtle)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))

            SectionLabel("Theme")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surfaceRaised)
                    .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppTheme.entries.forEach { t ->
                    ThemeSwatch(
                        theme = t,
                        selected = t == theme,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setTheme(t) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))

            SectionLabel("Reminders")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surfaceRaised)
                    .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                    .padding(16.dp),
            ) {
                Text("Coming soon", color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Daily check-in, evening review, and missed-meds nudges will land here.",
                    color = palette.fgMuted, fontSize = 12.sp, lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(modifier = Modifier.height(18.dp))

            SectionLabel("Data")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surfaceRaised)
                    .border(1.dp, palette.border, RoundedCornerShape(14.dp)),
            ) {
                DataRow("Export to CSV", enabled = false)
                Divider(palette.borderSubtle)
                DataRow("Import from spreadsheet", enabled = false)
                Divider(palette.borderSubtle)
                DataRow("Share with my clinician", enabled = false)
            }
        }

        BottomTabBar(active = Screen.Settings.route, onSelect = onTabSelect)
    }
}

@Composable
private fun SectionLabel(text: String) {
    val palette = LocalAppPalette.current
    Text(
        text = text.uppercase(),
        color = palette.fgMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun StoolSystemRow(system: StoolSystem, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
                .border(
                    2.dp,
                    if (selected) MaterialTheme.colorScheme.primary else palette.fgFaint,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(system.displayName, color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(system.sub, color = palette.fgMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ThemeSwatch(theme: AppTheme, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    val (bg, surf, accent) = when (theme) {
        AppTheme.DEEP_NAVY -> Triple(Color(0xFF0A1020), Color(0xFF1C2340), Color(0xFF4F7CFF))
        AppTheme.CHARCOAL  -> Triple(Color(0xFF0A0A0A), Color(0xFF1E1E1E), Color(0xFF06B6D4))
        AppTheme.RETRO     -> Triple(Color(0xFF1A0B1E), Color(0xFF2E1438), Color(0xFFFF00CC))
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .border(
                    if (selected) 2.dp else 1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else palette.border,
                    RoundedCornerShape(12.dp),
                ),
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 6.dp, end = 6.dp, top = 6.dp)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(surf),
            )
            Box(
                modifier = Modifier
                    .padding(start = 6.dp, end = 6.dp, top = 20.dp, bottom = 6.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
                    .background(surf.copy(alpha = 0.85f)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 10.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = theme.displayName,
            color = if (selected) palette.accentText else palette.fgMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DataRow(label: String, enabled: Boolean) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f),
            color = if (enabled) MaterialTheme.colorScheme.onSurface else palette.fgMuted,
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(if (enabled) "" else "soon", color = palette.fgFaint, fontSize = 11.sp)
    }
}

@Composable
private fun Divider(color: Color) {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color))
}
