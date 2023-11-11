package com.example.whatsapplinker

import android.telephony.PhoneNumberUtils
import android.text.Selection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.*

/**
 * This class provides phone number formatting for TextFields in compose.
 * https://medium.com/google-developer-experts/hands-on-jetpack-compose-visualtransformation-to-create-a-phone-number-formatter-99b0347fc4f6
 */
class PhoneNumberVisualTransformation(
        countryCode: String = Locale.US.country
) : VisualTransformation {

    private val phoneNumberFormatter = PhoneNumberUtil.getInstance().getAsYouTypeFormatter(countryCode)

    override fun filter(text: AnnotatedString): TransformedText {
        val transformation = reformat(text, Selection.getSelectionEnd(text))

        return TransformedText(AnnotatedString(transformation.formatted
                ?: ""), object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val arraySize = transformation.originalToTransformed.size
                val offsetToUse = offset.coerceAtLeast(0).coerceAtMost(arraySize - 1)
                return transformation.originalToTransformed[offsetToUse]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val arraySize = transformation.transformedToOriginal.size
                val offsetToUse = offset.coerceAtLeast(0).coerceAtMost(arraySize - 1)
                return transformation.transformedToOriginal[offsetToUse]
            }
        })
    }

    private fun reformat(s: CharSequence, cursor: Int): Transformation {
        phoneNumberFormatter.clear()

        val curIndex = cursor - 1
        var formatted: String? = null
        var lastNonSeparator = 0.toChar()
        var hasCursor = false

        s.forEachIndexed { index, char ->
            if (PhoneNumberUtils.isNonSeparator(char)) {
                if (lastNonSeparator.code != 0) {
                    formatted = getFormattedNumber(lastNonSeparator, hasCursor)
                    hasCursor = false
                }
                lastNonSeparator = char
            }
            if (index == curIndex) {
                hasCursor = true
            }
        }

        if (lastNonSeparator.code != 0) {
            formatted = getFormattedNumber(lastNonSeparator, hasCursor)
        }
        val originalToTransformed = mutableListOf<Int>()
        val transformedToOriginal = mutableListOf<Int>()
        var specialCharsCount = 0
        formatted?.forEachIndexed { index, char ->
            if (PhoneNumberUtils.isNonSeparator(char)) {
                originalToTransformed.add(index)
            } else {
                specialCharsCount++
            }
            // don't store negative values
            transformedToOriginal.add(maxOf(0, index - specialCharsCount))
        }
        originalToTransformed.add(originalToTransformed.maxOrNull()?.plus(1) ?: 0)
        transformedToOriginal.add(transformedToOriginal.maxOrNull()?.plus(1) ?: 0)

        return Transformation(formatted, originalToTransformed, transformedToOriginal)
    }

    private fun getFormattedNumber(lastNonSeparator: Char, hasCursor: Boolean): String? {
        return if (hasCursor) {
            phoneNumberFormatter.inputDigitAndRememberPosition(lastNonSeparator)
        } else {
            phoneNumberFormatter.inputDigit(lastNonSeparator)
        }
    }

    private data class Transformation(
            val formatted: String?,
            val originalToTransformed: List<Int>,
            val transformedToOriginal: List<Int>
    )
}