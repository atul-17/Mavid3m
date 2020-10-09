package com.mavid.models

class ModelPopularOptions {
    constructor(popularCustomName: String, setAsDefault: Boolean) {
        this.popularCustomName = popularCustomName
        this.setAsDefault = setAsDefault
    }

    var popularCustomName = ""
    var setAsDefault:Boolean = false
}