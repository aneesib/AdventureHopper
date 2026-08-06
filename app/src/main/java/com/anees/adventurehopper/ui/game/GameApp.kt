package com.anees.adventurehopper.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameApp(appState: AdventureAppState) {
    Surface(modifier = Modifier.fillMaxSize()) {
        when (appState.currentScreen) {
            AppScreen.MainMenu -> MainMenuScreen(
                onStart = appState::goToSceneSelection,
                onGallery = appState::goToGallery
            )
            AppScreen.SceneSelection -> SceneSelectionScreen(
                scenes = appState.availableScenes,
                onSelectScene = appState::selectScene,
                onBack = appState::goToMainMenu
            )
            AppScreen.Coloring -> ColoringScreen(
                scene = appState.selectedScene!!,
                filledRegions = appState.filledRegions,
                selectedColor = appState.selectedColor,
                palette = appState.colorPalette,
                onSelectColor = appState::chooseColor,
                onFillRegion = appState::fillRegion,
                onUndo = appState::undoLastAction,
                onReset = appState::resetScene,
                onSave = appState::saveDrawing,
                onOpenGallery = appState::goToGallery,
                onBack = appState::goToSceneSelection
            )
            AppScreen.Gallery -> GalleryScreen(
                savedDrawings = appState.savedDrawings,
                onBack = appState::goToMainMenu
            )
        }
    }
}

@Composable
fun MainMenuScreen(onStart: () -> Unit, onGallery: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Adventure Hopper",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Start Coloring")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onGallery, modifier = Modifier.fillMaxWidth()) {
            Text(text = "View Gallery")
        }
    }
}

@Composable
fun SceneSelectionScreen(scenes: List<ColoringScene>, onSelectScene: (ColoringScene) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Choose a Scene", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            scenes.forEach { scene ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onSelectScene(scene) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFFEEF2FF), shape = CircleShape)
                                .border(2.dp, Color(0xFF8A8AFF), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = scene.name, fontSize = 18.sp)
                    }
                }
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Back")
        }
    }
}

@Composable
fun ColoringScreen(
    scene: ColoringScene,
    filledRegions: Map<String, Color>,
    selectedColor: Color,
    palette: List<Color>,
    onSelectColor: (Color) -> Unit,
    onFillRegion: (String) -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onOpenGallery: () -> Unit,
    onBack: () -> Unit
) {
    var savedMessageVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = scene.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = onBack) { Text(text = "Back") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceEvenly) {
                scene.regions.forEach { region ->
                    RegionTile(
                        region = region,
                        color = filledRegions[region.id] ?: Color(0xFFECECEC),
                        onFill = { onFillRegion(region.id) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ColorPaletteRow(palette = palette, selectedColor = selectedColor, onSelectColor = onSelectColor)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = onUndo, modifier = Modifier.weight(1f)) { Text(text = "Undo") }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text(text = "Reset") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                onSave()
                savedMessageVisible = true
            }, modifier = Modifier.weight(1f)) {
                Text(text = "Save")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onOpenGallery, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Open Gallery")
        }
        if (savedMessageVisible) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Drawing saved!", color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun RegionTile(region: SceneRegion, color: Color, onFill: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(color, shape = RoundedCornerShape(12.dp))
            .border(2.dp, Color(0xFFB0BEC5), shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onFill)
            .padding(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = region.label, color = Color(0xFF333333), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ColorPaletteRow(palette: List<Color>, selectedColor: Color, onSelectColor: (Color) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        palette.forEach { color ->
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color, shape = CircleShape)
                    .border(
                        width = if (color == selectedColor) 4.dp else 2.dp,
                        color = if (color == selectedColor) Color.Black else Color(0xFFD1D1D1),
                        shape = CircleShape
                    )
                    .clickable { onSelectColor(color) }
            )
        }
    }
}

@Composable
fun GalleryScreen(savedDrawings: List<SavedDrawing>, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Gallery", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (savedDrawings.isEmpty()) {
                Text(text = "No saved drawings yet.", fontSize = 16.sp)
            } else {
                savedDrawings.forEach { drawing ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = drawing.sceneName, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Regions colored: ${drawing.filledRegions.size}")
                        }
                    }
                }
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Back")
        }
    }
}
