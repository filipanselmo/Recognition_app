package com.example.recognition_app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.recognition_app.components.TaskItem
import com.example.recognition_app.dto.Task
import com.example.recognition_app.fetchTasks
import com.example.recognition_app.updateTaskStatus
import kotlinx.coroutines.launch

@Composable
fun TaskScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    // Состояние для отображения диалога
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        fetchTasks { fetchedTasks ->
            tasks = fetchedTasks
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    fetchTasks { newTasks ->
                        tasks = newTasks
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Получить задания")
        }

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn {
                items(tasks) { task ->
                    TaskItem(task) { taskId ->
                        selectedTaskId = taskId
                        showConfirmationDialog = true
                    }
                }
            }
        }

        // Отображаем диалог при необходимости
        if (showConfirmationDialog && selectedTaskId != null) {
            ConfirmationDialog(
                taskId = selectedTaskId!!,
                onConfirm = {
                    scope.launch {
                        updateTaskStatus(selectedTaskId!!, "in_progress")
                        navController.navigate("photo_capture/${selectedTaskId}")
                    }
                    showConfirmationDialog = false
                },
                onDismiss = { showConfirmationDialog = false }
            )
        }
    }
}

// Вынесенный диалог в отдельный Composable
@Composable
fun ConfirmationDialog(
    taskId: Long,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Взять задание") },
        text = { Text("Вы действительно хотите взять задание в работу?") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Да") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Нет") }
        }
    )
}