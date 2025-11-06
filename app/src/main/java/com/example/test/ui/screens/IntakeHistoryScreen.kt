package com.example.test.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.test.R
import com.example.test.data.model.MedicineIntake
import com.example.test.ui.viewmodel.MedicineViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeHistoryScreen(
    viewModel: MedicineViewModel,
    onNavigateBack: () -> Unit
) {
    val intakeHistory by viewModel.intakeHistory.collectAsState()
    val medicines by viewModel.medicines.collectAsState()

    // Create a map of medicineId to medicine name for easy lookup
    val medicineNames = remember(medicines) {
        medicines.associate { it.medicine.id to it.medicine.name }
    }

    // Calendar state
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }

    // Group intakes by date
    val intakesByDate = remember(intakeHistory) {
        intakeHistory.groupBy { intake ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = intake.scheduledTime
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.intake_history)) },
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Month navigation
            MonthNavigator(
                currentMonth = currentMonth,
                onPreviousMonth = {
                    currentMonth = Calendar.getInstance().apply {
                        timeInMillis = currentMonth.timeInMillis
                        add(Calendar.MONTH, -1)
                    }
                },
                onNextMonth = {
                    currentMonth = Calendar.getInstance().apply {
                        timeInMillis = currentMonth.timeInMillis
                        add(Calendar.MONTH, 1)
                    }
                }
            )

            Divider()

            // Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Green legend
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.all_taken),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Red legend
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.some_missed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Calendar grid
            CalendarView(
                currentMonth = currentMonth,
                intakesByDate = intakesByDate,
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    selectedDate = if (selectedDate?.timeInMillis == date.timeInMillis) null else date
                }
            )

            Divider()

            // Selected date details or month summary
            if (selectedDate != null) {
                val dateKey = selectedDate!!.timeInMillis
                val dayIntakes = intakesByDate[dateKey] ?: emptyList()

                Text(
                    text = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(selectedDate!!.time),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )

                if (dayIntakes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_records_today),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dayIntakes) { intake ->
                            IntakeHistoryItem(
                                intake = intake,
                                medicineName = medicineNames[intake.medicineId] ?: stringResource(R.string.unknown_medicine)
                            )
                        }
                    }
                }
            } else {
                // Show month summary
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.click_date_for_details),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun IntakeHistoryItem(
    intake: MedicineIntake,
    medicineName: String
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val scheduledDate = remember(intake.scheduledTime) {
        dateFormat.format(Date(intake.scheduledTime))
    }
    val takenDate = remember(intake.takenTime) {
        dateFormat.format(Date(intake.takenTime))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (intake.wasTaken)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = medicineName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (intake.wasTaken) stringResource(R.string.was_taken) else stringResource(R.string.not_taken),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (intake.wasTaken)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = stringResource(R.string.scheduled_time, scheduledDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (intake.wasTaken) {
                Text(
                    text = stringResource(R.string.taken_time, takenDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MonthNavigator(
    currentMonth: Calendar,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthFormat = remember { SimpleDateFormat("yyyy年 MM月", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_month))
        }

        Text(
            text = monthFormat.format(currentMonth.time),
            style = MaterialTheme.typography.titleLarge
        )

        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = stringResource(R.string.next_month))
        }
    }
}

@Composable
fun CalendarView(
    currentMonth: Calendar,
    intakesByDate: Map<Long, List<MedicineIntake>>,
    selectedDate: Calendar?,
    onDateSelected: (Calendar) -> Unit
) {
    val daysOfWeek = listOf("日", "一", "二", "三", "四", "五", "六")

    // Get first day of month and number of days
    val firstDayOfMonth = Calendar.getInstance().apply {
        timeInMillis = currentMonth.timeInMillis
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val startDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

    Column(modifier = Modifier.padding(8.dp)) {
        // Day headers
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar grid
        val totalCells = (startDayOfWeek + daysInMonth)
        val rows = (totalCells + 6) / 7

        Column {
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayOfMonth = cellIndex - startDayOfWeek + 1

                        if (dayOfMonth in 1..daysInMonth) {
                            val date = Calendar.getInstance().apply {
                                timeInMillis = currentMonth.timeInMillis
                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            val dateKey = date.timeInMillis
                            val intakes = intakesByDate[dateKey] ?: emptyList()
                            val hasTaken = intakes.any { it.wasTaken }
                            val hasMissed = intakes.any { !it.wasTaken }
                            val isSelected = selectedDate?.timeInMillis == dateKey

                            CalendarDay(
                                day = dayOfMonth,
                                hasTaken = hasTaken,
                                hasMissed = hasMissed,
                                isSelected = isSelected,
                                onClick = { onDateSelected(date) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDay(
    day: Int,
    hasTaken: Boolean,
    hasMissed: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )

            // Indicator: Green dot if all taken, Red dot if any missed
            Box(
                modifier = Modifier.height(8.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    hasMissed -> {
                        // Red dot: Any medicine was missed
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                    hasTaken -> {
                        // Green dot: All medicines taken
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary)
                        )
                    }
                }
            }
        }
    }
}
