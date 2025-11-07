package com.dailyflow.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dailyflow.app.data.model.Category
import com.dailyflow.app.ui.viewmodel.CategoryManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(navController: NavController, viewModel: CategoryManagementViewModel = hiltViewModel()) {
    val categories by viewModel.categories.collectAsState()
    val showArchived by viewModel.showArchived.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Управление категориями") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                categoryToEdit = null
                showDialog = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить категорию")
            }
        }
    ) {
        Column(modifier = Modifier.padding(it).padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = showArchived, onCheckedChange = { viewModel.toggleShowArchived(it) })
                Spacer(modifier = Modifier.width(8.dp))
                Text("Показывать архивные")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filterType == CategoryFilterType.ALL, onClick = { viewModel.setFilterType(CategoryFilterType.ALL) }, label = { Text("Все") })
                FilterChip(selected = filterType == CategoryFilterType.FOR_TASKS, onClick = { viewModel.setFilterType(CategoryFilterType.FOR_TASKS) }, label = { Text("Для задач") })
                FilterChip(selected = filterType == CategoryFilterType.FOR_NOTES, onClick = { viewModel.setFilterType(CategoryFilterType.FOR_NOTES) }, label = { Text("Для заметок") })
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    CategoryItem(category = category, onClick = {
                        categoryToEdit = category
                        showDialog = true
                    })
                }
            }
        }
    }

    if (showDialog) {
        CategoryEditDialog(
            category = categoryToEdit,
            onDismiss = { showDialog = false },
            onSave = { id, name, color, icon, forTasks, forNotes ->
                viewModel.saveCategory(id, name, color, icon, forTasks, forNotes)
                showDialog = false
            },
            onArchive = {
                viewModel.archiveCategory(it)
                showDialog = false
            },
            onUnarchive = {
                viewModel.unarchiveCategory(it)
                showDialog = false
            }
        )
    }
}

@Composable
fun CategoryItem(category: Category, onClick: () -> Unit) {
    val alpha = if (category.isArchived) 0.5f else 1f
    Card(modifier = Modifier
        .fillMaxWidth()
        .alpha(alpha)
        .clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(category.color)))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = category.name, modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryEditDialog(
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, String, Boolean, Boolean) -> Unit,
    onArchive: (String) -> Unit,
    onUnarchive: (String) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var selectedColor by remember { mutableStateOf(category?.color ?: "#F44336") }
    var selectedIcon by remember { mutableStateOf(category?.icon ?: "work") }
    var forTasks by remember { mutableStateOf(category?.forTasks ?: true) }
    var forNotes by remember { mutableStateOf(category?.forNotes ?: true) }

    val colors = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
        "#795548", "#9E9E9E", "#607D8B", "#FFFFFF", "#000000", "#D32F2F", "#C2185B", "#7B1FA2",
        "#512DA8", "#303F9F", "#1976D2", "#0288D1", "#0097A7", "#00796B", "#388E3C", "#689F38",
        "#AFB42B", "#FBC02D", "#FFA000", "#F57C00", "#E64A19", "#5D4037", "#616161", "#455A64",
        "#FBE9E7", "#F3E5F5", "#E8EAF6", "#E3F2FD", "#E0F7FA", "#E0F2F1", "#F1F8E9", "#FFFDE7",
        "#FFF8E1", "#FFF3E0"
    )
    val icons = mapOf(
        "work" to Icons.Default.Work, "home" to Icons.Default.Home, "health" to Icons.Default.LocalHospital,
        "education" to Icons.Default.School, "finance" to Icons.Default.AttachMoney, "shopping" to Icons.Default.ShoppingCart,
        "sport" to Icons.Default.Sports, "star" to Icons.Default.Star, "favorite" to Icons.Default.Favorite,
        "build" to Icons.Default.Build, "pets" to Icons.Default.Pets, "restaurant" to Icons.Default.Restaurant,
        "movie" to Icons.Default.Movie, "flight" to Icons.Default.Flight, "music_note" to Icons.Default.MusicNote,
        "face" to Icons.Default.Face, "person" to Icons.Default.Person, "group" to Icons.Default.Group, 
        "public" to Icons.Default.Public, "deck" to Icons.Default.Deck, "park" to Icons.Default.Park,
        "local_florist" to Icons.Default.LocalFlorist, "cloud" to Icons.Default.Cloud, "wb_sunny" to Icons.Default.WbSunny,
        "bedtime" to Icons.Default.Bedtime, "notifications" to Icons.Default.Notifications, "warning" to Icons.Default.Warning,
        "error" to Icons.Default.Error, "info" to Icons.Default.Info, "help" to Icons.Default.Help,
        "flag" to Icons.Default.Flag, "campaign" to Icons.Default.Campaign, "lightbulb" to Icons.Default.Lightbulb,
        "memory" to Icons.Default.Memory, "headset" to Icons.Default.Headset, "computer" to Icons.Default.Computer,
        "smartphone" to Icons.Default.Smartphone, "watch" to Icons.Default.Watch, "camera_alt" to Icons.Default.CameraAlt,
        "photo_camera" to Icons.Default.PhotoCamera, "palette" to Icons.Default.Palette, "brush" to Icons.Default.Brush,
        "edit" to Icons.Default.Edit, "content_cut" to Icons.Default.ContentCut, "mic" to Icons.Default.Mic,
        "videocam" to Icons.Default.Videocam, "call" to Icons.Default.Call, "email" to Icons.Default.Email,
        "location_on" to Icons.Default.LocationOn, "place" to Icons.Default.Place, "map" to Icons.Default.Map,
        "train" to Icons.Default.Train, "directions_car" to Icons.Default.DirectionsCar, "local_shipping" to Icons.Default.LocalShipping,
        "local_taxi" to Icons.Default.LocalTaxi, "local_bar" to Icons.Default.LocalBar, "local_cafe" to Icons.Default.LocalCafe,
        "local_dining" to Icons.Default.LocalDining, "local_pizza" to Icons.Default.LocalPizza, "fastfood" to Icons.Default.Fastfood,
        "hotel" to Icons.Default.Hotel, "child_care" to Icons.Default.ChildCare, "fitness_center" to Icons.Default.FitnessCenter,
        "casino" to Icons.Default.Casino, "spa" to Icons.Default.Spa, "smoke_free" to Icons.Default.SmokeFree,
        "smoking_rooms" to Icons.Default.SmokingRooms, "cake" to Icons.Default.Cake, "sports_esports" to Icons.Default.SportsEsports,
        "videogame_asset" to Icons.Default.VideogameAsset, "vpn_key" to Icons.Default.VpnKey, "lock" to Icons.Default.Lock,
        "lock_open" to Icons.Default.LockOpen, "security" to Icons.Default.Security, "bug_report" to Icons.Default.BugReport,
        "report" to Icons.Default.Report, "shield" to Icons.Default.Shield, "verified" to Icons.Default.Verified,
        "task_alt" to Icons.Default.TaskAlt, "bookmark" to Icons.Default.Bookmark, "bookmarks" to Icons.Default.Bookmarks,
        "history" to Icons.Default.History, "schedule" to Icons.Default.Schedule, "today" to Icons.Default.Today,
        "event" to Icons.Default.Event, "alarm" to Icons.Default.Alarm, "update" to Icons.Default.Update,
        "rocket_launch" to Icons.Default.RocketLaunch, "science" to Icons.Default.Science, "anchor" to Icons.Default.Anchor, 
        "construction" to Icons.Default.Construction, "eco" to Icons.Default.Eco, "flutter_dash" to Icons.Default.FlutterDash,
        "savings" to Icons.Default.Savings, "paid" to Icons.Default.Paid, "account_balance" to Icons.Default.AccountBalance
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Новая категория" else "Редактировать категорию") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") })
                
                Text("Цвет:")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colors.forEach { colorString ->
                            ColorCircle(colorString, selectedColor) { selectedColor = it }
                        }
                    }
                }

                Text("Иконка:")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        icons.entries.forEach { (iconName, iconVector) ->
                            IconSelector(iconName, iconVector, selectedIcon) { selectedIcon = it }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = forTasks, onCheckedChange = { forTasks = it })
                    Text("Для задач")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = forNotes, onCheckedChange = { forNotes = it })
                    Text("Для заметок")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(category?.id, name, selectedColor, selectedIcon, forTasks, forNotes) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            Row {
                 if (category?.isArchived == true) {
                     TextButton(onClick = { onUnarchive(category.id) }) {
                        Text("Восстановить")
                    }
                 } else if (category != null) {
                    TextButton(onClick = { onArchive(category.id) }) {
                        Text("Архивировать")
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            }
        }
    )
}

@Composable
fun ColorCircle(colorString: String, selectedColor: String, onColorSelected: (String) -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(android.graphics.Color.parseColor(colorString)))
            .clickable { onColorSelected(colorString) }
            .border(
                width = 2.dp,
                color = if (selectedColor == colorString) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape
            )
    )
}

@Composable
fun IconSelector(iconName: String, iconVector: ImageVector, selectedIcon: String, onIconSelected: (String) -> Unit) {
    Icon(
        imageVector = iconVector,
        contentDescription = iconName,
        modifier = Modifier
            .size(40.dp)
            .clickable { onIconSelected(iconName) }
            .padding(4.dp)
            .border(
                width = 2.dp,
                color = if (selectedIcon == iconName) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape
            )
    )
}
