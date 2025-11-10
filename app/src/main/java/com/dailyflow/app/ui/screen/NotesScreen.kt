package com.dailyflow.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dailyflow.app.data.model.Note
import com.dailyflow.app.data.model.Category
import com.dailyflow.app.ui.navigation.Screen
import com.dailyflow.app.ui.viewmodel.NotesViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(navController: NavController) {
    val viewModel: NotesViewModel = hiltViewModel()
    val filteredNotes by viewModel.filteredNotes.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val showCompleted by viewModel.showCompleted.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.NoteDetail.createRoute(null)) }) {
                Icon(Icons.Default.Add, contentDescription = "Создать заметку")
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(it)) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        modifier = Modifier.height(32.dp),
                        selected = selectedCategoryId == null,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text("Все", maxLines = 1) }
                    )
                }
                items(categories) { category ->
                     FilterChip(
                        modifier = Modifier.height(32.dp),
                        selected = selectedCategoryId == category.id,
                        onClick = { viewModel.selectCategory(category.id) },
                        label = { 
                            Text(
                                text = category.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        }
                    )
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = showCompleted, onCheckedChange = { viewModel.toggleShowCompleted(it) })
                Spacer(modifier = Modifier.width(8.dp))
                Text("Показывать выполненные")
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredNotes) { note ->
                    NoteListItem(note = note, category = categories.find { it.id == note.categoryId }, navController = navController)
                }
            }
        }
    }
}

@Composable
fun NoteListItem(note: Note, category: Category?, navController: NavController) {
    val alpha = if (note.isCompleted) 0.6f else 1f
    val textDecoration = if (note.isCompleted) TextDecoration.LineThrough else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clickable { navController.navigate(Screen.NoteDetail.createRoute(note.id)) },
        colors = CardDefaults.cardColors(
            containerColor = category?.color?.let { Color(android.graphics.Color.parseColor(it)).copy(alpha = 0.1f) } ?: MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textDecoration = textDecoration)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (note.isChecklist) {
                val items = note.checklistItems
                if (items.isNullOrEmpty()) {
                    Text(text = note.content, style = MaterialTheme.typography.bodyMedium, maxLines = 5, textDecoration = textDecoration)
                } else {
                    items.take(5).forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (item.isChecked) Icons.Default.Done else Icons.Default.Circle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (item.isChecked) TextDecoration.LineThrough else textDecoration
                            )
                        }
                    }
                    if (items.size > 5) {
                        Text("…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                Text(text = note.content, style = MaterialTheme.typography.bodyMedium, maxLines = 5, textDecoration = textDecoration)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                category?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Circle, contentDescription = null, tint = Color(android.graphics.Color.parseColor(it.color)), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = it.name, style = MaterialTheme.typography.labelSmall)
                    }
                }
                note.dateTime?.let {
                    Text(text = it.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
