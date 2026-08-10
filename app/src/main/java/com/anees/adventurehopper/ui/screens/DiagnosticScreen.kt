package com.anees.adventurehopper.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anees.adventurehopper.ui.diagnostic.DiagnosticAnswer
import com.anees.adventurehopper.ui.diagnostic.DiagnosticCategory
import com.anees.adventurehopper.ui.diagnostic.DiagnosticQuestionType
import com.anees.adventurehopper.ui.diagnostic.DiagnosticResult

@Composable
fun DiagnosticScreen(
    category: DiagnosticCategory,
    onBack: () -> Unit,
    onComplete: (List<DiagnosticAnswer>, String) -> Unit
) {
    var questionIndex by remember(category) { mutableIntStateOf(0) }
    var answers by remember(category) {
        mutableStateOf(List<DiagnosticAnswer?>(category.questions.size) { null })
    }
    var description by remember(category) { mutableStateOf("") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF4F8FC)
        ) {
            if (category.questions.isEmpty()) {
                OtherIssueContent(
                    category = category,
                    description = description,
                    onDescriptionChanged = { description = it },
                    onBack = onBack,
                    onComplete = { onComplete(emptyList(), description) }
                )
            } else {
                val question = category.questions[questionIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FlowHeader(title = category.title, onBack = onBack)
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = "שאלה ${questionIndex + 1} מתוך ${category.questions.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B6E99)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = question.text,
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF102A43)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    val answerOptions = when (question.type) {
                        DiagnosticQuestionType.YES_NO -> listOf(
                            "כן" to DiagnosticAnswer.YES,
                            "לא" to DiagnosticAnswer.NO,
                            "לא יודע" to DiagnosticAnswer.UNKNOWN
                        )
                        DiagnosticQuestionType.MULTIPLE_CHOICE -> question.options.map { option ->
                            option to DiagnosticAnswer.MULTIPLE_CHOICE(option)
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        answerOptions.forEach { (text, answer) ->
                            AnswerCard(
                                text = text,
                                selected = answers[questionIndex] == answer,
                                onClick = {
                                    answers = answers.updated(questionIndex, answer)
                                    advanceQuestion(
                                        questionIndex = questionIndex,
                                        questionCount = category.questions.size,
                                        answers = answers.updated(questionIndex, answer),
                                        onComplete = { completedAnswers -> onComplete(completedAnswers, description) },
                                        onNext = { questionIndex++ }
                                    )
                                }
                            )
                            }
                    }
                    if (questionIndex > 0) {
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(onClick = { questionIndex-- }) {
                            Text(text = "לשאלה הקודמת")
                        }
                    }
                }
            }
        }
    }
}

private fun advanceQuestion(
    questionIndex: Int,
    questionCount: Int,
    answers: List<DiagnosticAnswer?>,
    onComplete: (List<DiagnosticAnswer>) -> Unit,
    onNext: () -> Unit
) {
    if (questionIndex == questionCount - 1) {
        onComplete(answers.map { it ?: DiagnosticAnswer.UNKNOWN })
    } else {
        onNext()
    }
}

@Composable
private fun OtherIssueContent(
    category: DiagnosticCategory,
    description: String,
    onDescriptionChanged: (String) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FlowHeader(title = category.title, onBack = onBack)
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "תאר בקצרה את הבעיה",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF102A43)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            modifier = Modifier.fillMaxWidth(),
            minLines = 5,
            shape = RoundedCornerShape(16.dp),
            label = { Text("תיאור התקלה") }
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = "הצג המלצה", fontSize = 17.sp)
        }
    }
}

@Composable
private fun FlowHeader(title: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            OutlinedButton(onClick = onBack) {
                Text(text = "חזרה לתקלות")
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF102A43)
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 16.dp),
            color = Color(0xFFD9E2EC)
        )
    }
}

@Composable
private fun AnswerCard(text: String, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        label = "answer press"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFD9F0FA) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = Color(0xFF102A43),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun DiagnosticResultScreen(
    category: DiagnosticCategory,
    result: DiagnosticResult,
    onRequestElectrician: () -> Unit,
    onBackToCategories: () -> Unit,
    onBackToHome: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF4F8FC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF52606D)
                )
                Spacer(modifier = Modifier.height(28.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.isSafetyWarning) Color(0xFFFFE4E1) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = result.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (result.isSafetyWarning) Color(0xFFB3261E) else Color(0xFF102A43)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = result.explanation,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF334E68)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "המלצת בטיחות",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (result.isSafetyWarning) Color(0xFFB3261E) else Color(0xFF0B6E99)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = result.safetyRecommendation,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF334E68)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
                Button(
                    onClick = onRequestElectrician,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0B6E99),
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "👨‍🔧 הזמנת חשמלאי באזור שלי", fontSize = 17.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onBackToCategories,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = "חזרה לתקלות", fontSize = 17.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onBackToHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = "חזרה למסך הראשי", fontSize = 17.sp)
                }
            }
        }
    }
}

private fun <T> List<T>.updated(index: Int, value: T): List<T> =
    toMutableList().also { it[index] = value }