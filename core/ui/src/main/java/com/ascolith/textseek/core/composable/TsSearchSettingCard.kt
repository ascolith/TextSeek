package com.ascolith.textseek.core.composable

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.resources.R

@Composable
fun TsSearchSettingCard(
    searchSettingId: SearchSettingId,
    text: String,
    enabled: Boolean,
    indicatorColor: Color,
    onClick: (SearchSettingId) -> Unit,
    modifier: Modifier = Modifier,
    topCornerRadius: Dp = 12.dp,
    bottomCornerRadius: Dp = 12.dp,
) {
    val disabledDescription = stringResource(R.string.disabled)
    Card(
        modifier = modifier.semantics { if (!enabled) stateDescription = disabledDescription },
        onClick = { onClick(searchSettingId) },
        shape = RoundedCornerShape(
            topStart = topCornerRadius,
            topEnd = topCornerRadius,
            bottomStart = bottomCornerRadius,
            bottomEnd = bottomCornerRadius
        ),
        colors = CardDefaults.cardColors().copy(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier
                    .semantics {
                        set(TsSemantics.borderColorKey, indicatorColor)
                    },
                imageVector = ImageVector.vectorResource(R.drawable.ic_text_match_outline),
                contentDescription = null,
                tint = indicatorColor
            )

            Spacer(modifier = Modifier.width(2.dp))

            Text(
                text = text,
                style = if (!enabled) {
                    LocalTextStyle.current.copy(textDecoration = TextDecoration.LineThrough)
                } else {
                    LocalTextStyle.current
                }
            )
        }
    }
}

@Preview(heightDp = 60, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(heightDp = 60, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun PreviewSearchSettingCardEnabled() {
    TsPreview {
        TsSearchSettingCard(
            searchSettingId = SearchSettingId(0),
            text = "Banana",
            enabled = true,
            indicatorColor = Color.Yellow,
            onClick = { },
        )
    }
}

@Preview(heightDp = 60, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(heightDp = 60, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun PreviewSearchSettingCardDisabled() {
    TsPreview {
        TsSearchSettingCard(
            searchSettingId = SearchSettingId(1),
            text = "Grape",
            enabled = false,
            indicatorColor = Color.Blue,
            onClick = { },
        )
    }
}
