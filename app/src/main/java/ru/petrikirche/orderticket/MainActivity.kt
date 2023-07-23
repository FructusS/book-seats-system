package ru.petrikirche.orderticket

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import ru.petrikirche.orderticket.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val list = listOf<Seat>(
            Seat(1,1,1,1,"asdasd",100f,SeatStatus.FREE_SEAT),
            Seat(6,1,1,1,"asdasd",100f,SeatStatus.FREE_SEAT),
            Seat(1,1,1,1,"asdasd",100f,SeatStatus.FREE_SEAT),
            Seat(2,1,1,1,"asdasd",100f,SeatStatus.ISSUED_SEAT),
            Seat(3,1,1,1,"asdasd",100f,SeatStatus.ISSUED_SEAT),
            Seat(4,1,1,1,"asdasd",100f,SeatStatus.ISSUED_SEAT),
            Seat(5,1,1,1,"asdasd",100f,SeatStatus.ISSUED_SEAT),
            Seat(1,2,1,1,"asdasd",100f,SeatStatus.NOT_AVAILABLE_SEAT),
            Seat(1,3,1,1,"asdasd",100f,SeatStatus.NOT_AVAILABLE_SEAT),
            Seat(1,4,1,1,"asdasd",100f,SeatStatus.NOT_AVAILABLE_SEAT),
            Seat(1,5,1,1,"asdasd",100f,SeatStatus.NOT_AVAILABLE_SEAT),
            Seat(1,6,1,1,"asdasd",100f,SeatStatus.ORDERED_SEAT),
            Seat(1,7,1,1,"asdasd",100f,SeatStatus.ORDERED_SEAT),
            Seat(1,8,1,1,"asdasd",100f,SeatStatus.ORDERED_SEAT),
        )

        binding.orderTicketView.setSeats(list)


    }
}