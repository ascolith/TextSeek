package com.ascolith.textseek.feature.camerascreen.ui

import android.Manifest
import android.content.res.Configuration
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.setFrom
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.ascolith.textseek.core.composable.TsLoadingScreen
import com.ascolith.textseek.core.composable.TsCameraPermissionContent
import com.ascolith.textseek.core.composable.TsErrorScreen
import com.ascolith.textseek.core.composable.TsPreview
import com.ascolith.textseek.core.ui.isHeightCompact
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.feature.camerascreen.ui.model.Chip
import com.ascolith.textseek.feature.camerascreen.ui.model.ChipType
import com.ascolith.textseek.feature.camerascreen.ui.model.QuickSearchLaunch
import com.ascolith.textseek.feature.camerascreen.ui.model.ScreenIntent
import com.ascolith.textseek.feature.camerascreen.ui.model.SideEffect
import com.ascolith.textseek.feature.camerascreen.ui.model.MatchOutline
import com.ascolith.textseek.feature.camerascreen.ui.model.TextMatchUiModel
import com.ascolith.textseek.feature.camerascreen.ui.model.TransientScreen
import com.ascolith.textseek.feature.camerascreen.ui.model.TsDialog
import com.ascolith.textseek.resources.R
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

private val QUICK_SEARCH_LABEL_DURATION = 3.seconds

private val MATCH_ANNOUNCEMENT_SETTLE_DURATION = 1.seconds

/** Sentinel for "no Quick Search launch handled yet"; no real launch id uses it. */
private const val NO_LAUNCH_HANDLED = 0L

private val MATCH_OUTLINE_WIDTH = 2.dp

/** Matches the translucent buttons over the preview. */
private const val OVERLAY_ALPHA = 0.7f

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onSearchSettingEditClicked: (id: SearchSettingId) -> Unit,
    onSearchGroupEditClicked: (id: SearchGroupId) -> Unit,
    modifier: Modifier = Modifier,
    quickSearchLaunch: QuickSearchLaunch? = null,
    viewModel: CameraScreenViewModel = hiltViewModel(),
) {
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    viewModel.collectSideEffect {
        when (it) {
            is SideEffect.NavigateToSearchSettingEdit -> onSearchSettingEditClicked(it.id)
            is SideEffect.NavigateToSearchGroupEdit -> onSearchGroupEditClicked(it.id)
        }
    }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    if (!cameraPermissionState.status.isGranted) {
        TsCameraPermissionContent(
            showRationale = cameraPermissionState.status.shouldShowRationale,
            onGrantClick = { cameraPermissionState.launchPermissionRequest() },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    val screenState by viewModel.collectAsState()
    val textMatches by viewModel.textMatches.collectAsState()
    LaunchedEffect(lifecycleOwner) {
        viewModel.bindToCamera(lifecycleOwner)
    }

    var lastHandledQuickSearchLaunchId by rememberSaveable { mutableLongStateOf(NO_LAUNCH_HANDLED) }
    LaunchedEffect(quickSearchLaunch) {
        if (quickSearchLaunch != null && quickSearchLaunch.launchId != lastHandledQuickSearchLaunchId) {
            lastHandledQuickSearchLaunchId = quickSearchLaunch.launchId
            val query = quickSearchLaunch.query?.trim()
            val intent = if (query.isNullOrEmpty()) {
                ScreenIntent.QuickSearch.Clicked
            } else {
                ScreenIntent.QuickSearch.StartDirect(
                    query = query,
                    caseSensitive = quickSearchLaunch.caseSensitive,
                    colorArgb = quickSearchLaunch.colorArgb,
                )
            }
            viewModel.handleIntent(intent)
        }
    }

    Box(modifier = modifier) {
        when (screenState.transientScreen) {
            is TransientScreen.Loading ->
                TsLoadingScreen(modifier = Modifier.fillMaxSize())

            is TransientScreen.Error ->
                TsErrorScreen(modifier = Modifier.fillMaxSize())

            is TransientScreen.None -> {
                val surfaceRequest = screenState.cameraState.surfaceRequest
                if (surfaceRequest == null) {
                    // The bind is under way but the viewfinder has no surface yet.
                    TsLoadingScreen(modifier = Modifier.fillMaxSize())
                } else {
                    val onChipClicked = remember<(Chip) -> Unit> {
                        { viewModel.handleIntent(ScreenIntent.SearchChipClicked(it)) }
                    }
                    val onZoom = remember<(Float) -> Unit> { { viewModel.handleIntent(ScreenIntent.ZoomChanged(it)) } }
                    val onTorchToggle = remember { { viewModel.handleIntent(ScreenIntent.TorchToggled) } }
                    val onQuickSearchClicked = remember { { viewModel.handleIntent(ScreenIntent.QuickSearch.Clicked) } }
                    val onQuickSearchStopClicked = remember {
                        { viewModel.handleIntent(ScreenIntent.QuickSearch.Stop) }
                    }
                    CameraScreenContent(
                        textMatches = textMatches,
                        searchChips = screenState.searchChips,
                        surfaceRequest = surfaceRequest,
                        transformationInfo = screenState.cameraState.transformationInfo,
                        zoomLevel = screenState.cameraState.zoomLevel,
                        hasFlashUnit = screenState.cameraState.hasFlashUnit,
                        torchEnabled = screenState.cameraState.torchEnabled,
                        quickSearchTerm = screenState.quickSearch?.text,
                        dialog = screenState.dialog,
                        onSearchChipClicked = onChipClicked,
                        onZoomChanged = onZoom,
                        onTorchToggled = onTorchToggle,
                        onQuickSearchClicked = onQuickSearchClicked,
                        onQuickSearchStopClicked = onQuickSearchStopClicked,
                        onIntent = viewModel::handleIntent,
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraScreenContent(
    textMatches: List<TextMatchUiModel>,
    searchChips: List<Chip>,
    surfaceRequest: SurfaceRequest?,
    transformationInfo: SurfaceRequest.TransformationInfo?,
    zoomLevel: Float,
    hasFlashUnit: Boolean,
    torchEnabled: Boolean,
    quickSearchTerm: String?,
    dialog: TsDialog,
    onSearchChipClicked: (Chip) -> Unit,
    onZoomChanged: (Float) -> Unit,
    onTorchToggled: () -> Unit,
    onQuickSearchClicked: () -> Unit,
    onQuickSearchStopClicked: () -> Unit,
    onIntent: (ScreenIntent.QuickSearch) -> Unit,
    modifier: Modifier = Modifier,
) {
    val compactHeight = isHeightCompact()
    val chipRow: @Composable (Color) -> Unit = { background ->
        SearchSettingsChipRow(
            searchChips = searchChips,
            quickSearchTerm = quickSearchTerm,
            onSearchChipClicked = onSearchChipClicked,
            onQuickSearchStopClicked = onQuickSearchStopClicked,
            background = background,
        )
    }
    val overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = OVERLAY_ALPHA)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // A short window floats the chip row over the preview instead, keeping the preview full height
        if (!compactHeight) {
            chipRow(MaterialTheme.colorScheme.background)
        }

        CameraPreview(
            modifier = Modifier
                .fillMaxSize(),
            surfaceRequest = surfaceRequest,
            transformationInfo = transformationInfo,
            zoomLevel = zoomLevel,
            textMatches = textMatches,
            hasFlashUnit = hasFlashUnit,
            torchEnabled = torchEnabled,
            quickSearchActive = quickSearchTerm != null,
            compactHeight = compactHeight,
            onZoomChanged = onZoomChanged,
            onTorchToggled = onTorchToggled,
            onQuickSearchClicked = onQuickSearchClicked,
            compactTopOverlay = { chipRow(overlayColor) },
        )
    }

    (dialog as? TsDialog.QuickSearch)?.let {
        QuickSearchDialog(
            dialog = it,
            onIntent = onIntent,
        )
    }
}

@Composable
private fun SearchSettingsChipRow(
    searchChips: List<Chip>,
    quickSearchTerm: String?,
    onSearchChipClicked: (Chip) -> Unit,
    onQuickSearchStopClicked: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.background,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (quickSearchTerm != null) {
            Text(
                text = stringResource(R.string.quick_search_active, quickSearchTerm),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(
                onClick = { onQuickSearchStopClicked() },
            ) {
                Text(stringResource(R.string.stop))
            }
        } else if (searchChips.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp, alignment = Alignment.Start)
            ) {
                items(
                    items = searchChips,
                    key = { it.type.key }
                ) { item ->
                    val chipDescription = stringResource(item.contentDescription, item.text)
                    val disabledDescription = stringResource(R.string.disabled)
                    AssistChip(
                        onClick = { onSearchChipClicked(item) },
                        modifier = Modifier.semantics {
                            contentDescription = chipDescription
                            if (!item.isEnabled) stateDescription = disabledDescription
                        },
                        label = {
                            Text(
                                text = item.text,
                                style = if (!item.isEnabled) {
                                    LocalTextStyle.current.copy(textDecoration = TextDecoration.LineThrough)
                                } else {
                                    LocalTextStyle.current
                                },
                            )
                        },
                        leadingIcon = {
                            if (item.leadingIcon != null) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(item.leadingIcon),
                                    tint = item.iconColorArgb?.let { Color(it) } ?: Color.White,
                                    contentDescription = null,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    surfaceRequest: SurfaceRequest?,
    transformationInfo: SurfaceRequest.TransformationInfo?,
    textMatches: List<TextMatchUiModel>,
    zoomLevel: Float,
    hasFlashUnit: Boolean,
    torchEnabled: Boolean,
    quickSearchActive: Boolean,
    compactHeight: Boolean,
    onZoomChanged: (Float) -> Unit,
    onTorchToggled: () -> Unit,
    onQuickSearchClicked: () -> Unit,
    modifier: Modifier = Modifier,
    compactTopOverlay: @Composable () -> Unit = {},
) {
    val coordinateTransformer = remember { MutableCoordinateTransformer() }
    val sensorToBufferMatrix = Matrix().apply {
        transformationInfo?.let { setFrom(it.sensorToBufferTransform) }
    }
    val bufferToUiMatrix = Matrix().apply {
        setFrom(coordinateTransformer.transformMatrix)
        invert()
    }
    val textOutlines = textMatches.map {
        it.copy(outline = it.outline.toUiCoordinates(sensorToBufferMatrix, bufferToUiMatrix))
    }

    var focusIndicator by remember { mutableStateOf<FocusIndicatorState?>(null) }

    Box(modifier = modifier) {
        if (surfaceRequest != null && !LocalInspectionMode.current) {
            CameraXViewfinder(
                modifier = Modifier.fillMaxSize(),
                surfaceRequest = surfaceRequest,
                coordinateTransformer = coordinateTransformer,
                isPinchToZoomEnabled = true,
                isTapToFocusEnabled = true,
                onTapToFocus = { position, focusState ->
                    focusIndicator = FocusIndicatorState(position, focusState)
                },
            )
        } else {
            // Stand-in for the viewfinder in previews.
            Box(
                modifier = Modifier
                    .background(Color.Gray)
                    .fillMaxSize(),
            )
        }

        focusIndicator?.let { indicator ->
            FocusIndicator(
                indicator = indicator,
                onHidden = { focusIndicator = null },
            )
        }

        val settledMatchTexts = rememberSettledMatchTexts(textMatches)
        val matchesDescription = settledMatchTexts.takeIf { it.isNotEmpty() }?.let {
            stringResource(R.string.content_description_matches_found, it.joinToString())
        }
        val reusablePath = remember { Path() }
        Canvas(
            Modifier
                .fillMaxSize()
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    matchesDescription?.let { contentDescription = it }
                },
        ) {
            textOutlines.forEach {
                reusablePath.reset()
                with(it.outline) {
                    reusablePath.moveTo(topLeft.x, topLeft.y)
                    reusablePath.lineTo(topRight.x, topRight.y)
                    reusablePath.lineTo(bottomRight.x, bottomRight.y)
                    reusablePath.lineTo(bottomLeft.x, bottomLeft.y)
                    reusablePath.close()
                }
                drawPath(reusablePath, color = Color(it.color), style = Stroke(MATCH_OUTLINE_WIDTH.toPx()))
            }
        }

        if (compactHeight) {
            CompactHeightControls(
                zoomLevel = zoomLevel,
                hasFlashUnit = hasFlashUnit,
                torchEnabled = torchEnabled,
                quickSearchActive = quickSearchActive,
                onZoomChanged = onZoomChanged,
                onTorchToggled = onTorchToggled,
                onQuickSearchClicked = onQuickSearchClicked,
                topOverlay = compactTopOverlay,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 64.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End,
            ) {
                if (hasFlashUnit) {
                    TorchToggleButton(
                        torchEnabled = torchEnabled,
                        onTorchToggled = onTorchToggled,
                    )
                }

                QuickSearchButton(
                    quickSearchActive = quickSearchActive,
                    onClick = onQuickSearchClicked,
                )
            }

            val zoomContentDescription = stringResource(R.string.content_description_zoom)
            Slider(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .semantics { contentDescription = zoomContentDescription },
                value = zoomLevel,
                valueRange = 0f..1f,
                onValueChange = onZoomChanged,
            )
        }
    }
}

/** The controls in a short window: [topOverlay] across the top, then a vertical zoom and the buttons at the end. */
@Composable
private fun CompactHeightControls(
    zoomLevel: Float,
    hasFlashUnit: Boolean,
    torchEnabled: Boolean,
    quickSearchActive: Boolean,
    onZoomChanged: (Float) -> Unit,
    onTorchToggled: () -> Unit,
    onQuickSearchClicked: () -> Unit,
    topOverlay: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        topOverlay()
        Column(
            modifier = Modifier
                .align(Alignment.End)
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End,
        ) {
            VerticalZoomSlider(
                zoomLevel = zoomLevel,
                onZoomChanged = onZoomChanged,
                // Matches the buttons so it lines up over them
                modifier = Modifier
                    .weight(1f)
                    .width(48.dp),
            )

            if (hasFlashUnit) {
                TorchToggleButton(
                    torchEnabled = torchEnabled,
                    onTorchToggled = onTorchToggled,
                )
            }

            QuickSearchButton(
                quickSearchActive = quickSearchActive,
                onClick = onQuickSearchClicked,
            )
        }
    }
}

/** Zooms in upward. Follows [zoomLevel], so pinch-to-zoom moves the thumb too. */
@Composable
private fun VerticalZoomSlider(
    zoomLevel: Float,
    onZoomChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sliderState = remember { SliderState(value = zoomLevel) }
    LaunchedEffect(zoomLevel) { sliderState.value = zoomLevel }

    val zoomContentDescription = stringResource(R.string.content_description_zoom)
    VerticalSlider(
        state = sliderState,
        onValueChange = onZoomChanged,
        modifier = modifier.semantics { contentDescription = zoomContentDescription },
        topToBottom = false,
    )
}

@Composable
private fun TorchToggleButton(
    torchEnabled: Boolean,
    onTorchToggled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                color = if (torchEnabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                },
            )
            .toggleable(value = torchEnabled, role = Role.Switch, onValueChange = { onTorchToggled() }),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(
                if (torchEnabled) R.drawable.ic_flashlight_on else R.drawable.ic_flashlight_off
            ),
            contentDescription = stringResource(R.string.torch_toggle),
            tint = if (torchEnabled) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun QuickSearchButton(
    quickSearchActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var labelVisible by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(QUICK_SEARCH_LABEL_DURATION)
        labelVisible = false
    }

    val contentColor = if (quickSearchActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val description = stringResource(R.string.content_description_quick_search)
    Row(
        modifier = modifier
            // Keeps the label once the text collapses, without reading it twice while it shows
            .semantics { contentDescription = description }
            .height(48.dp)
            .clip(CircleShape)
            .background(
                color = if (quickSearchActive) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                },
            )
            .clickable(role = Role.Button) { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_search),
            contentDescription = null,
            tint = contentColor,
        )
        AnimatedVisibility(
            visible = labelVisible,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut(),
        ) {
            Text(
                text = stringResource(R.string.quick_search),
                modifier = Modifier.padding(start = 8.dp),
                color = contentColor,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}


/** The matched texts, updated only once they hold still so flickering matches aren't announced every frame. */
@Composable
private fun rememberSettledMatchTexts(textMatches: List<TextMatchUiModel>): List<String> {
    val matchTexts = remember(textMatches) { textMatches.map { it.text }.distinct().sorted() }
    var settled by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(matchTexts) {
        delay(MATCH_ANNOUNCEMENT_SETTLE_DURATION)
        settled = matchTexts
    }
    return settled
}

/** Maps a match's outline from sensor coordinates to viewfinder coordinates. */
private fun MatchOutline.toUiCoordinates(
    sensorToBufferMatrix: Matrix,
    bufferToUiMatrix: Matrix,
): MatchOutline = map { bufferToUiMatrix.map(sensorToBufferMatrix.map(it)) }

private val mockChips = listOf(
    Chip(
        type = ChipType.SearchSetting(id = SearchSettingId(1)),
        text = "First",
        iconColorArgb = Color.Yellow.toArgb(),
        contentDescription = R.string.content_description_search_setting,
        isEnabled = true,
        leadingIcon = R.drawable.ic_text_match_outline,
    ),
    Chip(
        type = ChipType.SearchSetting(id = SearchSettingId(2)),
        text = "Second and disabled too",
        iconColorArgb = Color.Blue.toArgb(),
        contentDescription = R.string.content_description_search_setting,
        isEnabled = false,
    ),
    Chip(
        type = ChipType.SearchSetting(id = SearchSettingId(3)),
        text = "Third",
        iconColorArgb = Color.Red.toArgb(),
        contentDescription = R.string.content_description_search_setting,
        isEnabled = true,
    ),
)


@PreviewLightDark
@Composable
private fun PreviewCameraScreen() {
    TsPreview {
        CameraScreenContent(
            searchChips = mockChips,
            textMatches = listOf(
                TextMatchUiModel("First", 0f, MatchOutline.of(Rect(80f, 300f, 420f, 400f)), Color.Yellow.toArgb()),
                TextMatchUiModel("Third", 0f, MatchOutline.of(Rect(500f, 700f, 780f, 790f)), Color.Red.toArgb()),
            ),
            onSearchChipClicked = {},
            surfaceRequest = null,
            transformationInfo = null,
            zoomLevel = 0.3f,
            hasFlashUnit = true,
            torchEnabled = false,
            quickSearchTerm = null,
            dialog = TsDialog.None,
            onZoomChanged = {},
            onTorchToggled = {},
            onQuickSearchClicked = {},
            onQuickSearchStopClicked = {},
            onIntent = {},
        )
    }
}

@Preview(widthDp = 891, heightDp = 411, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCameraScreenLandscape() {
    TsPreview {
        CameraScreenContent(
            searchChips = mockChips,
            textMatches = listOf(
                TextMatchUiModel("First", 0f, MatchOutline.of(Rect(300f, 300f, 640f, 400f)), Color.Yellow.toArgb()),
            ),
            onSearchChipClicked = {},
            surfaceRequest = null,
            transformationInfo = null,
            zoomLevel = 0.3f,
            hasFlashUnit = true,
            torchEnabled = false,
            quickSearchTerm = null,
            dialog = TsDialog.None,
            onZoomChanged = {},
            onTorchToggled = {},
            onQuickSearchClicked = {},
            onQuickSearchStopClicked = {},
            onIntent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewSearchChipRow() {
    TsPreview {
        SearchSettingsChipRow(
            searchChips = mockChips,
            quickSearchTerm = null,
            onSearchChipClicked = {},
            onQuickSearchStopClicked = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewSearchChipRowQuickSearchActive() {
    TsPreview {
        SearchSettingsChipRow(
            searchChips = mockChips,
            quickSearchTerm = "A quick search term long enough to reach the stop button",
            onSearchChipClicked = {},
            onQuickSearchStopClicked = {},
        )
    }
}
