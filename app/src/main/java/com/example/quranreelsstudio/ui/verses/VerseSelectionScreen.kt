package com.example.quranreelsstudio.ui.verses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quranreelsstudio.data.QuranSurahs
import com.example.quranreelsstudio.data.Surah
import com.example.quranreelsstudio.data.TranslationLanguages
import com.example.quranreelsstudio.data.VerseSelection

private enum class SurahFilter { ALL, CURATED }

@Composable
fun VerseSelectionScreen(
    onNext: (VerseSelection) -> Unit
) {
    var selectedSurah by remember { mutableStateOf<Surah?>(null) }
    var isBrowsingDirectory by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(SurahFilter.ALL) }
    var fromAyah by remember { mutableStateOf(1) }
    var toAyah by remember { mutableStateOf(1) }
    var wantsTranslation by remember { mutableStateOf(false) }
    var translationCode by remember { mutableStateOf<String?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

            Text(text = "Quran Verses & Text Selection", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Select your Surah and verse range for the video reel, and configure optional multilingual translation text.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (isBrowsingDirectory) {
                DirectoryBrowser(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    filter = filter,
                    onFilterChange = { filter = it },
                    onSurahPicked = { surah ->
                        selectedSurah = surah
                        fromAyah = 1
                        toAyah = surah.verseCount
                        isBrowsingDirectory = false
                    }
                )
            } else if (selectedSurah == null) {
                // Nothing picked yet: same directory browser shown directly.
                DirectoryBrowser(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    filter = filter,
                    onFilterChange = { filter = it },
                    onSurahPicked = { surah ->
                        selectedSurah = surah
                        fromAyah = 1
                        toAyah = surah.verseCount
                    }
                )
            } else {
                val surah = selectedSurah!!

                SelectedSurahCard(surah = surah, onBrowseClick = { isBrowsingDirectory = true })

                Spacer(modifier = Modifier.height(16.dp))
                Text("From verse:", style = MaterialTheme.typography.bodyMedium)
                NumberStepper(
                    value = fromAyah,
                    range = 1..surah.verseCount,
                    onValueChange = { newValue ->
                        fromAyah = newValue
                        if (toAyah < fromAyah) toAyah = fromAyah
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("To verse:", style = MaterialTheme.typography.bodyMedium)
                NumberStepper(
                    value = toAyah,
                    range = fromAyah..surah.verseCount,
                    onValueChange = { toAyah = it }
                )

                Spacer(modifier = Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Add translation?", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(
                        checked = wantsTranslation,
                        onCheckedChange = {
                            wantsTranslation = it
                            if (!it) translationCode = null
                        }
                    )
                }

                if (wantsTranslation) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Choose translation language:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(TranslationLanguages.all) { lang ->
                            val isSelected = translationCode == lang.code
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                    .clickable { translationCode = lang.code }
                                    .padding(12.dp)
                            ) {
                                Text(lang.displayName)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        onNext(
                            VerseSelection(
                                surah = surah,
                                fromAyah = fromAyah,
                                toAyah = toAyah,
                                translationLanguageCode = if (wantsTranslation) translationCode else null
                            )
                        )
                    },
                    enabled = !wantsTranslation || translationCode != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Text("Next: Choose Reciter", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SelectedSurahCard(surah: Surah, onBrowseClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text("#${surah.number}", color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = "${surah.englishName}   ${surah.arabicName}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    text = "${if (surah.isMeccan) "Meccan" else "Medinan"} • ${surah.verseCount} verses in Surah",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onBrowseClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Browse 114 Surahs Directory")
        }
    }
}

@Composable
private fun DirectoryBrowser(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filter: SurahFilter,
    onFilterChange: (SurahFilter) -> Unit,
    onSurahPicked: (Surah) -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        label = { Text("Search a Surah") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(10.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChipBox(
            label = "All 114 Surahs",
            isSelected = filter == SurahFilter.ALL,
            onClick = { onFilterChange(SurahFilter.ALL) }
        )
        FilterChipBox(
            label = "24+ Curated Passages",
            isSelected = filter == SurahFilter.CURATED,
            onClick = { onFilterChange(SurahFilter.CURATED) }
        )
    }
    Spacer(modifier = Modifier.height(12.dp))

    val filtered = remember(searchQuery, filter) {
        QuranSurahs.all
            .filter { filter == SurahFilter.ALL || it.isCurated }
            .filter {
                it.arabicName.contains(searchQuery.trim()) ||
                    it.englishName.contains(searchQuery.trim(), ignoreCase = true)
            }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filtered) { surah ->
            SurahRow(surah = surah) { onSurahPicked(surah) }
        }
    }
}

@Composable
private fun FilterChipBox(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SurahRow(surah: Surah, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "${surah.number}", modifier = Modifier.width(32.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "${surah.englishName}   ${surah.arabicName}", fontWeight = FontWeight.SemiBold)
            Text(
                text = "${if (surah.isMeccan) "Meccan" else "Medinan"} · ${surah.verseCount} verses",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun NumberStepper(value: Int, range: IntRange, onValueChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton(text = "-") { if (value - 1 >= range.first) onValueChange(value - 1) }
        Text(text = "$value", modifier = Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleLarge)
        StepperButton(text = "+") { if (value + 1 <= range.last) onValueChange(value + 1) }
    }
}

@Composable
private fun StepperButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
