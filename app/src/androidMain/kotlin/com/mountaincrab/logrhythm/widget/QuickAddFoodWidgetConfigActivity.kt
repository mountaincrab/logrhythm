package com.mountaincrab.logrhythm.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.logrhythm.data.local.entity.FoodItemEntity
import com.mountaincrab.logrhythm.ui.theme.LocalAppPalette
import com.mountaincrab.logrhythm.ui.theme.LogRhythmTheme
import com.mountaincrab.logrhythm.ui.theme.ThemeViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel as koinActivityViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Picks what one home-screen tile logs: a catalogue item, and how many servings.
 *
 * The launcher starts this when a widget is dropped, and again on reconfigure. Nothing here
 * edits the food definition — its icon, name and serving size are the catalogue's, so the only
 * choices a widget owns are which item it points at and the quantity a tap records.
 */
class QuickAddFoodWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private val viewModel: QuickAddWidgetConfigViewModel by koinActivityViewModel()

    // The widget is already saved by the time this dialog goes up, so the answer only decides
    // whether the confirmation arrives as a banner as well as on the tile.
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // The launcher reads RESULT_CANCELED as "don't place it", so it is the correct state
        // for a back-press and the only safe default until the user has actually chosen.
        setResult(RESULT_CANCELED, resultIntent())

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = koinViewModel()
            val appTheme by themeViewModel.appTheme.collectAsStateWithLifecycle()
            LogRhythmTheme(appTheme = appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    QuickAddWidgetConfigScreen(
                        viewModel = viewModel,
                        appWidgetId = appWidgetId,
                        onCancel = ::finish,
                        onSave = ::save,
                    )
                }
            }
        }
    }

    private fun save(foodItemId: String, quantity: Double) {
        lifecycleScope.launch {
            viewModel.save(appWidgetId, foodItemId, quantity)
            setResult(RESULT_OK, resultIntent())

            // Asked here because here is where it is earned: the user has just built the one
            // feature the permission serves. Once only — see [QuickAddNotificationPermission].
            if (QuickAddNotificationPermission.isNeeded(this@QuickAddFoodWidgetConfigActivity) &&
                !viewModel.isNotificationPromptShown()
            ) {
                viewModel.markNotificationPromptShown()
                requestNotificationPermission.launch(QuickAddNotificationPermission.PERMISSION)
            } else {
                finish()
            }
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

    companion object {
        /** Opens this screen for an already-placed widget — the route back for a tile that
         *  lost its config, and what the tile's own tap falls through to. */
        fun reconfigureIntent(context: Context, appWidgetId: Int): Intent =
            Intent(context, QuickAddFoodWidgetConfigActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
}

@Composable
private fun QuickAddWidgetConfigScreen(
    viewModel: QuickAddWidgetConfigViewModel,
    appWidgetId: Int,
    onCancel: () -> Unit,
    onSave: (String, Double) -> Unit,
) {
    val palette = LocalAppPalette.current
    val items by viewModel.foodItems.collectAsStateWithLifecycle()
    var selectedId by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableStateOf(1.0) }
    var isReconfigure by remember { mutableStateOf(false) }

    LaunchedEffect(appWidgetId) {
        viewModel.existingConfig(appWidgetId)?.let { existing ->
            selectedId = existing.foodItemId
            quantity = existing.quantity
            isReconfigure = true
        }
    }

    // A widget is saved against whichever profile is active, so reconfiguring one under a
    // different profile must not keep a selection that profile's catalogue cannot resolve.
    LaunchedEffect(items, selectedId) {
        val chosen = selectedId
        if (chosen != null && items.isNotEmpty() && items.none { it.id == chosen }) {
            selectedId = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text("Quick add widget", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.size(4.dp))
        Text(
            "One tap logs this at the time you tap it.",
            color = palette.fgMuted,
            fontSize = 13.sp,
        )
        Spacer(Modifier.size(16.dp))

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "Add a food item in the app's Food library first.",
                    color = palette.fgMuted,
                    fontSize = 14.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    FoodItemRow(
                        item = item,
                        selected = item.id == selectedId,
                        onClick = { selectedId = item.id },
                    )
                }
            }
            Spacer(Modifier.size(16.dp))
            QuantityStepper(
                quantity = quantity,
                servingLabel = items.firstOrNull { it.id == selectedId }
                    ?.let { "${it.amount} ${it.unit}".trim() },
                onChange = { quantity = it },
            )
        }

        Spacer(Modifier.size(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel", color = palette.fgMuted)
            }
            Button(
                onClick = { selectedId?.let { onSave(it, quantity) } },
                enabled = selectedId != null,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (isReconfigure) "Save" else "Add widget", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FoodItemRow(
    item: FoodItemEntity,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) palette.accentSoft else palette.surfaceRaised)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else palette.border,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(item.icon, fontSize = 24.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("${item.amount} ${item.unit}".trim(), color = palette.fgMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Double,
    servingLabel: String?,
    onChange: (Double) -> Unit,
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Quantity", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (servingLabel != null) {
                Text(
                    "${formatQuantity(quantity)} × $servingLabel",
                    color = palette.fgMuted,
                    fontSize = 12.sp,
                )
            }
        }
        StepperButton(label = "−", enabled = quantity > MIN_QUANTITY) {
            onChange((quantity - QUANTITY_STEP).coerceAtLeast(MIN_QUANTITY))
        }
        Text(
            formatQuantity(quantity),
            modifier = Modifier.padding(horizontal = 14.dp),
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        StepperButton(label = "+", enabled = quantity < MAX_QUANTITY) {
            onChange((quantity + QUANTITY_STEP).coerceAtMost(MAX_QUANTITY))
        }
    }
}

@Composable
private fun StepperButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(44.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(
            label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) palette.accentText else palette.fgDisabled,
        )
    }
}

private const val MIN_QUANTITY = 0.5
private const val MAX_QUANTITY = 20.0
private const val QUANTITY_STEP = 0.5
