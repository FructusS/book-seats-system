package ru.petrikirche.orderticket

data class Seat(
    val x :  Int,
    val y :  Int,
    val place : Int,
    val row : Int,
    val info : String,
    val cost : Float,
    var status : SeatStatus
)