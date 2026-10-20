package de.lefitgenie.scoresheets.ui

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.lefitgenie.scoresheets.R
import de.lefitgenie.scoresheets.ScoreSheetsApp
import de.lefitgenie.scoresheets.data.PLAYER_SEPARATOR
import de.lefitgenie.scoresheets.game.GameType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NewGameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = getApplication<ScoreSheetsApp>().repository

    /** Ausgewählte Spieler in Spielreihenfolge; überlebt Drehungen des Geräts. */
    val selected = mutableStateListOf<String>()

    val savedPlayers: StateFlow<List<String>> = repository.savedPlayers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Fügt einen Namen hinzu, sofern er gültig und noch nicht vergeben ist. */
    fun add(rawName: String, maxPlayers: Int): Boolean {
        val name = rawName.replace(PLAYER_SEPARATOR, " ").trim()
        val duplicate = selected.any { it.equals(name, ignoreCase = true) }
        if (name.isEmpty() || duplicate || selected.size >= maxPlayers) return false
        selected.add(name)
        return true
    }

    fun remove(name: String) {
        selected.remove(name)
    }

    fun forget(name: String) {
        viewModelScope.launch { repository.forgetPlayer(name) }
    }

    fun start(type: GameType, onStarted: (Long) -> Unit) {
        val players = selected.toList()
        viewModelScope.launch {
            onStarted(repository.createSession(type, players))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewGameScreen(
    type: GameType,
    onBack: () -> Unit,
    onStarted: (Long) -> Unit,
    viewModel: NewGameViewModel = viewModel(),
) {
    val saved by viewModel.savedPlayers.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val selected = viewModel.selected
    val canAdd = selected.size < type.maxPlayers

    fun submit() {
        if (viewModel.add(input, type.maxPlayers)) input = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(type.titleRes())) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.replace("\n", "") },
                    label = { Text(stringResource(R.string.player_name)) },
                    singleLine = true,
                    enabled = canAdd,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { submit() }, enabled = canAdd && input.isNotBlank()) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_player))
                }
            }

            val availableSaved = saved.filter { name -> selected.none { it.equals(name, ignoreCase = true) } }
            if (availableSaved.isNotEmpty()) {
                Text(stringResource(R.string.saved_names), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableSaved.forEach { name ->
                        InputChip(
                            selected = false,
                            enabled = canAdd,
                            onClick = { viewModel.add(name, type.maxPlayers) },
                            label = { Text(name) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.forget_name),
                                    modifier = Modifier.size(18.dp).clickable { viewModel.forget(name) },
                                )
                            },
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.selected_players, selected.size, type.maxPlayers),
                style = MaterialTheme.typography.titleSmall,
            )
            selected.forEachIndexed { index, name ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${index + 1}. $name", modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.remove(name) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_player))
                    }
                }
            }
            if (selected.size < type.minPlayers) {
                Text(
                    text = stringResource(R.string.min_players_hint, type.minPlayers),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = { viewModel.start(type, onStarted) },
                enabled = selected.size >= type.minPlayers,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.start_game))
            }
        }
    }
}
