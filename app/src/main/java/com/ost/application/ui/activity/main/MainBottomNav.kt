package com.ost.application.ui.activity.main
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ost.application.R
import com.ost.application.util.CardPosition
import com.ost.application.util.CustomCardItem
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppBottomNavigation(
    directItems: List<MenuItemData>,
    selectedItemId: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalFloatingToolbar(
        modifier = modifier.shadow(5.dp, shape = MaterialTheme.shapes.extraLarge),
        expanded = true,
    ) {
        directItems.forEach { item ->
            val isSelected = item.id == selectedItemId
            Surface(
                onClick = { onItemClick(item.id) },
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    MenuItemIcon(
                        icon = item.icon,
                        contentDescription = stringResource(item.titleResId),
                        modifier = Modifier.size(24.dp)
                    )
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = expandHorizontally() + fadeIn(),
                        exit = shrinkHorizontally() + fadeOut()
                    ) {
                        Row {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(item.titleResId),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        val isMoreSelected = selectedItemId == MORE_ITEM_ID
        IconButton(
            onClick = { onItemClick(MORE_ITEM_ID) },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (isMoreSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                contentColor = if (isMoreSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                Icons.Rounded.MoreHoriz,
                contentDescription = stringResource(R.string.more),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
@Composable
fun MoreBottomSheetContent(
    menuItems: List<MenuItemData?>,
    currentSelectedScreenId: String?,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val nonNullItems = remember(menuItems) { menuItems.filterNotNull() }
    LazyColumn(
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        itemsIndexed(nonNullItems) { index, item ->
            val position = when {
                nonNullItems.size == 1 -> CardPosition.SINGLE
                index == 0 -> CardPosition.TOP
                index == nonNullItems.lastIndex -> CardPosition.BOTTOM
                else -> CardPosition.MIDDLE
            }
            val selectedCardCorners = CardPosition.SINGLE
            val isSelected = item.id == currentSelectedScreenId
            CustomCardItem(
                position = if (isSelected) selectedCardCorners else position,
                title = stringResource(item.titleResId),
                icon = if (item.icon is MenuIcon.Res) item.icon.resId else null,
                colors = if (isSelected) {
                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                } else {
                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                },
                onClick = { onItemClick(item.id) }
            )
        }
    }
}
