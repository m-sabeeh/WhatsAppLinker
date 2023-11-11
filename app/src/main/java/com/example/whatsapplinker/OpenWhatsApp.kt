package com.example.whatsapplinker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.whatsapplinker.ui.theme.WhatsAppLinkerTheme
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber

class OpenWhatsApp : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.let { intent ->
            intent.getStringExtra(MainActivity.FORMATTED)?.let {
                startActivity(Intent(Intent.ACTION_VIEW, it.toWhatsAppUri()))
                finish()
                return
            }
            // launch from share menu to open WhatsApp directly
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                val unformattedNumber = it.replace("[^0-9]".toRegex(), "")
                val telephonyManager =
                    getSystemService(TELEPHONY_SERVICE) as TelephonyManager
                val currentRegion = telephonyManager.networkCountryIso.uppercase()
                val formattedNumber = getFormattedNumber(unformattedNumber, currentRegion)
                startActivity(Intent(Intent.ACTION_VIEW, formattedNumber.toWhatsAppUri()))
                finish()
            }
        }

        setContent {
            WhatsAppLinkerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                }
            }
        }
    }
}

fun getFormattedNumber(text: String, countryISO: String): String {
    var number = text
    val phoneNumber: Phonenumber.PhoneNumber?
    try {
        val util = PhoneNumberUtil.getInstance()
        phoneNumber = util.parse(number, countryISO)
        number = util.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
    } catch (e: NumberParseException) {
        e.printStackTrace()
    }

    return number
}

fun String.toWhatsAppUri(): Uri =
    Uri.parse("https://wa.me/$this")
