package com.example.whatsapplinker

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.annotations.PrimaryKey

open class CountryData() : RealmObject {
    @Index
    var country: String = ""

    @Index
    var countryCode: Int = 0

    @Index
    var textToDisplay: String = ""

    @PrimaryKey
    var region: String = ""

    constructor(
        country: String,
        countryCode: Int,
        textToDisplay: String,
        region: String
    ) : this() {
        this.country = country
        this.countryCode = countryCode
        this.textToDisplay = textToDisplay
        this.region = region
    }
}