package com.example.recognition_app.components

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.example.recognition_app.updateTaskStatus
import kotlinx.coroutines.launch

@Composable
fun showConfirmationDialog(navController: NavHostController, taskId: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope() // Получаем корутинный scope

    AlertDialog(
        onDismissRequest = { },
        title = { Text("Взять задание") },
        text = { Text("Вы действительно хотите взять задание в работу?") },
        confirmButton = {
            TextButton(onClick = {
                scope.launch { // Запускаем корутину
                    try {
                        updateTaskStatus(taskId, "in_progress")
                        navController.navigate("photo_capture/$taskId") // Передаем taskId
                    } catch (e: Exception) {
                        // Обработка ошибки
                        Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }) { Text("Да") }
        },
        dismissButton = {
            TextButton(onClick = { }) {
                Text("Нет")
            }
        }
    )
}