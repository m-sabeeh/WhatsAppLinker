package com.example.whatsapplinker

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

class MainViewModel : ViewModel() {

    private val countriesData = MutableStateFlow<List<CountryData>>(emptyList())

    private val query = MutableStateFlow("")
    val filteredCountriesData: StateFlow<List<CountryData>> = query.map { query ->
        if (query.isEmpty()) {
            return@map countriesData.value
        }
        if (query.matches(Regex("^$|^[0-9*#w+]+$"))) {
            val results = countriesData.value.filter { countryData ->
                countryData.textToDisplay.contains(query, ignoreCase = true)
            }
            return@map results
        }
        if (query.length <= 2) {
            return@map countriesData.value.filter { countryData ->
                countryData.textToDisplay.startsWith(query, ignoreCase = true)
            }
        }
        val results = countriesData.value.filter { countryData ->
            countryData.textToDisplay.contains(query, ignoreCase = true)
        }

        if (results.isEmpty()) {
            return@map countriesData.value
        }
        return@map results
    }.stateIn(viewModelScope, SharingStarted.Lazily, countriesData.value)

    private val currentCountryIso = MutableStateFlow<String>("US")
    val currentCountryData: StateFlow<CountryData> =
        combine(countriesData, currentCountryIso) { _, iso ->
            val results = countriesData.value.firstOrNull { countryData ->
                countryData.region == iso
            }
            Log.d("TAG>>>", "current country = ${results?.countryCode}, iso = $iso")
            return@combine results ?: CountryData()
        }.stateIn(viewModelScope, SharingStarted.Lazily, CountryData())

    init {
        val regions = PhoneNumberUtil.getInstance().supportedRegions
        val countries = regions.map { region ->
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
        countriesData.value = countries
    }

    fun searchCountries(query: String) {
        this.query.value = query
    }

    fun updateCurrentCountry(countryISO: String) {
        currentCountryIso.value = countryISO
    }
}