package com.example.domain.model

import java.util.Locale

enum class SupportedLanguage(
    val code: String,
    val bcp47Tag: String,
    val displayName: String,
    val nativeName: String,
    val locale: Locale,
    val aiDisclosureSample: String
) {
    HINDI(
        code = "hi",
        bcp47Tag = "hi-IN",
        displayName = "Hindi",
        nativeName = "हिन्दी",
        locale = Locale("hi", "IN"),
        aiDisclosureSample = "Namaste! Main %s ka AI assistant hoon. Woh abhi available nahi hain. Main aapki kya madad kar sakta hoon?"
    ),
    PUNJABI(
        code = "pa",
        bcp47Tag = "pa-IN",
        displayName = "Punjabi",
        nativeName = "ਪੰਜਾਬੀ",
        locale = Locale("pa", "IN"),
        aiDisclosureSample = "Sat Sri Akal! Main %s da AI assistant haan. Oh es vele available nahi han. Main tuhadi ki madad kar sakda haan?"
    ),
    ENGLISH(
        code = "en",
        bcp47Tag = "en-IN",
        displayName = "English",
        nativeName = "English",
        locale = Locale("en", "IN"),
        aiDisclosureSample = "Hello! I am %s's AI phone assistant. They are unavailable right now. May I have your name and reason for calling?"
    ),
    URDU(
        code = "ur",
        bcp47Tag = "ur-IN",
        displayName = "Urdu",
        nativeName = "اردو",
        locale = Locale("ur", "IN"),
        aiDisclosureSample = "Assalam-o-Alaikum! Main %s ka AI assistant hoon. Woh abhi dastiyab nahi hain. Main aapki kya madad kar sakta hoon?"
    ),
    KASHMIRI(
        code = "ks",
        bcp47Tag = "ks-IN",
        displayName = "Kashmiri",
        nativeName = "कॉशुर / کٲشُر",
        locale = Locale("ks", "IN"),
        aiDisclosureSample = "Aadaab! Bi chhus %s sund AI assistant. Tim chhi na woni available. Tuhi kya madad chhi zaroorat?"
    ),
    BENGALI(
        code = "bn",
        bcp47Tag = "bn-IN",
        displayName = "Bengali",
        nativeName = "বাংলা",
        locale = Locale("bn", "IN"),
        aiDisclosureSample = "Nomoshkar! Ami %s-er AI assistant. Uni ekhon byasto achen. Ami apnake kibhabe sahajyo korte pari?"
    ),
    GUJARATI(
        code = "gu",
        bcp47Tag = "gu-IN",
        displayName = "Gujarati",
        nativeName = "ગુજરાતી",
        locale = Locale("gu", "IN"),
        aiDisclosureSample = "Namaste! Hu %s no AI assistant chhu. Teo haal available nathi. Hu tamari shu madad kari shaku?"
    ),
    MARATHI(
        code = "mr",
        bcp47Tag = "mr-IN",
        displayName = "Marathi",
        nativeName = "मराठी",
        locale = Locale("mr", "IN"),
        aiDisclosureSample = "Namaskar! Mi %s cha AI assistant aahe. Te sadhya uplabdha nahit. Mi tumchi kay madat karu shakto?"
    ),
    TAMIL(
        code = "ta",
        bcp47Tag = "ta-IN",
        displayName = "Tamil",
        nativeName = "தமிழ்",
        locale = Locale("ta", "IN"),
        aiDisclosureSample = "Vanakkam! Naan %s-in AI udhaviyalar. Avargal ippozhudhu kidaikkavillai. Ungalukku enna udhavi vendum?"
    ),
    TELUGU(
        code = "te",
        bcp47Tag = "te-IN",
        displayName = "Telugu",
        nativeName = "తెలుగు",
        locale = Locale("te", "IN"),
        aiDisclosureSample = "Namaskaram! Nenu %s yokka AI assistant ni. Varu ipudu అందుబాటులో లేరు. Nenu meeku ela sahayam cheyagalanu?"
    ),
    KANNADA(
        code = "kn",
        bcp47Tag = "kn-IN",
        displayName = "Kannada",
        nativeName = "ಕನ್ನಡ",
        locale = Locale("kn", "IN"),
        aiDisclosureSample = "Namaskara! Naanu %s avara AI assistant. Avaru eega labhyavilla. Naanu nimage hege sahaya madabahudu?"
    ),
    MALAYALAM(
        code = "ml",
        bcp47Tag = "ml-IN",
        displayName = "Malayalam",
        nativeName = "മലയാളം",
        locale = Locale("ml", "IN"),
        aiDisclosureSample = "Namaskaram! Njan %s-nte AI assistant aanu. Avar ippol labhyamalla. Enikku enthu sahayam cheyyan kazhiyum?"
    ),
    OTHER(
        code = "und",
        bcp47Tag = "en-US",
        displayName = "Other / Mixed",
        nativeName = "Multilingual",
        locale = Locale.ENGLISH,
        aiDisclosureSample = "Hello! I am %s's AI assistant. How may I assist you and take a message?"
    );

    companion object {
        fun fromCode(code: String): SupportedLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
    }
}
