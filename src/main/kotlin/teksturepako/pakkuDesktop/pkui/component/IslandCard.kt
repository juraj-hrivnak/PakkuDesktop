/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.elm.RecomposeProbe

/** Outlined card, same resting chrome as a project card: border only, no fill. */
@Composable
fun IslandCard(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    RecomposeProbe("IslandCard")
    val shape = RoundedCornerShape(PakkuDesktopConstants.cardCornerRadius)
    Column(
        modifier = modifier
            .clip(shape)
            .border(PakkuDesktopConstants.cardBorderWidth, PakkuDesktopConstants.cardBorderColor, shape)
            .padding(PakkuDesktopConstants.cardPadding),
        verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.cardContentSpacing),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = PakkuDesktopConstants.cardTitleSize)
                if (description != null) {
                    Text(
                        description,
                        color = JewelTheme.contentColor.copy(alpha = PakkuDesktopConstants.secondaryContentAlpha),
                        fontSize = PakkuDesktopConstants.cardCaptionSize,
                    )
                }
            }
            trailing()
        }
        content()
    }
}
