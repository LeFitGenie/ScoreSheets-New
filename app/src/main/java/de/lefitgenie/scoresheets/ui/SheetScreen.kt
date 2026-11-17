package de.lefitgenie.scoresheets.ui

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.lefitgenie.scoresheets.R
import de.lefitgenie.scoresheets.ScoreSheetsApp
import de.lefitgenie.scoresheets.game.Entries
import de.lefitgenie.scoresheets.game.GameRules
import de.lefitgenie.scoresheets.game.GameType
import de.lefitgenie.scoresheets.game.FieldType
import de.lefitgenie.scoresheets.game.leaders
import de.lefitgenie.scoresheets.game.rulesFor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SheetUiState(
    val type: GameType,
    val players: List<String>,
    val dealerIndex: Int,
    val entries: Entries,
)

class SheetViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val repository = getApplication<ScoreSheetsApp>().repository
    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    val state: StateFlow<SheetUiState?> =
        combine(repository.session(sessionId), repository.entries(sessionId)) { session, entries ->
            session?.let {
                SheetUiState(
                    type = GameType.fromId(it.gameType),
                    players = it.playerNames,
                    dealerIndex = it.dealerIndex,
                    entries = entries,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveCell(row: Int, player: Int, values: Map<Int, Int?>) {
        viewModelScope.launch { repository.setCell(sessionId, row, player, values) }
    }

    fun nextDealer() {
        val current = state.value ?: return
        viewModelScope.launch {
            repository.setDealer(sessionId, (current.dealerIndex + 1) % current.players.size)
        }
    }
}

private val LabelWidth = 112.dp
private val MinCellWidth = 88.dp
private val RowHeight = 52.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetScreen(
    onBack: () -> Unit,
    viewModel: SheetViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state
    var editing by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current?.let { stringResource(it.type.titleRes) }.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (current != null && current.players.size > 1) {
                        IconButton(onClick = viewModel::nextDealer) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.next_dealer))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (current != null) {
            SheetTable(
                state = current,
                onCellClick = { row, player -> editing = row to player },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }

    val target = editing
    if (current != null && target != null) {
        val rules = rulesFor(current.type)
        EditDialog(
            rules = rules,
            row = target.first,
            title = "${current.players[target.second]} – ${rowLabel(rules, target.first)}",
            entries = current.entries,
            player = target.second,
            onDismiss = { editing = null },
            onSave = { values ->
                viewModel.saveCell(target.first, target.second, values)
                editing = null
            },
        )
    }
}

@Composable
private fun rowLabel(rules: GameRules, row: Int): String {
    val category = rules.paschCategory(row)
    return if (category != null) {
        stringResource(category.labelRes())
    } else {
        stringResource(R.string.round_n, row + 1)
    }
}

@Composable
private fun SheetTable(
    state: SheetUiState,
    onCellClick: (row: Int, player: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rules = remember(state.type) { rulesFor(state.type) }
    val playerCount = state.players.size
    val rowCount = rules.rowCount(playerCount, state.entries)
    val totals = List(playerCount) { rules.total(it, playerCount, state.entries) }
    val winners = leaders(totals, rules.lowestWins, hasEntries = !state.entries.isEmpty)

    BoxWithConstraints(modifier) {
        val contentWidth = maxOf(maxWidth, LabelWidth + MinCellWidth * playerCount)
        Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
            Column(Modifier.width(contentWidth).fillMaxHeight()) {
                HeaderRow(state)
                LazyColumn(Modifier.weight(1f)) {
                    items(rowCount) { row ->
                        Row(Modifier.fillMaxWidth().height(RowHeight)) {
                            LabelCell(rowLabel(rules, row))
                            for (player in 0 until playerCount) {
                                ScoreCell(
                                    rules = rules,
                                    row = row,
                                    player = player,
                                    playerCount = playerCount,
                                    entries = state.entries,
                                    onClick = { onCellClick(row, player) },
                                )
                            }
                        }
                    }
                }
                TotalsRow(rules, totals, winners)
            }
        }
    }
}

@Composable
private fun RowScope.LabelCell(text: String, bold: Boolean = false) {
    Box(
        modifier = Modifier
            .width(LabelWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(CellBorder())
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CellBorder(): BorderStroke = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun HeaderRow(state: SheetUiState) {
    val dealerDescription = stringResource(R.string.dealer)
    Row(Modifier.fillMaxWidth().height(RowHeight)) {
        Box(
            modifier = Modifier
                .width(LabelWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(CellBorder()),
        )
        state.players.forEachIndexed { index, name ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(CellBorder())
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (index == state.dealerIndex && state.players.size > 1) {
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(18.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .semantics { contentDescription = dealerDescription },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "D",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ScoreCell(
    rules: GameRules,
    row: Int,
    player: Int,
    playerCount: Int,
    entries: Entries,
    onClick: () -> Unit,
) {
    val score = rules.rowScore(row, player, playerCount, entries)
    val detail = cellDetail(rules, row, player, entries)
    val fieldType = rules.fieldType(row)
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .border(CellBorder())
            .then(if (fieldType == FieldType.DEFAULT) Modifier.clickable(onClick = onClick) else Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            }
            Text(
                text = score?.toString().orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Kleine Zusatzzeile bei Spielen mit mehreren Feldern, z. B. "2/1" für Ansage und Stiche. */
private fun cellDetail(rules: GameRules, row: Int, player: Int, entries: Entries): String? {
    if (rules.fields.size < 2) return null
    val first = entries[row, 0, player]
    val second = entries[row, 1, player]
    val bonus = if (rules.fields.size > 2) entries[row, 2, player] else null
    if (first == null && second == null && bonus == null) return null
    val main = "${first ?: "–"}/${second ?: "–"}"
    return if (bonus != null && bonus != 0) "$main +$bonus" else main
}

@Composable
private fun TotalsRow(rules: GameRules, totals: List<Int>, winners: Set<Int>) {
    val reachedDescription = stringResource(R.string.target_reached)
    Row(Modifier.fillMaxWidth().height(RowHeight)) {
        LabelCell(stringResource(R.string.total), bold = true)
        totals.forEachIndexed { index, total ->
            val isLeader = index in winners
            val reached = rules.targetScore?.let { total >= it } == true
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (isLeader) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    )
                    .border(CellBorder()),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = total.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (reached) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = reachedDescription,
                            modifier = Modifier.padding(start = 4.dp).size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun sanitize(input: String, allowNegative: Boolean): String {
    val negative = allowNegative && input.startsWith("-")
    val digits = input.filter { it.isDigit() }.take(6)
    return if (negative) "-$digits" else digits
}

@Composable
private fun EditDialog(
    rules: GameRules,
    row: Int,
    title: String,
    entries: Entries,
    player: Int,
    onDismiss: () -> Unit,
    onSave: (Map<Int, Int?>) -> Unit,
) {
    val texts = remember {
        mutableStateListOf<String>().apply {
            rules.fields.indices.forEach { add(entries[row, it, player]?.toString().orEmpty()) }
        }
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Pro Feld: gültig, wenn leer oder eine Zahl im erlaubten Bereich.
    val valid = rules.fields.indices.map { field ->
        val text = texts[field]
        if (text.isEmpty()) {
            true
        } else {
            val value = text.toIntOrNull()
            val min = if (rules.allowNegative(field)) Int.MIN_VALUE else 0
            val max = rules.maxValue(row, field) ?: Int.MAX_VALUE
            value != null && value in min..max && rules.isValid(row, field)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rules.fields.forEachIndexed { field, kind ->
                    val allowNegative = rules.allowNegative(field)
                    val max = rules.maxValue(row, field)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = texts[field],
                            onValueChange = { texts[field] = sanitize(it, allowNegative) },
                            label = { Text(stringResource(kind.labelRes())) },
                            singleLine = true,
                            isError = !valid[field],
                            supportingText = if (!valid[field] && max != null) {
                                { Text(stringResource(R.string.out_of_range, if (allowNegative) Int.MIN_VALUE else 0, max)) }
                            } else {
                                null
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .then(if (field == 0) Modifier.focusRequester(focusRequester) else Modifier),
                        )
                        if (allowNegative) {
                            TextButton(onClick = {
                                val text = texts[field]
                                texts[field] = if (text.startsWith("-")) text.drop(1) else "-$text"
                            }) { Text("±") }
                        }
                    }
                }
                if (rules.type == GameType.RUMMY) {
                    TextButton(onClick = {
                        val value = texts[0].toIntOrNull() ?: 0
                        texts[0] = (value * 2).toString()
                    }) { Text("×2  " + stringResource(R.string.double_score)) }
                }
                if (rules.type == GameType.PASCH) {
                    TextButton(onClick = { texts[0] = "0" }) { Text(stringResource(R.string.strike_out)) }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid.all { it },
                onClick = {
                    onSave(texts.mapIndexed { field, text -> field to text.toIntOrNull() }.toMap())
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave(rules.fields.indices.associateWith { null }) }) {
                    Text(stringResource(R.string.clear))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
