package ru.petrikirche.orderticket

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import ru.petrikirche.orderticket.databinding.ActivityMainBinding
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val list = mutableListOf<Seat>()
        for (i in 1..44){
            for(j in 1..44){
                val status = when(Random.nextInt(0,5)){
                    0 -> SeatStatus.ORDERED_SEAT
                    1 -> SeatStatus.ISSUED_SEAT
                    2 -> SeatStatus.NOT_AVAILABLE_SEAT
                    3 -> SeatStatus.FREE_SEAT
                    else -> SeatStatus.EMPTY
                }
                list.add(i - 1,Seat(i - 1,j - 1,j,i,"asdasd", Random.nextInt(0, 1000000).toFloat(),status))

            }

        }

        binding.orderTicketView.setSeats(list)


        binding.orderTicketView.seatClickListener = object : SeatClickListener{
            override fun onSeatClicked(seat: Seat) {
                Toast.makeText(
                    this@MainActivity,
                    "Clicked Seat: Row ${seat.row}, Place ${seat.place}",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
    }
}