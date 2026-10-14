package com.example.cipherspend.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cipherspend.data.db.DatabaseProvider
import com.example.cipherspend.data.model.CustomRuleEntity
import com.example.cipherspend.ui.theme.AmbientGlowBackdrop
import com.example.cipherspend.ui.theme.CyberCyan
import com.example.cipherspend.ui.theme.DarkBackground
import com.example.cipherspend.ui.theme.NeonEmerald
import com.example.cipherspend.ui.theme.PurpleGlow
import com.example.cipherspend.ui.theme.TextMuted
import com.example.cipherspend.ui.theme.TextPrimary
import com.example.cipherspend.ui.theme.TextSecondary
import com.example.cipherspend.ui.theme.glassEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

enum class NerLabelType {
    MERCHANT,
    AMOUNT,
    IGNORE
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveTrainingScreen(
    smsBody: String = "Credit Alert! Rs.861.00 credited to HDFC Bank A/c XX6334 on 27-04-26 from VPA amazon.refunds@rapl (UPI 611791926559)",
    sender: String = "HDFC-BANK",
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val rawBody = smsBody.ifBlank {
        "Credit Alert! Rs.861.00 credited to HDFC Bank A/c XX6334 on 27-04-26 from VPA amazon.refunds@rapl (UPI 611791926559)"
    }

    val words = remember(rawBody) {
        rawBody.split("\\s+".toRegex()).filter { it.isNotBlank() }
    }

    val selectedIndices = remember { mutableStateMapOf<Int, Boolean>() }
    val labeledWords = remember { mutableStateMapOf<Int, NerLabelType>() }

    var generatedRegex by remember { mutableStateOf<String?>(null) }
    var detectedMerchant by remember { mutableStateOf("") }

    val selectedCount = selectedIndices.count { it.value }

    fun buildTokenRegex(words: List<String>, labeledWords: Map<Int, NerLabelType>): String {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < words.size) {
            val label = labeledWords[i]
            when (label) {
                NerLabelType.AMOUNT -> {
                    tokens.add("""(?<amount>[0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?)""")
                    i++
                }
                NerLabelType.MERCHANT -> {
                    var j = i
                    while (j + 1 < words.size && labeledWords[j + 1] == NerLabelType.MERCHANT) {
                        j++
                    }
                    if (j == words.size - 1) {
                        tokens.add("""(?<merchant>.+)""")
                    } else {
                        tokens.add("""(?<merchant>.+?)""")
                    }
                    i = j + 1
                }
                else -> {
                    tokens.add(Pattern.quote(words[i]))
                    i++
                }
            }
        }
        return tokens.joinToString("""\s+""")
    }

    fun applyLabel(label: NerLabelType) {
        selectedIndices.filter { it.value }.keys.forEach { index ->
            labeledWords[index] = label
        }
        selectedIndices.clear()

        // Extract merchant name from labeled merchant tokens
        val merchantTokens = labeledWords
            .filter { it.value == NerLabelType.MERCHANT }
            .toSortedMap()
            .map { words[it.key] }

        if (merchantTokens.isNotEmpty()) {
            detectedMerchant = merchantTokens.joinToString(" ")
        }

        // Build token-by-token regex
        if (labeledWords.any { it.value == NerLabelType.MERCHANT || it.value == NerLabelType.AMOUNT }) {
            generatedRegex = buildTokenRegex(words, labeledWords)
        }
    }

    fun saveRule() {
        val regex = generatedRegex ?: if (labeledWords.any { it.value == NerLabelType.MERCHANT || it.value == NerLabelType.AMOUNT }) {
            buildTokenRegex(words, labeledWords)
        } else {
            val merchantTokens = labeledWords
                .filter { it.value == NerLabelType.MERCHANT }
                .toSortedMap()
                .map { words[it.key] }
            if (merchantTokens.isNotEmpty()) {
                detectedMerchant = merchantTokens.joinToString(" ")
                ".*(?i)(${Pattern.quote(detectedMerchant)}).*"
            } else {
                ".*(?i)(${Pattern.quote(sender)}).*"
            }
        }

        val merchantName = if (detectedMerchant.isNotBlank()) detectedMerchant else "Custom Rule ($sender)"

        coroutineScope.launch(Dispatchers.IO) {
            val db = DatabaseProvider.getDatabase(context)
            val rule = CustomRuleEntity(
                sender = sender,
                regexPattern = regex,
                merchantName = merchantName
            )
            db.customRuleDao().insertRule(rule)

            // Update any existing transaction with exact same body and sender with newly extracted detectedMerchant
            if (detectedMerchant.isNotBlank()) {
                db.transactionDao().updateMerchantByBodyAndSender(
                    body = rawBody,
                    sender = sender,
                    merchant = detectedMerchant
                )
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Regex rule saved successfully", Toast.LENGTH_SHORT).show()
                onNavigateBack()
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Smart Rule Editor",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AmbientGlowBackdrop()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Subheader: Label Message + Selection count
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = NeonEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LABEL MESSAGE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NeonEmerald,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (selectedCount > 0) {
                            "$selectedCount word(s) selected — choose a label below."
                        } else {
                            "Tap words to select and label them as Merchant, Amount, or Ignore."
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // FlowRow Chip Container Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassEffect(
                                shape = RoundedCornerShape(20.dp),
                                backgroundColor = Color(0xFF0F1726).copy(alpha = 0.85f)
                            )
                            .padding(18.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            words.forEachIndexed { index, word ->
                                val isSelected = selectedIndices[index] == true
                                val label = labeledWords[index]

                                val chipBg = when {
                                    isSelected -> CyberCyan.copy(alpha = 0.25f)
                                    label == NerLabelType.MERCHANT -> PurpleGlow.copy(alpha = 0.22f)
                                    label == NerLabelType.AMOUNT -> CyberCyan.copy(alpha = 0.18f)
                                    label == NerLabelType.IGNORE -> Color.White.copy(alpha = 0.04f)
                                    else -> Color.White.copy(alpha = 0.06f)
                                }

                                val chipBorder = when {
                                    isSelected -> CyberCyan
                                    label == NerLabelType.MERCHANT -> PurpleGlow
                                    label == NerLabelType.AMOUNT -> CyberCyan
                                    label == NerLabelType.IGNORE -> Color.White.copy(alpha = 0.12f)
                                    else -> Color.White.copy(alpha = 0.08f)
                                }

                                val chipTextColor = when {
                                    isSelected -> CyberCyan
                                    label == NerLabelType.MERCHANT -> PurpleGlow
                                    label == NerLabelType.AMOUNT -> CyberCyan
                                    label == NerLabelType.IGNORE -> TextMuted
                                    else -> TextPrimary
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(chipBg)
                                        .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedIndices[index] = !(selectedIndices[index] ?: false)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 9.dp)
                                ) {
                                    Text(
                                        text = word,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = chipTextColor,
                                            fontWeight = if (isSelected || label != null) FontWeight.SemiBold else FontWeight.Normal,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (generatedRegex != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassEffect(shape = RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Matching Pattern",
                                    style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = generatedRegex ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }
                    }
                }

                // Bottom Action Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 3 Label Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Merchant (PurpleGlow)
                        Button(
                            onClick = { applyLabel(NerLabelType.MERCHANT) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurpleGlow.copy(alpha = 0.25f),
                                contentColor = PurpleGlow
                            ),
                            border = BorderStroke(1.dp, PurpleGlow)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Merchant",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Amount (CyberCyan)
                        Button(
                            onClick = { applyLabel(NerLabelType.AMOUNT) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan.copy(alpha = 0.2f),
                                contentColor = CyberCyan
                            ),
                            border = BorderStroke(1.dp, CyberCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Amount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Ignore (Muted)
                        Button(
                            onClick = { applyLabel(NerLabelType.IGNORE) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.06f),
                                contentColor = TextSecondary
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ignore",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Save Rule Button
                    Button(
                        onClick = { saveRule() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Rule",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
