package com.example.rewardshub

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private sealed interface Dlg {
    data object Add : Dlg
    data class Rename(val acc: Account) : Dlg
    data class Delete(val acc: Account) : Dlg
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(state: AppState) {
    var dialog by remember { mutableStateOf<Dlg?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val acc = state.selected

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box2 {
                        TextButton(onClick = { menuOpen = true }) {
                            Text(acc?.name ?: "Sin cuenta", style = MaterialTheme.typography.titleMedium)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Cambiar cuenta")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            state.accounts.forEach { a ->
                                DropdownMenuItem(
                                    text = { Text(a.name) },
                                    onClick = { state.select(a); menuOpen = false }
                                )
                            }
                            if (state.accounts.isNotEmpty()) HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Agregar cuenta") },
                                leadingIcon = { Icon(Icons.Default.Add, null) },
                                onClick = { menuOpen = false; dialog = Dlg.Add }
                            )
                            if (acc != null) {
                                DropdownMenuItem(
                                    text = { Text("Renombrar") },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                                    onClick = { menuOpen = false; dialog = Dlg.Rename(acc) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Eliminar") },
                                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                                    onClick = { menuOpen = false; dialog = Dlg.Delete(acc) }
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (acc != null && state.tab != Tab.HOY) {
                        val s = state.sessionFor(acc)
                        TextButton(onClick = { state.toggleDesktop(s) }) {
                            Text(if (s.desktop) "Modo: PC" else "Modo: Móvil")
                        }
                        IconButton(onClick = { s.webView.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Recargar")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = state.tab == t,
                        onClick = { state.tab = t },
                        icon = {
                            Icon(
                                when (t) {
                                    Tab.HOY -> Icons.Default.Home
                                    Tab.BUSCAR -> Icons.Default.Search
                                    Tab.TARJETAS -> Icons.Default.Star
                                    Tab.PUNTOS -> Icons.Default.Info
                                }, contentDescription = t.label
                            )
                        },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            when {
                acc == null -> EmptyState { dialog = Dlg.Add }
                state.tab == Tab.HOY -> ChecklistScreen(state)
                else -> key(acc.id) { WebScreen(state, acc, state.tab) }
            }
        }
    }

    when (val d = dialog) {
        Dlg.Add -> NameDialog("Nueva cuenta", "", "Agregar", { dialog = null }) {
            state.addAccount(it); dialog = null
        }
        is Dlg.Rename -> NameDialog("Renombrar cuenta", d.acc.name, "Guardar", { dialog = null }) {
            state.rename(d.acc, it); dialog = null
        }
        is Dlg.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Eliminar \"${d.acc.name}\"") },
            text = { Text("Se borra su sesión y su checklist de esta app. La cuenta de Microsoft no se toca.") },
            confirmButton = { TextButton(onClick = { state.remove(d.acc); dialog = null }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancelar") } }
        )
        null -> {}
    }
}

@Composable
private fun Box2(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box { content() }
}

@Composable
private fun NameDialog(
    title: String, initial: String, confirmLabel: String,
    onDismiss: () -> Unit, onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                label = { Text("Nombre (ej. Personal, Mamá)") }, singleLine = true
            )
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text) }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Agrega tu primera cuenta", style = MaterialTheme.typography.titleLarge)
        Text(
            "Cada cuenta tiene su propia sesión de Microsoft, aislada de las demás.",
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Button(onClick = onAdd) { Text("Agregar cuenta") }
    }
}

@Composable
private fun WebScreen(state: AppState, acc: Account, tab: Tab) {
    val session = state.sessionFor(acc)
    BackHandler {
        if (session.webView.canGoBack()) session.webView.goBack() else state.tab = Tab.HOY
    }
    Column(Modifier.fillMaxSize()) {
        if (session.progress < 100) {
            LinearProgressIndicator(
                progress = { session.progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = {
                (session.webView.parent as? ViewGroup)?.removeView(session.webView)
                session.webView
            },
            update = {
                if (session.tab != tab) {
                    session.tab = tab
                    tab.url?.let { u -> it.loadUrl(u) }
                }
            }
        )
    }
}

@Composable
private fun ChecklistScreen(state: AppState) {
    val acc = state.selected ?: return
    val done = state.done(acc.id)

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Hoy · ${acc.name}", style = MaterialTheme.typography.titleMedium)
                    Text("${done.size} de ${TASKS.size} tareas")
                    LinearProgressIndicator(
                        progress = { done.size / TASKS.size.toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
        }
        items(TASKS) { t ->
            Row(
                Modifier.fillMaxWidth().clickable { state.toggleTask(acc.id, t.id) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = t.id in done, onCheckedChange = { state.toggleTask(acc.id, t.id) })
                Column {
                    Text(t.label, style = MaterialTheme.typography.bodyLarge)
                    Text(t.hint, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            OutlinedButton(onClick = { state.resetToday(acc.id) }) { Text("Reiniciar checklist de hoy") }
        }
        if (state.accounts.size > 1) {
            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Todas las cuentas", style = MaterialTheme.typography.titleMedium)
            }
            items(state.accounts) { a ->
                Row(
                    Modifier.fillMaxWidth().clickable { state.select(a) }.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(a.name + if (a.id == acc.id) "  (actual)" else "")
                    Text("${state.done(a.id).size}/${TASKS.size}")
                }
            }
        }
        if (!state.profilesSupported) {
            item {
                Text(
                    "Aviso: tu WebView no soporta perfiles aislados. Las cuentas compartirían la sesión; " +
                        "actualiza \"Android System WebView\" desde Play Store.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}
