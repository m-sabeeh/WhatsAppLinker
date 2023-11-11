package com.example.whatsapplinker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.i18n.phonenumbers.PhoneNumberUtil
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.notifications.InitialResults
import io.realm.kotlin.notifications.ResultsChange
import io.realm.kotlin.notifications.UpdatedResults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class MainViewModel : ViewModel() {
    private val config = RealmConfiguration.create(schema = setOf(CountryData::class))
    private val realm: Realm = Realm.open(config)


    private val countriesDataResults: Flow<ResultsChange<CountryData>> =
        realm.query<CountryData>(CountryData::class)
            .sort("country")
            .asFlow()
    private val countriesData = MutableStateFlow<List<CountryData>>(emptyList())

    private val query = MutableStateFlow("")
    val filteredCountriesData: StateFlow<List<CountryData>> = query.map { query ->
        if (query.isEmpty()) {
            return@map countriesData.value
        }
        val results = realm.query<CountryData>(CountryData::class)
            .query("textToDisplay CONTAINS[c] $0", query)
            .sort("country")
            .find()
        if (results.isEmpty()) {
            return@map countriesData.value
        }
        return@map results
    }.stateIn(viewModelScope, SharingStarted.Lazily, countriesData.value)

    private val currentCountryIso = MutableStateFlow<String>("US")
    private val USAData = realm.query<CountryData>(CountryData::class)
        .query("region == $0", "US")
        .find()
        .first()
    val currentCountryData = currentCountryIso.map {
        realm.query<CountryData>(CountryData::class)
            .query("region == $0", it)
            .find()
            .first()
    }.stateIn(viewModelScope, SharingStarted.Lazily, USAData)

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
        }
        //add data to Realm
        realm.writeBlocking {
            countries.forEach { countryData ->
                copyToRealm(instance = countryData, updatePolicy = UpdatePolicy.ALL)
            }
        }

        viewModelScope.launch {
            countriesDataResults.collect() {
                when (it) {
                    is InitialResults -> {
                        countriesData.value = it.list
                    }

                    is UpdatedResults -> {
                        countriesData.value = it.list
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        realm.close()
    }

    fun searchCountries(query: String) {
        this.query.value = query
    }

    fun updateCurrentCountry(countryISO: String) {
        currentCountryIso.value = countryISO
    }
}