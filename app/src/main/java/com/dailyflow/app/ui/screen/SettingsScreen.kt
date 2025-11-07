package com.dailyflow.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dailyflow.app.ui.navigation.Screen
import com.dailyflow.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val privacyAccepted by viewModel.privacyAccepted.collectAsState()
    val analyticsEnabled by viewModel.analyticsEnabled.collectAsState()
    val crashReportingEnabled by viewModel.crashReportingEnabled.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Privacy section
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Конфиденциальность",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (!privacyAccepted) {
                        PrivacyConsentCard(
                            onAccept = { viewModel.acceptPrivacyPolicy() }
                        )
                    } else {
                        PrivacySettingsCard(
                            analyticsEnabled = analyticsEnabled,
                            crashReportingEnabled = crashReportingEnabled,
                            onAnalyticsToggle = { viewModel.setAnalyticsEnabled(it) },
                            onCrashReportingToggle = { viewModel.setCrashReportingEnabled(it) },
                            onResetData = { viewModel.resetAllData() }
                        )
                    }
                }
            }
        }
        
        // App settings section
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Настройки приложения",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    SettingItem(
                        icon = Icons.Default.Category,
                        title = "Категории",
                        subtitle = "Управление категориями",
                        onClick = { navController.navigate(Screen.CategoryManagement.route) }
                    )

                    SettingItem(
                        icon = Icons.Default.Notifications,
                        title = "Уведомления",
                        subtitle = "Управление напоминаниями",
                        onClick = { /* TODO: Navigate to notification settings */ }
                    )
                    
                    SettingItem(
                        icon = Icons.Default.Backup,
                        title = "Резервное копирование",
                        subtitle = "Экспорт и импорт данных",
                        onClick = { /* TODO: Navigate to backup settings */ }
                    )
                    
                    SettingItem(
                        icon = Icons.Default.Language,
                        title = "Язык",
                        subtitle = "Русский",
                        onClick = { /* TODO: Navigate to language settings */ }
                    )
                }
            }
        }
        
        // About section
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "О приложении",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    SettingItem(
                        icon = Icons.Default.Info,
                        title = "Версия",
                        subtitle = "1.0.0",
                        onClick = { /* TODO: Show version info */ }
                    )
                    
                    SettingItem(
                        icon = Icons.Default.Help,
                        title = "Помощь",
                        subtitle = "FAQ и поддержка",
                        onClick = { /* TODO: Navigate to help */ }
                    )
                    
                    SettingItem(
                        icon = Icons.Default.PrivacyTip,
                        title = "Политика конфиденциальности",
                        subtitle = "Как мы защищаем ваши данные",
                        onClick = { /* TODO: Open privacy policy */ }
                    )
                }
            }
        }
    }
}

@Composable
fun PrivacyConsentCard(
    onAccept: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Согласие на обработку данных",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Для работы приложения необходимо ваше согласие на обработку персональных данных. Все данные хранятся локально на вашем устройстве и не передаются третьим лицам.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Принять и продолжить")
            }
        }
    }
}

@Composable
fun PrivacySettingsCard(
    analyticsEnabled: Boolean,
    crashReportingEnabled: Boolean,
    onAnalyticsToggle: (Boolean) -> Unit,
    onCrashReportingToggle: (Boolean) -> Unit,
    onResetData: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SwitchItem(
            title = "Аналитика",
            subtitle = "Помочь улучшить приложение",
            checked = analyticsEnabled,
            onCheckedChange = onAnalyticsToggle
        )
        
        SwitchItem(
            title = "Отчеты об ошибках",
            subtitle = "Автоматически отправлять отчеты",
            checked = crashReportingEnabled,
            onCheckedChange = onCrashReportingToggle
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = onResetData,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Удалить все данные")
        }
    }
}

@Composable
fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun SwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
