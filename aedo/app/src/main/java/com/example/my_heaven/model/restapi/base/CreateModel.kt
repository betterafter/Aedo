package com.example.my_heaven.model.restapi.base

data class CreateModel(
    var resident: Resident?=null,
    var place: Place ?=null,
    var deceased: Deceased ?=null,
    var eod: Eod? = null,
    var coffin: Coffin?=null,
    var dofp: Dofp?=null,
    var buried: String?= null,
    var word: String?= null,
    var agreement: String? = null,
)

data class Resident (
    var relation : String?=null,
    var name : String?=null,
    var phone : String?=null
)

data class Place(
    var place_name : String?=null,
)

data class Deceased(
    var name: String?=null,
    var age: String?=null,
)

data class Eod(
    var date: String?=null,
    var time: String?=null
)

data class Coffin(
    var date: String?=null,
    var time: String?=null
)

data class Dofp(
    var date: String?=null,
    var time: String?=null
)

