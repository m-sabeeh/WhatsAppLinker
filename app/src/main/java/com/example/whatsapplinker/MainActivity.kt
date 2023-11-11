package com.example.whatsapplinker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.TelephonyManager
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.whatsapplinker.ui.theme.WhatsAppLinkerTheme
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    data class PhoneData(
        val unformattedNumber: String,
        val textToShow: String,
        val countryCode: Int,
        val region: String
    )

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fun getStringFromIntent(): String {
            return intent.getStringExtra(Intent.EXTRA_TEXT) ?: intent.getCharSequenceExtra(
                Intent.EXTRA_PROCESS_TEXT
            )?.toString() ?: ""
        }

        val data = getStringFromIntent().let {
            //format number
            val unformattedNumber = it.replace("[^0-9]".toRegex(), "")
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val countryISO = telephonyManager.networkCountryIso.uppercase()
            val currentRegion =
                PhoneNumberUtil.getInstance().supportedRegions.firstOrNull { region ->
                    countryISO == region
                } ?: "US"
            val countryCode = PhoneNumberUtil.getInstance().getCountryCodeForRegion(currentRegion)
            val country = Locale("", currentRegion).displayCountry
            val textToShow = "$country (+$countryCode)"
            PhoneData(
                unformattedNumber = unformattedNumber,
                textToShow = textToShow,
                countryCode = countryCode,
                region = currentRegion
            )
        }

        setContent {
            WhatsAppLinkerTheme {
                // A surface container using the 'background' color from the theme
                Scaffold {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.weight(1F))
                            val context = LocalContext.current
                            var phoneNumber by remember {
                                mutableStateOf(
                                    data.unformattedNumber
                                )
                            }
                            var regionToUse by remember {
                                mutableStateOf(data.region)
                            }

                            PhoneNumberInput(
                                value = phoneNumber,
                                onValueChange = {
                                    phoneNumber = it
                                },
                                region = regionToUse
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            var countryCodeAndName by remember {
                                mutableStateOf(
                                    data.textToShow
                                )
                            }
                            var countryCode by remember() {
                                mutableStateOf(data.countryCode)
                            }
                            EditableExposedDropdownMenu(
                                value = countryCodeAndName,
                                onValueChange = {
                                    countryCodeAndName = it
                                    countryCode = -1
                                },
                                onCountrySelected = { countryData ->
                                    countryCode = countryData.countryCode
                                    countryCodeAndName = countryData.textToDisplay
                                    regionToUse = countryData.region
                                },
                                codeWrong = countryCode == -1
                            )
                            Spacer(modifier = Modifier.weight(0.25F))
                            val phoneNumberError = !isValidPhoneNumber(phoneNumber, regionToUse)
                            val formattedNumber = getFormattedNumber(phoneNumber, regionToUse)
                            val statusText = buildString {
                                if (phoneNumberError) {
                                    append("* Please enter a valid $regionToUse phone number.")
                                }
                                if (countryCode == -1) {
                                    append("\n")
                                    append("* Please select a country.")
                                }
                                append("\n")
                                append(formattedNumber.toWhatsAppUri())
                            }

                            Text(
                                textAlign = TextAlign.Center,
                                text = statusText,
                                color = if (countryCode == -1 || phoneNumberError) {
                                    Color(0xFFF92D56)
                                } else {
                                    Color.Unspecified
                                },
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = Color(0xFF25D366),
                                    contentColor = Color.Black
                                ),
                                onClick = {
                                    //start OpenWhatsApp activity
                                    startActivity(
                                        Intent(context, OpenWhatsApp::class.java).apply {
                                            putExtra(
                                                FORMATTED,
                                                formattedNumber
                                            )
                                        }
                                    )
                                }
                            ) {
                                Text(text = "Open WhatsApp")
                            }
                            Spacer(modifier = Modifier.weight(1F))
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val FORMATTED = "FORMATTED"
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PhoneNumberInput(value: String, onValueChange: (String) -> Unit, region: String) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val keyboardController = LocalSoftwareKeyboardController.current
    var isError by rememberSaveable { mutableStateOf(false) }
    val errorRes =
        R.string.error_invalid_phone
    var rawNumber by rememberSaveable() {
        mutableStateOf(value)
    }

    fun validate(number: String): Boolean {
        isError = !isValidPhoneNumber(number, region = region)
        if (isError) {
            scope.launch {
                if (number.isBlank()) {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.error_invalid_phone)
                    )
                    return@launch
                }
                snackbarHostState.showSnackbar(context.getString(errorRes))
            }
        }
        return isError
    }

    val countryISORemembered by rememberSaveable {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val countryISO = telephonyManager.networkCountryIso.uppercase()
        mutableStateOf(countryISO)
    }

    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                // Provide localized description of the error
                if (true) error(context.getString(errorRes))
            },
        isError = isError,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                if (validate(rawNumber)) {
                    return@KeyboardActions
                }
                focusManager.moveFocus(FocusDirection.Next)
            }
        ),
        value = rawNumber,
        onValueChange = {
            isError = false
            // We do following validation to not allow bad input in the textfield i.e alphabets.
            // allow 0-9 *,#,w,+, and empty string.
            // (empty string is allowed to support cut, otherwise cut won't clear the textfield)
            if (!it.matches(Regex("^$|^[0-9*#w+]+$"))) {
                return@OutlinedTextField
            }
            rawNumber = it
            onValueChange(it)
        },
        singleLine = true,
        label = {
            val labelText = if (isError) "Phone Number*" else "Phone Number"
            Text(text = labelText)
        },
        visualTransformation = PhoneNumberVisualTransformation(countryISORemembered),
    )
}

fun isValidPhoneNumber(
    value: CharSequence?,
    region: String = Locale.getDefault().country
): Boolean {
    if (value.isNullOrBlank()) {
        return false
    }
    val util = PhoneNumberUtil.getInstance()
    return util.isPossibleNumber(value, region) || TextUtils.equals(
        value,
        "911"
    )
}

data class CountryData(
    val country: String,
    val countryCode: Int,
    val textToDisplay: String,
    val region: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun EditableExposedDropdownMenu(
    value: String,
    onValueChange: (String) -> Unit,
    onCountrySelected: (CountryData) -> Unit,
    codeWrong: Boolean = false
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val regions = PhoneNumberUtil.getInstance().supportedRegions
    val options by rememberSaveable {
        val map = regions.map { region ->
            val countryCode = PhoneNumberUtil.getInstance().getCountryCodeForRegion(region)
            val country = Locale("", region).displayCountry
            val textToDisplay = "$country (+$countryCode)"
            CountryData(
                country = country,
                countryCode = countryCode,
                textToDisplay = textToDisplay,
                region = region
            )
        }.sortedBy {
            it.country
        }
        mutableStateOf(map)
    }
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        modifier = Modifier.fillMaxWidth(),
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                }
            ),
            isError = codeWrong,
            label = {
                val labelText = if (codeWrong) "Select Country*" else "Select Country"
                Text(labelText)
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        var query by remember(value) {
            mutableStateOf(value.trim())
        }
//        value.useDebounce {
//            query = it
//        }

        // filter options based on text field value
        val filteringOptions by remember(query, options) {
            derivedStateOf {
                if (query.isEmpty()) {
                    options
                } else {
                    val filtered = options.filter {
                        it.textToDisplay.contains(query, ignoreCase = true)
                    }
                    filtered.ifEmpty {
                        options
                    }
                }
            }
        }
        if (filteringOptions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                filteringOptions.forEach { selectionOption ->
                    DropdownMenuItem(
                        onClick = {
                            onCountrySelected(selectionOption)
                            expanded = false
                        },
                        text = {
                            Text(text = selectionOption.textToDisplay)
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun <T> T.useDebounce(
    delayMillis: Long = 500L,
    // 1. couroutine scope
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    onChange: (T) -> Unit
): T {
    // 2. updating state
    val state by rememberUpdatedState(this)

    // 3. launching the side-effect handler
    DisposableEffect(state) {
        val job = coroutineScope.launch {
            delay(delayMillis)
            onChange(state)
        }
        onDispose {
            job.cancel()
        }
    }
    return state
}