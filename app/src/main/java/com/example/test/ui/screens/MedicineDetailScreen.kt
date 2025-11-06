package com.example.test.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import com.example.test.R
import com.example.test.alarm.AlarmScheduler
import com.example.test.data.model.MedicineSchedule
import com.example.test.ui.viewmodel.MedicineViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineDetailScreen(
    medicineId: Long,
    viewModel: MedicineViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val alarmScheduler = remember { AlarmScheduler(context) }

    LaunchedEffect(medicineId) {
        viewModel.loadMedicineWithSchedules(medicineId)
    }

    val medicineWithSchedules by viewModel.selectedMedicine.collectAsState()
    var showScheduleDialog by remember { mutableStateOf(false) }
    val calendar = Calendar.getInstance()
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = true
    )
    var intervalDays by remember { mutableStateOf(1) }

    if (showScheduleDialog) {
        ScheduleDialog(
            timePickerState = timePickerState,
            intervalDays = intervalDays,
            onIntervalChange = { intervalDays = it },
            onDismiss = {
                showScheduleDialog = false
                intervalDays = 1
            },
            onConfirm = {
                viewModel.addSchedule(medicineId, timePickerState.hour, timePickerState.minute, intervalDays)
                showScheduleDialog = false
                intervalDays = 1
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(medicineWithSchedules?.medicine?.name ?: stringResource(R.string.medicine_detail))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showScheduleDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_reminder))
            }
        }
    ) { paddingValues ->
        medicineWithSchedules?.let { data ->
            if (data.schedules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_reminders),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(data.schedules) { schedule ->
                        ScheduleItem(
                            schedule = schedule,
                            medicineId = data.medicine.id,
                            medicineName = data.medicine.name,
                            viewModel = viewModel,
                            onToggle = { updatedSchedule ->
                                viewModel.updateSchedule(updatedSchedule)
                                if (updatedSchedule.isEnabled) {
                                    alarmScheduler.scheduleAlarm(updatedSchedule, data.medicine.name)
                                } else {
                                    alarmScheduler.cancelAlarm(updatedSchedule.id)
                                }
                            },
                            onDelete = {
                                alarmScheduler.cancelAlarm(schedule.id)
                                viewModel.deleteSchedule(schedule)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleItem(
    schedule: MedicineSchedule,
    medicineId: Long,
    medicineName: String,
    viewModel: MedicineViewModel,
    onToggle: (MedicineSchedule) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val alarmScheduler = remember { AlarmScheduler(context) }

    var wasTakenToday by remember { mutableStateOf(false) }
    var showScheduleEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = schedule.hour,
        initialMinute = schedule.minute,
        is24Hour = true
    )
    var intervalDays by remember { mutableStateOf(schedule.intervalDays) }

    LaunchedEffect(schedule) {
        if (schedule.isEnabled) {
            alarmScheduler.scheduleAlarm(schedule, medicineName)
        }
        wasTakenToday = viewModel.wasScheduleTakenToday(schedule.id)
        intervalDays = schedule.intervalDays
    }

    if (showScheduleEditDialog) {
        ScheduleDialog(
            timePickerState = timePickerState,
            intervalDays = intervalDays,
            onIntervalChange = { intervalDays = it },
            onDismiss = {
                showScheduleEditDialog = false
                intervalDays = schedule.intervalDays
            },
            onConfirm = {
                val updatedSchedule = schedule.copy(
                    hour = timePickerState.hour,
                    minute = timePickerState.minute,
                    intervalDays = intervalDays
                )
                onToggle(updatedSchedule)
                showScheduleEditDialog = false
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_schedule)) },
            text = { Text(stringResource(R.string.delete_schedule_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = {
                viewModel.markScheduleAsTaken(
                    medicineId = medicineId,
                    scheduleId = schedule.id,
                    scheduledTime = System.currentTimeMillis()
                )
                wasTakenToday = true
            },
            enabled = !wasTakenToday,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(stringResource(R.string.taken))
        }

        Card(
            modifier = Modifier.weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (wasTakenToday)
                    MaterialTheme.colorScheme.tertiaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showScheduleEditDialog = true }
                ) {
                    Text(
                        text = String.format("%02d:%02d", schedule.hour, schedule.minute),
                        style = MaterialTheme.typography.displaySmall,
                        color = if (wasTakenToday)
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                    if (schedule.intervalDays > 1) {
                        Text(
                            text = "每 ${schedule.intervalDays} 天",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (wasTakenToday)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Switch(
                        checked = schedule.isEnabled,
                        onCheckedChange = { isEnabled ->
                            onToggle(schedule.copy(isEnabled = isEnabled))
                        }
                    )

                    FilledIconButton(
                        onClick = { showDeleteConfirmation = true },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.confirm))
            }
        },
        text = {
            content()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDialog(
    timePickerState: TimePickerState,
    intervalDays: Int,
    onIntervalChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.confirm))
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TimePicker(state = timePickerState)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "提醒间隔",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("每")

                    OutlinedButton(
                        onClick = { if (intervalDays > 1) onIntervalChange(intervalDays - 1) },
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("-")
                    }

                    Text(
                        text = intervalDays.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.width(40.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    OutlinedButton(
                        onClick = { if (intervalDays < 30) onIntervalChange(intervalDays + 1) },
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+")
                    }

                    Text("天")
                }
            }
        }
    )
}
