package com.dailyflow.app.ui.viewmodel

import com.dailyflow.app.data.model.Task

data class RecurringActionDialogState(
    val task: Task,
    val actionType: RecurringActionType
)

enum class RecurringActionType {
    DELETE,
    CANCEL
}

internal data class PendingRecurringAction(
    val task: Task,
    val actionType: RecurringActionType
)

