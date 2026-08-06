package com.anees.adventurehopper.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

sealed interface AppScreen {
    object MainMenu : AppScreen
    object SceneSelection : AppScreen
    object Coloring : AppScreen
    object Gallery : AppScreen
}

data class SceneRegion(
    val id: String,
    val label: String
)

data class ColoringScene(
    val id: String,
    val name: String,
    val regions: List<SceneRegion>
)

data class SavedDrawing(
    val id: String,
    val sceneId: String,
    val sceneName: String,
    val filledRegions: Map<String, Color>
)

sealed interface SceneAction {
    data class Fill(val regionId: String, val previousColor: Color?) : SceneAction
}

val adventurePalette = listOf(
    Color(0xFFEF6161),
    Color(0xFF4EB1D2),
    Color(0xFF48B95E),
    Color(0xFFF7C948),
    Color(0xFF9B6BFF),
    Color(0xFFEE8B8B)
)

val adventureScenes = listOf(
    ColoringScene(
        id = "forest_friends",
        name = "Forest Friends",
        regions = listOf(
            SceneRegion(id = "tree", label = "Tree"),
            SceneRegion(id = "cloud", label = "Cloud"),
            SceneRegion(id = "flower", label = "Flower"),
            SceneRegion(id = "mushroom", label = "Mushroom")
        )
    ),
    ColoringScene(
        id = "playful_puppy",
        name = "Playful Puppy",
        regions = listOf(
            SceneRegion(id = "body", label = "Body"),
            SceneRegion(id = "ear", label = "Ear"),
            SceneRegion(id = "ball", label = "Ball"),
            SceneRegion(id = "grass", label = "Grass")
        )
    ),
    ColoringScene(
        id = "magic_castle",
        name = "Magic Castle",
        regions = listOf(
            SceneRegion(id = "tower", label = "Tower"),
            SceneRegion(id = "flag", label = "Flag"),
            SceneRegion(id = "door", label = "Door"),
            SceneRegion(id = "ground", label = "Ground")
        )
    )
)

class AdventureAppState(
    scenes: List<ColoringScene> = adventureScenes,
    palette: List<Color> = adventurePalette
) {
    var currentScreen: AppScreen by mutableStateOf(AppScreen.MainMenu)
    var selectedScene: ColoringScene? by mutableStateOf(null)
    var selectedColor: Color by mutableStateOf(palette.first())
    var filledRegions: Map<String, Color> by mutableStateOf(emptyMap())
    private var actionHistory: List<SceneAction> by mutableStateOf(emptyList())
    var savedDrawings: List<SavedDrawing> by mutableStateOf(emptyList())
    val colorPalette: List<Color> = palette
    val availableScenes: List<ColoringScene> = scenes

    fun goToMainMenu() {
        currentScreen = AppScreen.MainMenu
        selectedScene = null
        filledRegions = emptyMap()
        actionHistory = emptyList()
    }

    fun goToGallery() {
        currentScreen = AppScreen.Gallery
    }

    fun goToSceneSelection() {
        currentScreen = AppScreen.SceneSelection
    }

    fun selectScene(scene: ColoringScene) {
        selectedScene = scene
        filledRegions = emptyMap()
        actionHistory = emptyList()
        currentScreen = AppScreen.Coloring
    }

    fun chooseColor(color: Color) {
        selectedColor = color
    }

    fun fillRegion(regionId: String) {
        val previousColor = filledRegions[regionId]
        if (previousColor == selectedColor) return
        filledRegions = filledRegions.toMutableMap().also { it[regionId] = selectedColor }
        actionHistory = actionHistory + SceneAction.Fill(regionId, previousColor)
    }

    fun undoLastAction() {
        val lastAction = actionHistory.lastOrNull() as? SceneAction.Fill ?: return
        val updated = filledRegions.toMutableMap()
        if (lastAction.previousColor == null) {
            updated.remove(lastAction.regionId)
        } else {
            updated[lastAction.regionId] = lastAction.previousColor
        }
        filledRegions = updated
        actionHistory = actionHistory.dropLast(1)
    }

    fun resetScene() {
        filledRegions = emptyMap()
        actionHistory = emptyList()
    }

    fun saveDrawing() {
        val scene = selectedScene ?: return
        val entry = SavedDrawing(
            id = "saved_${savedDrawings.size + 1}",
            sceneId = scene.id,
            sceneName = scene.name,
            filledRegions = filledRegions
        )
        savedDrawings = savedDrawings + entry
    }
}
