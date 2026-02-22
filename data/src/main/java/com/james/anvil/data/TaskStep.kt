package com.james.anvil.data

import java.util.UUID

data class TaskStep(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false
)
