package ru.petrikirche.orderticket

typealias OnFieldChangedListener = (field: Seat) -> Unit

class SeatField constructor(    val rows: Int,
                                        val columns: Int,
                                        val seats: List<Seat>){
    val listeners = mutableSetOf<OnFieldChangedListener>()

    constructor(rows: Int, columns: Int) : this(
        rows,
        columns,
        listOf<Seat>()
    )

    init {
        listeners.forEach { listener -> seats.forEach { listener.invoke(it)} }
    }


    fun setSeat(seat: Seat){
        if (seat.status == SeatStatus.FREE_SEAT){
            seat.status = SeatStatus.SELECTED
        }
        else if (seat.status == SeatStatus.SELECTED){
            seat.status = SeatStatus.FREE_SEAT
        }
        listeners.forEach { it.invoke(seat) }

    }
}