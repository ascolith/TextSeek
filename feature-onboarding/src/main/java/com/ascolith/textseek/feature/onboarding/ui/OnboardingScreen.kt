package com.ascolith.textseek.feature.onboarding.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.ascolith.textseek.core.composable.TsCameraPermissionAnimation
import com.ascolith.textseek.core.composable.TsCameraPermissionContent
import com.ascolith.textseek.core.composable.TsPreview
import com.ascolith.textseek.core.composable.tsLinkStyles
import com.ascolith.textseek.core.ui.isHeightCompact
import com.ascolith.textseek.core.ui.labelResId
import com.ascolith.textseek.core.ui.sampleResId
import com.ascolith.textseek.domain.model.TextRecognitionScript
import com.ascolith.textseek.feature.onboarding.ui.model.OnboardingPage
import com.ascolith.textseek.feature.onboarding.ui.model.ScreenIntent
import com.ascolith.textseek.feature.onboarding.ui.model.SideEffect
import com.ascolith.textseek.feature.onboarding.ui.model.UiState
import com.ascolith.textseek.resources.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val messageScope = rememberCoroutineScope()
    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            // Launched so a showing snackbar doesn't hold back the next side effect
            is SideEffect.ShowMessage -> messageScope.launch {
                snackbarHostState.showSnackbar(context.getString(sideEffect.messageResId))
            }
        }
    }

    OnboardingContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::handleIntent,
        modifier = modifier,
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun OnboardingContent(
    state: UiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (ScreenIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = state.currentPage > 0) { onIntent(ScreenIntent.Back) }

    // The view model owns the page, so the pager only follows it
    val pagerState = rememberPagerState(initialPage = state.currentPage) { state.pages.size }
    LaunchedEffect(state.currentPage) { pagerState.animateScrollToPage(state.currentPage) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            OnboardingBottomBar(
                state = state,
                onBack = { onIntent(ScreenIntent.Back) },
                onForward = { onIntent(ScreenIntent.Forward) },
            )
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            userScrollEnabled = false,
            key = { state.pages[it] },
        ) { index ->
            when (state.pages[index]) {
                OnboardingPage.Welcome -> WelcomePage()
                OnboardingPage.PrivacyPolicy -> PrivacyPolicyPage(
                    agreed = state.privacyAgreed,
                    onAgreedChange = { onIntent(ScreenIntent.SetPrivacyAgreed(it)) },
                )
                OnboardingPage.CameraPermission -> CameraPermissionPage()
                OnboardingPage.TextRecognitionScript -> TextRecognitionScriptPage(
                    selected = state.script,
                    onSelect = { onIntent(ScreenIntent.SelectScript(it)) },
                )
            }
        }
    }
}

@Composable
private fun OnboardingBottomBar(
    state: UiState,
    onBack: () -> Unit,
    onForward: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (state.currentPage > 0) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.back))
                }
            }
        }
        PagerIndicator(pageCount = state.pages.size, currentPage = state.currentPage)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            Button(onClick = onForward, enabled = state.canGoForward) {
                Text(stringResource(if (state.isLastPage) R.string.finish else R.string.next))
            }
        }
    }
}

@Composable
private fun PagerIndicator(pageCount: Int, currentPage: Int) {
    val description = stringResource(R.string.onboarding_page_indicator, currentPage + 1, pageCount)
    Row(
        // Announces the move to a new page, since focus stays on the Next button
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = description
            liveRegion = LiveRegionMode.Polite
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(pageCount) { page ->
            val selected = page == currentPage
            val width by animateDpAsState(if (selected) 24.dp else 8.dp)
            val color by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            )
            Box(
                Modifier
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/** In short windows the [animation] moves beside the content so both fit. */
@Composable
private fun OnboardingPageLayout(
    @StringRes titleResId: Int,
    animation: (@Composable (Modifier) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val compactHeight = isHeightCompact()
    when {
        compactHeight && animation != null -> Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 24.dp, top = 24.dp, bottom = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                animation(
                    Modifier
                        .heightIn(max = 300.dp)
                        .fillMaxHeight()
                        .aspectRatio(1f, matchHeightConstraintsFirst = true),
                )
            }
            OnboardingPageColumn(titleResId = titleResId, modifier = Modifier.weight(1f), content = content)
        }
        // Keeps wide content like the script cards from stretching across the screen
        compactHeight -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            OnboardingPageColumn(titleResId = titleResId, modifier = Modifier.widthIn(max = 600.dp), content = content)
        }
        else -> OnboardingPageColumn(titleResId = titleResId) {
            animation?.invoke(
                Modifier
                    .size(300.dp)
                    .align(Alignment.CenterHorizontally),
            )
            content()
        }
    }
}

@Composable
private fun OnboardingPageColumn(
    @StringRes titleResId: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(titleResId),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
        content()
    }
}

@Composable
private fun WelcomePage() {
    OnboardingPageLayout(
        titleResId = R.string.onboarding_welcome_title,
        animation = { OnboardingAnimation(resId = R.raw.lottie_text_search_scan_single, modifier = it) },
    ) {
        Text(
            text = stringResource(R.string.onboarding_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun PrivacyPolicyPage(
    agreed: Boolean,
    onAgreedChange: (Boolean) -> Unit,
) {
    OnboardingPageLayout(titleResId = R.string.onboarding_privacy_title) {
        Text(
            text = stringResource(R.string.onboarding_privacy_intro),
            style = MaterialTheme.typography.bodyLarge,
        )
        PrivacySectionHeading(R.string.onboarding_terms_heading)
        Text(
            text = AnnotatedString.fromHtml(
                htmlString = stringResource(
                    R.string.onboarding_terms_body,
                    stringResource(R.string.gpl_v3_url),
                ),
                linkStyles = tsLinkStyles(),
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        PrivacySectionHeading(R.string.onboarding_data_privacy_heading)
        Text(
            text = AnnotatedString.fromHtml(
                htmlString = stringResource(
                    R.string.onboarding_data_privacy_body,
                    stringResource(R.string.privacy_policy_url),
                ),
                linkStyles = tsLinkStyles(),
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .toggleable(value = agreed, role = Role.Checkbox, onValueChange = onAgreedChange)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = agreed, onCheckedChange = null)
            Text(
                text = stringResource(R.string.onboarding_privacy_agree),
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun PrivacySectionHeading(@StringRes textResId: Int) {
    Text(
        text = stringResource(textResId),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun OnboardingAnimation(
    @RawRes resId: Int,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
    LottieAnimation(
        composition = composition,
        modifier = modifier,
        iterations = LottieConstants.IterateForever,
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun CameraPermissionPage(previewPermissionStatus: PermissionStatus = PermissionStatus.Granted) {
    val permissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA,
        previewPermissionStatus = previewPermissionStatus,
    )
    OnboardingPageLayout(
        titleResId = R.string.onboarding_camera_title,
        animation = { TsCameraPermissionAnimation(it) },
    ) {
        if (permissionState.status.isGranted) {
            Text(
                text = stringResource(R.string.camera_permission_explanation),
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_check_circle),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.camera_access_granted),
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        } else {
            TsCameraPermissionContent(
                showRationale = permissionState.status.shouldShowRationale,
                onGrantClick = { permissionState.launchPermissionRequest() },
                modifier = Modifier.fillMaxWidth(),
                showAnimation = false,
            )
        }
    }
}

@Composable
private fun TextRecognitionScriptPage(
    selected: TextRecognitionScript,
    onSelect: (TextRecognitionScript) -> Unit,
) {
    OnboardingPageLayout(titleResId = R.string.onboarding_script_title) {
        Text(
            text = stringResource(R.string.onboarding_script_body),
            style = MaterialTheme.typography.bodyLarge,
        )
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextRecognitionScript.entries.forEach { script ->
                ScriptCard(script = script, selected = script == selected, onClick = { onSelect(script) })
            }
        }
    }
}

@Composable
private fun ScriptCard(
    script: TextRecognitionScript,
    selected: Boolean,
    onClick: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = if (selected) {
            CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        } else {
            CardDefaults.outlinedCardColors()
        },
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            CardDefaults.outlinedCardBorder()
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(script.sampleResId),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(script.labelResId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.onSecondaryContainer),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewOnboardingWelcome() {
    TsPreview {
        OnboardingContent(state = UiState(), snackbarHostState = SnackbarHostState(), onIntent = {})
    }
}

@PreviewLightDark
@Composable
private fun PreviewOnboardingPrivacyPolicy() {
    TsPreview {
        PrivacyPolicyPage(agreed = false, onAgreedChange = {})
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@PreviewLightDark
@Composable
private fun PreviewOnboardingCameraPermission() {
    TsPreview {
        CameraPermissionPage(previewPermissionStatus = PermissionStatus.Denied(shouldShowRationale = false))
    }
}

@PreviewLightDark
@Composable
private fun PreviewOnboardingTextRecognitionScript() {
    TsPreview {
        TextRecognitionScriptPage(selected = TextRecognitionScript.JAPANESE, onSelect = {})
    }
}

@Preview(widthDp = 891, heightDp = 411)
@Composable
private fun PreviewOnboardingWelcomeLandscape() {
    TsPreview {
        OnboardingContent(state = UiState(), snackbarHostState = SnackbarHostState(), onIntent = {})
    }
}

@Preview(widthDp = 891, heightDp = 411)
@Composable
private fun PreviewOnboardingPrivacyPolicyLandscape() {
    TsPreview {
        PrivacyPolicyPage(agreed = false, onAgreedChange = {})
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Preview(widthDp = 891, heightDp = 411)
@Composable
private fun PreviewOnboardingCameraPermissionLandscape() {
    TsPreview {
        CameraPermissionPage(previewPermissionStatus = PermissionStatus.Denied(shouldShowRationale = false))
    }
}

@Preview(widthDp = 891, heightDp = 411)
@Composable
private fun PreviewOnboardingTextRecognitionScriptLandscape() {
    TsPreview {
        TextRecognitionScriptPage(selected = TextRecognitionScript.JAPANESE, onSelect = {})
    }
}
