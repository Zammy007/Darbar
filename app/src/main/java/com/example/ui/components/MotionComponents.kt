package com.example.ui.components

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.SubjectTab
import com.example.ui.theme.IconActionBlue
import com.example.ui.theme.IconSuccessEmerald
import com.example.ui.theme.IconUploadOrange
import com.example.ui.theme.IconWarningAmber
import kotlinx.coroutines.delay

/**
 * Centralized motion tokens for Darbar.
 * Every animation uses consistent durations, ease-out curves, and GPU-friendly transform/opacity properties.
 */
object DarbarMotion {
    const val FAST_MS: Int = 160
    const val MEDIUM_MS: Int = 260
    const val PAGE_MS: Int = 300
    const val MODAL_EXIT_MS: Int = 200
    const val STAGGER_STEP_MS: Long = 60L
    const val MAX_STAGGER_ITEMS: Int = 8

    val SLIDE_DISTANCE_DP: Dp = 14.dp
    val HOVER_LIFT_DP: Dp = 4.dp

    // Crisp ease-out curve for natural deceleration
    val EaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val EaseInOut = FastOutSlowInEasing

    fun duration(standardMs: Int, reducedMotion: Boolean): Int =
        if (reducedMotion) 0 else standardMs
}

/**
 * CompositionLocal reflecting the OS reduced-motion preference (`ANIMATOR_DURATION_SCALE == 0f`).
 */
val LocalReducedMotion = compositionLocalOf { false }

@Composable
fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Button micro-interaction modifier:
 * - Subtle 1.03x scale on hover, 0.97x scale on press (GPU transform only).
 * - Automatically minimized when reduced motion is active.
 */
fun Modifier.interactiveButtonMotion(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true
): Modifier = composed {
    val reducedMotion = LocalReducedMotion.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val targetScale = when {
        !enabled || reducedMotion -> 1f
        isPressed -> 0.97f
        isHovered -> 1.03f
        else -> 1f
    }

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion),
            easing = DarbarMotion.EaseOut
        ),
        label = "btn_scale"
    )

    this
        .hoverable(interactionSource = interactionSource, enabled = enabled)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}

/**
 * Card micro-interaction modifier:
 * - Lifts gently (-4dp) with a soft GPU shadow on hover, subtle 0.975x scale on press.
 * - Uses only GPU-accelerated graphicsLayer properties (scale, translationY, shadowElevation).
 */
fun Modifier.interactiveCardMotion(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(20.dp),
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val reducedMotion = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val density = LocalDensity.current

    val targetScale = when {
        !enabled || reducedMotion -> 1f
        isPressed -> 0.975f
        isHovered -> 1.015f
        else -> 1f
    }
    val targetLiftDp = when {
        !enabled || reducedMotion -> 0.dp
        isHovered && !isPressed -> -DarbarMotion.HOVER_LIFT_DP
        else -> 0.dp
    }
    val targetShadowDp = when {
        !enabled -> 0.dp
        isHovered && !isPressed -> 10.dp
        isPressed -> 2.dp
        else -> 1.dp
    }

    val durationMs = DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion)
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMs, easing = DarbarMotion.EaseOut),
        label = "card_scale"
    )
    val liftDp by animateDpAsState(
        targetValue = targetLiftDp,
        animationSpec = tween(durationMs, easing = DarbarMotion.EaseOut),
        label = "card_lift"
    )
    val shadowDp by animateDpAsState(
        targetValue = targetShadowDp,
        animationSpec = tween(durationMs, easing = DarbarMotion.EaseOut),
        label = "card_shadow"
    )

    val base = this
        .hoverable(interactionSource = interactionSource, enabled = enabled)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationY = with(density) { liftDp.toPx() }
            shadowElevation = with(density) { shadowDp.toPx() }
            this.shape = shape
            clip = false
        }

    if (onClick != null) {
        base
            .clip(shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
    } else {
        base
    }
}

/**
 * Tactile click modifier supporting both hover lift and 0.97x press scale.
 */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    hoverScale: Float = 1.02f,
    onClick: () -> Unit
): Modifier = composed {
    val reducedMotion = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val targetScale = when {
        !enabled || reducedMotion -> 1f
        isPressed -> pressedScale
        isHovered -> hoverScale
        else -> 1f
    }

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion),
            easing = DarbarMotion.EaseOut
        ),
        label = "bounce_scale"
    )
    this
        .hoverable(interactionSource = interactionSource, enabled = enabled)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = androidx.compose.material3.ripple(),
            onClick = onClick
        )
}

/**
 * IconButton with smooth GPU opacity/scale and tint transition on hover & press.
 */
@Composable
fun MotionIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val durationMs = DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion)
    val scale by animateFloatAsState(
        targetValue = when {
            !enabled || reducedMotion -> 1f
            isPressed -> 0.92f
            isHovered -> 1.08f
            else -> 1f
        },
        animationSpec = tween(durationMs, easing = DarbarMotion.EaseOut),
        label = "icon_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = when {
            !enabled -> 0.38f
            isPressed -> 0.72f
            else -> 1f
        },
        animationSpec = tween(durationMs, easing = DarbarMotion.EaseOut),
        label = "icon_alpha"
    )

    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .hoverable(interactionSource = interactionSource, enabled = enabled)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        content = content
    )
}

/**
 * GPU-friendly staggered entrance reveal (fade + gentle 14dp slide-up) with 60ms stagger.
 * Triggers as items load or scroll into composition, and respects reduced motion.
 */
@Composable
fun StaggeredFadeIn(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val density = LocalDensity.current
    val slidePx = with(density) { DarbarMotion.SLIDE_DISTANCE_DP.toPx() }

    var visible by remember { mutableStateOf(reducedMotion) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            visible = true
        } else {
            delay(index.coerceAtMost(DarbarMotion.MAX_STAGGER_ITEMS) * DarbarMotion.STAGGER_STEP_MS)
            visible = true
        }
    }

    val durationMs = DarbarMotion.duration(DarbarMotion.MEDIUM_MS, reducedMotion)
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMs, easing = DarbarMotion.EaseOut),
        label = "stagger_alpha"
    )
    val translationY by animateFloatAsState(
        targetValue = if (visible || reducedMotion) 0f else slidePx,
        animationSpec = tween(durationMs, easing = DarbarMotion.EaseOut),
        label = "stagger_ty"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translationY
        }
    ) {
        content()
    }
}

/**
 * Animated two-button View / Upload tab switcher using GPU-only `graphicsLayer.translationX`
 * with Modern Black & White monochrome pill and vibrantly colored icons.
 */
@Composable
fun SubjectSlidingTabBar(
    activeTab: SubjectTab,
    isUploadUnlocked: Boolean,
    accentColor: Color,
    onViewClick: () -> Unit,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalReducedMotion.current
    val density = LocalDensity.current
    val pillColor = MaterialTheme.colorScheme.primary
    val activeLabelColor = MaterialTheme.colorScheme.onPrimary

    val durationMs = DarbarMotion.duration(DarbarMotion.MEDIUM_MS, reducedMotion)
    val indicatorFraction by animateFloatAsState(
        targetValue = if (activeTab == SubjectTab.VIEW) 0f else 1f,
        animationSpec = tween(durationMs, easing = DarbarMotion.EaseOut),
        label = "tab_indicator_fraction"
    )

    val viewColor by animateColorAsState(
        targetValue = if (activeTab == SubjectTab.VIEW) {
            activeLabelColor
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion)),
        label = "view_tab_color"
    )
    val uploadColor by animateColorAsState(
        targetValue = if (activeTab == SubjectTab.UPLOAD) {
            activeLabelColor
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(DarbarMotion.duration(DarbarMotion.FAST_MS, reducedMotion)),
        label = "upload_tab_color"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            val tabWidth = maxWidth / 2
            val tabWidthPx = with(density) { tabWidth.toPx() }

            // GPU-accelerated monochrome sliding indicator pill
            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = tabWidthPx * indicatorFraction
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(pillColor)
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                // Separate VIEW button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("view_tab_button")
                        .clickable(onClick = onViewClick),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            tint = IconActionBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.tab_view),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = viewColor
                        )
                    }
                }

                // Separate UPLOAD button (password protected)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("upload_tab_button")
                        .clickable(onClick = onUploadClick),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.UploadFile,
                            contentDescription = null,
                            tint = IconUploadOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.tab_upload),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = uploadColor
                        )
                        Icon(
                            imageVector = if (isUploadUnlocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                            contentDescription = if (isUploadUnlocked) "Unlocked" else "Password required",
                            tint = if (isUploadUnlocked) IconSuccessEmerald else IconWarningAmber,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Smooth shimmer skeleton loader card while Firestore / Gemini data loads.
 * Falls back to a calm static placeholder when reduced motion is enabled.
 */
@Composable
fun ShimmerDocumentSkeleton(modifier: Modifier = Modifier) {
    val reducedMotion = LocalReducedMotion.current
    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    val highlight = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)

    val brush = if (reducedMotion) {
        Brush.linearGradient(colors = listOf(base, base))
    } else {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateX by transition.animateFloat(
            initialValue = -300f,
            targetValue = 900f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1150, easing = DarbarMotion.EaseInOut),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_x"
        )
        Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(translateX - 240f, 0f),
            end = Offset(translateX, 180f)
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .width(84.dp)
                            .height(28.dp)
                            .clip(CircleShape)
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .width(84.dp)
                            .height(28.dp)
                            .clip(CircleShape)
                            .background(brush)
                    )
                }
            }
        }
    }
}

@Composable
fun DarbarFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .padding(bottom = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text = stringResource(R.string.app_footer),
            style = MaterialTheme.typography.labelMedium,
            color = LocalContentColor.current.copy(alpha = 0.68f)
        )
    }
}
