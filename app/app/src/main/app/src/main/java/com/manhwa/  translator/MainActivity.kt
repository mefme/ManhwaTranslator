package com.manhwa.translator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

// 1. القاموس المخصص للتصحيح الديني
val defaultDictionary = listOf(
    "إله" to "حاكم", "إلهة" to "حاكمة", "آلهة" to "حكام",
    "عبادة" to "طاعة", "معبد" to "مقر", "كاهن" to "حارس", "كهنة" to "حراس",
    "شيطان" to "عفريت", "شياطين" to "عفاريت", "ملك شياطين" to "ملك عفريت",
    "ملاك" to "كائن نوراني", "ملائكة" to "كائنات نورانية",
    "جنة" to "عالم نوراني", "جحيم" to "عالم سفلي"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TranslatorApp()
                }
            }
        }
    }
}

@Composable
fun TranslatorApp() {
    var sourceText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showDictionary by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("مترجم المانهوا", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))
        
        OutlinedTextField(
            value = sourceText,
            onValueChange = { sourceText = it },
            label = { Text("النص الأصلي (كوري/ياباني/صيني)") },
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = {
                isLoading = true
                scope.launch(Dispatchers.IO) {
                    try {
                        val client = OkHttpClient()
                        val encodedText = URLEncoder.encode(sourceText, "UTF-8")
                        val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=ar&dt=t&q=$encodedText"
                        val request = Request.Builder().url(url).build()
                        val response = client.newCall(request).execute()
                        val body = response.body?.string() ?: ""
                        
                        // تحليل بسيط للاستجابة
                        var rawTranslation = body
                        if (body.contains("[[[")) {
                            rawTranslation = body.substringAfter("[[[\"").substringBefore("\",")
                        }
                        
                        // تطبيق تصحيحات القاموس الديني
                        var finalText = rawTranslation
                        for ((bad, good) in defaultDictionary) {
                            finalText = finalText.replace(bad, good, ignoreCase = true)
                        }
                        
                        withContext(Dispatchers.Main) {
                            translatedText = finalText
                            isLoading = false
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            translatedText = "خطأ في الترجمة: تأكد من الاتصال بالإنترنت"
                            isLoading = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) Text("جاري الترجمة...") else Text("ترجم وصحّح المصطلحات")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = translatedText,
            onValueChange = { translatedText = it },
            label = { Text("الترجمة المصححة") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            readOnly = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = { showDictionary = !showDictionary }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showDictionary) "إخفاء القاموس" else "عرض القاموس المخصص")
        }
        
        if (showDictionary) {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(defaultDictionary) { (original, replacement) ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(original, color = MaterialTheme.colorScheme.error)
                            Text("➔")
                            Text(replacement, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
