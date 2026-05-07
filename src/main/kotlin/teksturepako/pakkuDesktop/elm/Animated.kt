/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.elm

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.styling.DividerStyle
import org.jetbrains.jewel.ui.theme.dividerStyle

// ---------------------------------------------------------------------------
// ELM-safe animation helpers
// ---------------------------------------------------------------------------

/**
 * Generic animated value. Animates toward [target] whenever it changes.
 * Reading the returned value inside a composable triggers recomposition on each frame.
 */
@Composable
fun <T, V : AnimationVector> animated(
    target: T,
    typeConverter: TwoWayConverter<T, V>,
    animationSpec: AnimationSpec<T> = spring(),
): T {
    val animatable = remember { Animatable(target, typeConverter) }
    LaunchedEffect(target) {
        animatable.animateTo(target, animationSpec)
    }
    return animatable.value
}

/**
 * Animates a [Color] toward [target] whenever it changes.
 */
@Composable
fun animatedColor(
    target: Color,
    animationSpec: AnimationSpec<Color> = tween(durationMillis = 300),
): Color = animateColorAsState(target, animationSpec).value

/**
 * Animates a [Float] toward [target] whenever it changes.
 */
@Composable
fun animatedFloat(
    target: Float,
    animationSpec: AnimationSpec<Float> = spring(),
): Float = animated(target, Float.VectorConverter, animationSpec)

/**
 * Animates a [Dp] toward [target] whenever it changes.
 */
@Composable
fun animatedDp(
    target: Dp,
    animationSpec: AnimationSpec<Dp> = spring(),
): Dp = animated(target, Dp.VectorConverter, animationSpec)

/**
 * Returns a [DividerStyle] whose color animates whenever the theme changes.
 * Defaults to [JewelTheme.globalColors].borders.normal so custom GlobalColors
 * (e.g. PakkuDarkGlobalColors) are correctly applied. Metrics come from
 * the theme's default divider style.
 */
@Composable
fun animatedDividerStyle(
    style: DividerStyle = JewelTheme.dividerStyle,
    color: Color = JewelTheme.globalColors.borders.normal,
    animationSpec: AnimationSpec<Color> = tween(durationMillis = 300),
): DividerStyle {
    val animatedColor = animateColorAsState(color, animationSpec).value
    return DividerStyle(color = animatedColor, metrics = style.metrics)
}

