@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.a8000053398.printly.ui.export

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.a8000053398.printly.model.ExportRecord
import com.a8000053398.printly.service.persistence.ExportHistoryStore
import com.a8000053398.printly.ui.components.IconBadge
import com.a8000053398.printly.ui.components.cardStyle
import com.a8000053398.printly.ui.theme.Metrics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Lists every export [ExportHistoryStore] has kept a copy of, newest first —
 * reachable from Settings. Tapping a row re-opens the Android sharesheet for it. */
@Composable
fun ExportHistoryScreen(onClose: () -> Unit) {
    val store = ExportHistoryStore.shared
    val records by store.records.collectAsState()
    val context = LocalContext.current
    var recordPendingDelete by remember { mutableStateOf<ExportRecord?>(null) }
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Export History") },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }) { padding ->
        if (records.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                Text("No Exports Yet", fontWeight = FontWeight.SemiBold)
                Text(
                    "PDFs and images you generate from the Export screen appear here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Metrics.spacingL.dp),
                verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cardStyle()
                            .padding(Metrics.spacingM.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)
                    ) {
                        IconBadge(com.a8000053398.printly.ui.theme.IconMapping.icon(record.format.iconName), size = 44.dp)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(record.projectName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${record.format.displayName} · ${record.pageCount} ${if (record.pageCount == 1) "page" else "pages"}",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(dateFormat.format(Date(record.createdAt)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        IconButton(onClick = {
                            val files = store.files(record)
                            val uris = files.map { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it) }
                            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = if (record.format == com.a8000053398.printly.model.ExportedFileFormat.PDF) "application/pdf" else "image/*"
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share"))
                        }) { Icon(Icons.Filled.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = { recordPendingDelete = record }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    val pending = recordPendingDelete
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { recordPendingDelete = null },
            title = { Text("Delete Export") },
            text = { Text("This only removes it from Export History — it doesn't affect the original project.") },
            confirmButton = { TextButton(onClick = { store.delete(pending); recordPendingDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { recordPendingDelete = null }) { Text("Cancel") } }
        )
    }
}
