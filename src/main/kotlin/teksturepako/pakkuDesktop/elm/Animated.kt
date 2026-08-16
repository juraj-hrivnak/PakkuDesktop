/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.elm

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.styling.DividerStyle
import org.jetbrains.jewel.ui.theme.dividerStyle

// -- Animation helpers --

/**
 * Generic animated value. Animates toward [target] whenever it changes.
 * Reading the returned value inside a composable triggers recomposition on each frame.
 *
 * Pass [initial] when the value should start elsewhere than [target] (e.g. fade-in from 0).
 */
@Composable
fun <T, V : AnimationVector> animated(
    target: T,
    typeConverter: TwoWayConverter<T, V>,
    animationSpec: AnimationSpec<T> = spring(),
    initial: T = target,
): T {
    val animatable = remember { Animatable(initial, typeConverter) }
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
    initial: Color = target,
): Color = animated<Color, AnimationVector4D>(
    target,
    Color.VectorConverter(initial.colorSpace),
    animationSpec,
    initial,
)

/**
 * Animates a [Float] toward [target] whenever it changes.
 */
@Composable
fun animatedFloat(
    target: Float,
    animationSpec: AnimationSpec<Float> = spring(),
    initial: Float = target,
): Float = animated<Float, AnimationVector1D>(target, Float.VectorConverter, animationSpec, initial)

/**
 * Animates a [Dp] toward [target] whenever it changes.
 */
@Composable
fun animatedDp(
    target: Dp,
    animationSpec: AnimationSpec<Dp> = spring(),
    initial: Dp = target,
): Dp = animated<Dp, AnimationVector1D>(target, Dp.VectorConverter, animationSpec, initial)

/**
 * Remounts [content] whenever [routeKey] changes and fades it in.
 *
 * Prefer this over [androidx.compose.animation.AnimatedContent] when content reads
 * live model state: exit frames would otherwise show the *new* model under the old key.
 * Purely local visual animation — no [publish], no driver.
 */
@Composable
fun animatedRoute(
    routeKey: Any?,
    animationSpec: AnimationSpec<Float> = tween(durationMillis = 280),
    content: @Composable () -> Unit,
) {
    key(routeKey) {
        val alpha = animatedFloat(target = 1f, animationSpec = animationSpec, initial = 0f)
        Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }) {
            content()
        }
    }
}

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
