package de.lefitgenie.scoresheets.ui

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.lefitgenie.scoresheets.R
import de.lefitgenie.scoresheets.ScoreSheetsApp
import de.lefitgenie.scoresheets.data.SessionEntity
import de.lefitgenie.scoresheets.game.GameType
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = getApplication<ScoreSheetsApp>().repository

    val sessions: StateFlow<List<SessionEntity>> = repository.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteSession(id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewGame: (GameType) -> Unit,
    onOpenSession: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<SessionEntity?>(null) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.new_game),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
			items(GameType.entries) { type ->
				Card(
					onClick = { onNewGame(type) },
					modifier = Modifier.fillMaxWidth()
				) {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(16.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						// Icon-Spalte
						Box(
							modifier = Modifier.width(56.dp),
							contentAlignment = Alignment.Center
						) {
							Image(
								painter = painterResource(type.iconRes),
								contentDescription = null,
								modifier = Modifier.size(32.dp)
							)
						}

						Spacer(modifier = Modifier.width(12.dp))

						// Text-Spalte
						Column {
							Text(
								stringResource(type.titleRes),
								style = MaterialTheme.typography.titleMedium
							)

							Text(
								text = stringResource(
									R.string.players_range,
									type.minPlayers,
									type.maxPlayers,
								),
								style = MaterialTheme.typography.bodySmall,
							)
						}
					}
				}
			}
            if (sessions.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.saved_games),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                items(sessions, key = { it.id }) { session ->
                    Card(onClick = { onOpenSession(session.id) }, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(GameType.fromId(session.gameType).titleRes),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = session.playerNames.joinToString(", "),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = dateFormat.format(Date(session.updatedAt)),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            IconButton(onClick = { pendingDelete = session }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_game_title)) },
            text = { Text(stringResource(R.string.delete_game_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(session.id)
                    pendingDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
