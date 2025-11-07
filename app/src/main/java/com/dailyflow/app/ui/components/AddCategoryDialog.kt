package com.dailyflow.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onAddCategory: (name: String, color: String, icon: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#FFC107") } // Default to yellow
    var icon by remember { mutableStateOf("work") } // Default icon

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Добавить категорию")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название категории") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Выберите цвет:")
                ColorPicker(selectedColor = color, onColorSelected = { color = it })
                Spacer(modifier = Modifier.height(16.dp))
                Text("Выберите иконку:")
                IconPicker(selectedIcon = icon, onIconSelected = { icon = it })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAddCategory(name, color, icon)
                        onDismiss()
                    }
                }
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
