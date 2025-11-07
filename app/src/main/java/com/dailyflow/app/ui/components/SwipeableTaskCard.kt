package com.dailyflow.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dailyflow.app.data.model.Category
import com.dailyflow.app.data.model.Task

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeableTaskCard(
    task: Task,
    category: Category?,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    val dismissState = rememberDismissState(
        confirmStateChange = { dismissValue ->
            when (dismissValue) {
                DismissValue.DismissedToEnd -> onToggle(true)
                DismissValue.DismissedToStart -> onToggle(false)
                else -> {}
            }
            false
        }
    )

    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
        background = {
            val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    DismissValue.DismissedToEnd -> Color.Green.copy(alpha = 0.5f)
                    DismissValue.DismissedToStart -> Color.Red.copy(alpha = 0.5f)
                    else -> Color.Transparent
                }
            )
            val alignment = when (direction) {
                DismissDirection.StartToEnd -> Alignment.CenterStart
                DismissDirection.EndToStart -> Alignment.CenterEnd
            }
            val icon = when (direction) {
                DismissDirection.StartToEnd -> Icons.Default.Check
                DismissDirection.EndToStart -> Icons.Default.Close
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(icon, contentDescription = "Localized description")
            }
        },
        dismissContent = {
            TaskCard(
                task = task,
                category = category,
                onClick = onClick,
                onToggle = onToggle,
                onDelete = onDelete,
                onCancel = onCancel
            )
        }
    )
}
