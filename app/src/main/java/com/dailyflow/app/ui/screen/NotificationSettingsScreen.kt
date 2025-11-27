package com.dailyflow.app.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dailyflow.app.ui.viewmodel.NotificationSettingsViewModel
import androidx.compose.ui.res.stringResource
import com.dailyflow.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    navController: NavController,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val defaultReminderMinutes by viewModel.defaultReminderMinutes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_notifications)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.padding(it).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(stringResource(R.string.settings_general), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = defaultReminderMinutes.toString(),
                    onValueChange = { value ->
                        viewModel.setDefaultReminderMinutes(value.toIntOrNull() ?: 60)
                    },
                    label = { Text(stringResource(R.string.settings_default_reminder)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Divider()
            }
            item {
                Button(onClick = { 
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", context.packageName, null)
                    context.startActivity(intent)
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_open_system_notifications))
                }
            }
        }
    }
}
