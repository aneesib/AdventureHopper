package com.anees.adventurehopper.ui.diagnostic

sealed interface DiagnosticAnswer {
    object YES : DiagnosticAnswer
    object NO : DiagnosticAnswer
    object UNKNOWN : DiagnosticAnswer
    data class MULTIPLE_CHOICE(val value: String) : DiagnosticAnswer
}

enum class DiagnosticQuestionType {
    YES_NO,
    MULTIPLE_CHOICE
}

data class DiagnosticQuestion(
    val text: String,
    val type: DiagnosticQuestionType = DiagnosticQuestionType.YES_NO,
    val options: List<String> = emptyList()
)

data class DiagnosticResult(
    val title: String,
    val explanation: String,
    val safetyRecommendation: String,
    val isSafetyWarning: Boolean = false
)

data class DiagnosticCategory(
    val title: String,
    val questions: List<DiagnosticQuestion>,
    val resultFor: (List<DiagnosticAnswer>) -> DiagnosticResult
)

private val cautiousResult = DiagnosticResult(
    title = "כדאי לבדוק בזהירות",
    explanation = "התשובות מצביעות על תקלה שדורשת בדיקה נוספת, אך לא ניתן לקבוע אבחנה ודאית מרחוק.",
    safetyRecommendation = "הימנע ממגע בחלקים חשופים או חיים. אם התקלה נמשכת, פנה לחשמלאי מוסמך."
)

private val generalSafetyResult = DiagnosticResult(
    title = "מומלץ לפנות לחשמלאי",
    explanation = "לא ניתן לזהות את מקור התקלה באופן בטוח מהמידע שנמסר.",
    safetyRecommendation = "הפסק שימוש בציוד או במעגל החשוד ופנה לחשמלאי מוסמך. במקרה של עשן, ניצוצות או חום חריג, התרחק מהמקום."
)

private val urgentSafetyResult = DiagnosticResult(
    title = "אזהרת בטיחות",
    explanation = "התסמינים שתיארת עלולים להעיד על סכנת התחממות או תקלה חשמלית מסוכנת.",
    safetyRecommendation = "הפסק מיד את השימוש בציוד או במעגל החשוד, התרחק מעשן או מניצוצות, ופנה בהקדם לחשמלאי מוסמך. אין לפתוח לוחות או לגעת בחוטים.",
    isSafetyWarning = true
)

val diagnosticCategories = listOf(
    DiagnosticCategory(
        title = "💡 אין חשמל",
        questions = listOf(
            DiagnosticQuestion(
                text = "האם כל הבית ללא חשמל?",
                type = DiagnosticQuestionType.MULTIPLE_CHOICE,
                options = listOf("כן, כל הבית ללא חשמל", "לא, רק חלק מהבית ללא חשמל", "לא יודע")
            ),
            DiagnosticQuestion("האם מכשירים חשמליים אחרים בבית עדיין עובדים?"),
            DiagnosticQuestion("האם אתה רואה מפסק הגנה או פחת שנמצא במצב מנותק?")
        ),
        resultFor = { answers ->
            if (answers.getOrNull(0) == DiagnosticAnswer.MULTIPLE_CHOICE("כן, כל הבית ללא חשמל")) {
                cautiousResult.copy(
                    title = "נראה שכל הבית מושפע",
                    explanation = "הפסקת חשמל בכל הבית עשויה להיות קשורה להזנה הראשית או לאמצעי הגנה.",
                    safetyRecommendation = "אל תפתח לוחות ואל תיגע בחיבורים. אם הבעיה נמשכת, פנה לחשמלאי מוסמך."
                )
            } else if (answers.getOrNull(2) == DiagnosticAnswer.YES) {
                cautiousResult.copy(
                    title = "נראה שמפסק ההגנה מנותק",
                    explanation = "ייתכן שהפסקת החשמל קשורה להפעלת אמצעי הגנה.",
                    safetyRecommendation = "אל תרים את המפסק שוב ושוב. אם הוא אינו נשאר מחובר או שהבעיה חוזרת, פנה לחשמלאי מוסמך."
                )
            } else {
                cautiousResult
            }
        }
    ),
    DiagnosticCategory(
        title = "🔌 שקע לא עובד",
        questions = listOf(
            DiagnosticQuestion("האם שקעים אחרים באותו אזור עובדים?"),
            DiagnosticQuestion(
                text = "האם הבעיה משפיעה על מכשיר אחד או כמה מכשירים?",
                type = DiagnosticQuestionType.MULTIPLE_CHOICE,
                options = listOf("מכשיר אחד", "כמה מכשירים", "לא יודע")
            ),
            DiagnosticQuestion("האם השקע נשאר לא פעיל גם לאחר זמן קצר?")
        ),
        resultFor = { answers ->
            if (answers.getOrNull(2) == DiagnosticAnswer.YES || answers.getOrNull(0) == DiagnosticAnswer.NO) {
                cautiousResult.copy(
                    title = "השקע דורש בדיקה",
                    explanation = "הבעיה נראית מתמשכת או מקומית לשקע, ולכן ייתכן שנדרשת בדיקה מקצועית.",
                    safetyRecommendation = "אל תשתמש בשקע אם הוא רופף, חם, מריח חרוך או מעלה ניצוצות. פנה לחשמלאי מוסמך."
                )
            } else {
                cautiousResult.copy(
                    title = "ייתכן שהבעיה קשורה למכשיר",
                    explanation = "אם רק מכשיר אחד אינו פועל, ייתכן שמקור התקלה הוא במכשיר עצמו.",
                    safetyRecommendation = "אל תשתמש במכשיר אם יש ריח, חום, עשן או ניצוצות. אחרת, אפשר לבדוק את המכשיר רק לפי הוראות היצרן ובאופן בטוח."
                )
            }
        }
    ),
    DiagnosticCategory(
        title = "💡 תאורה לא עובדת",
        questions = listOf(
            DiagnosticQuestion("האם גופי תאורה אחרים בבית עובדים?"),
            DiagnosticQuestion(
                text = "כמה גופי תאורה מושפעים מהבעיה?",
                type = DiagnosticQuestionType.MULTIPLE_CHOICE,
                options = listOf("גוף תאורה אחד", "כמה גופי תאורה", "לא יודע")
            ),
            DiagnosticQuestion("האם התאורה נשארת כבויה גם לאחר זמן קצר?")
        ),
        resultFor = { answers ->
            if (answers.getOrNull(2) == DiagnosticAnswer.YES || answers.getOrNull(0) == DiagnosticAnswer.NO) {
                cautiousResult.copy(
                    title = "התאורה דורשת בדיקה",
                    explanation = "הבעיה נראית מתמשכת או אינה מוגבלת לנורה אחת.",
                    safetyRecommendation = "אל תפתח גוף תאורה או תיגע בחיבורים. אם יש ריח חרוך, חום או ניצוצות, הפסק שימוש ופנה לחשמלאי מוסמך."
                )
            } else {
                cautiousResult.copy(
                    title = "ייתכן שמדובר בנורה או בגוף תאורה",
                    explanation = "כאשר שאר התאורה פועלת ורק גוף אחד מושפע, ייתכן שהבעיה מקומית.",
                    safetyRecommendation = "בצע רק החלפה בטוחה לפי הוראות היצרן, בלי לפתוח חיבורים. אם הבעיה נמשכת, פנה לחשמלאי מוסמך."
                )
            }
        }
    ),
    DiagnosticCategory(
        title = "⚡ הפחת קופץ",
        questions = listOf(
            DiagnosticQuestion("האם הפחת קופץ מיד כאשר אתה מנסה להרים אותו?"),
            DiagnosticQuestion("האם הפחת נשאר מורם כאשר המכשירים החשמליים הבעייתיים מנותקים מהשימוש?"),
            DiagnosticQuestion("האם הבעיה חוזרת שוב לאחר זמן קצר?")
        ),
        resultFor = { answers ->
            if (answers.any { it == DiagnosticAnswer.YES }) {
                urgentSafetyResult.copy(
                    title = "הפחת חוזר לקפוץ",
                    explanation = "קפיצות חוזרות של הפחת עלולות להצביע על תקלה או זליגה חשמלית.",
                    safetyRecommendation = "אל תנסה לעקוף את הפחת ואל תרים אותו שוב ושוב. הפסק שימוש בציוד החשוד ופנה לחשמלאי מוסמך."
                )
            } else {
                cautiousResult.copy(
                    title = "נדרשת תשומת לב",
                    explanation = "לא זוהה דפוס חד-משמעי, אך חשוב לעקוב אם הבעיה חוזרת.",
                    safetyRecommendation = "אם הפחת קופץ שוב, הפסק שימוש במכשירים החשודים ופנה לחשמלאי מוסמך."
                )
            }
        }
    ),
    DiagnosticCategory(
        title = "🔥 ריח או חימום חשוד",
        questions = listOf(
            DiagnosticQuestion("האם יש ריח שרוף, עשן או ניצוצות?"),
            DiagnosticQuestion("האם הציוד או השקע חמים באופן חריג?"),
            DiagnosticQuestion("האם התסמין חוזר כאשר הציוד פועל?")
        ),
        resultFor = { answers ->
            if (answers.any { it == DiagnosticAnswer.YES }) urgentSafetyResult else urgentSafetyResult.copy(
                title = "נדרשת זהירות מיוחדת",
                explanation = "גם ללא סימן חד-משמעי, ריח או חימום חשוד מצריכים בדיקה מקצועית.",
                safetyRecommendation = "הפסק שימוש בציוד או במעגל החשוד ופנה לחשמלאי מוסמך. אין לנסות לתקן לבד."
            )
        }
    ),
    DiagnosticCategory(
        title = "🌀 מכשיר חשמלי",
        questions = listOf(
            DiagnosticQuestion("האם המכשיר פועל בשקע אחר, בלי לבצע שינוי בחיווט או בחיבורים?"),
            DiagnosticQuestion("האם מכשירים אחרים פועלים באותו שקע?"),
            DiagnosticQuestion("האם הבעיה חוזרת שוב ושוב בעת הפעלת המכשיר?")
        ),
        resultFor = { answers ->
            if (answers.getOrNull(2) == DiagnosticAnswer.YES) {
                urgentSafetyResult.copy(
                    title = "המכשיר דורש בדיקה",
                    explanation = "תקלה שחוזרת בעת הפעלת המכשיר עלולה להיות קשורה למכשיר או להזנה שלו.",
                    safetyRecommendation = "הפסק שימוש במכשיר ונתק אותו רק אם ניתן לעשות זאת בבטחה, בלי לגעת בחלקים חשופים. פנה לטכנאי או לחשמלאי מוסמך."
                )
            } else {
                cautiousResult.copy(
                    title = "ייתכן שהבעיה במכשיר",
                    explanation = "ההשוואה בין מכשירים ושקעים יכולה לעזור למקד את הבדיקה, אך אינה אבחנה ודאית.",
                    safetyRecommendation = "אם יש ריח, חום, עשן, ניצוצות או פעולה חריגה, הפסק שימוש ופנה לאיש מקצוע."
                )
            }
        }
    ),
    DiagnosticCategory(
        title = "❓ משהו אחר",
        questions = emptyList(),
        resultFor = { generalSafetyResult }
    )
)