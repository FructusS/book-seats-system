package ru.petrikirche.orderticket

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.min

class OrderTicketView @JvmOverloads constructor(context : Context, attrs : AttributeSet? = null) :
    View(context, attrs) {

    private var seatList : List<Seat> = listOf()

    private lateinit var textPaint: Paint
    private lateinit var borderPaint: Paint
    private lateinit var freeSeatPaint: Paint
    private lateinit var orderedSeatPaint: Paint
    private lateinit var notAvailableSeatPaint: Paint
    private lateinit var issuedSeatPaint: Paint
    // safe zone rect where we can drawing
    private val fieldRect = RectF(0f, 0f, 0f, 0f)
    // size of one cell
    private var cellSize: Float = 0f
    // padding in the cell
    private var cellPadding: Float = 0f
    private var row : Int = 40
    private var column : Int = 50
    // helper variable to avoid object allocation in onDraw
    private val cellRect = RectF()

    companion object {
        const val FREE_SEAT = Color.GREEN
        const val ORDERED_SEAT = Color.MAGENTA
        const val NOT_AVAILABLE_SEAT = Color.GRAY
        const val ISSUED_SEAT = Color.CYAN
    }

    init {
        initPaints()
        if (isInEditMode){
            seatList = listOf<Seat>(
                Seat(0,1,1,1,"asdasd",100f,SeatStatus.FREE_SEAT),
                Seat(0,2,1,1,"asdasd",100f,SeatStatus.FREE_SEAT),
                Seat(0,3,1,1,"asdasd",100f,SeatStatus.FREE_SEAT),
                Seat(0,4,1,1,"asdasd",100f,SeatStatus.ISSUED_SEAT),
                Seat(0,5,1,1,"asdasd",100f,SeatStatus.ISSUED_SEAT),
                Seat(0,6,1,1,"asdasd",100f,SeatStatus.ISSUED_SEAT),
                Seat(0,7,1,1,"asdasd",100f,SeatStatus.ISSUED_SEAT),
                Seat(0,8,1,1,"asdasd",100f,SeatStatus.NOT_AVAILABLE_SEAT),
                Seat(0,9,1,1,"asdasd",100f,SeatStatus.NOT_AVAILABLE_SEAT),
            )
        }
    }

    private fun initPaints(){
        textPaint = Paint().apply {
            Paint.ANTI_ALIAS_FLAG
            color = Color.BLACK
            style = Paint.Style.STROKE

            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,8f,resources.displayMetrics)
        }
        textPaint = TextPaint().apply {
            Paint.ANTI_ALIAS_FLAG
            color = Color.BLACK
            style = Paint.Style.STROKE

            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,8f,resources.displayMetrics)
        }
        borderPaint = Paint().apply {
            Paint.ANTI_ALIAS_FLAG
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,10f,resources.displayMetrics)
        }
        freeSeatPaint = Paint().apply {
            Paint.ANTI_ALIAS_FLAG
            color = FREE_SEAT
            style = Paint.Style.FILL
        }
        orderedSeatPaint = Paint().apply {
            Paint.ANTI_ALIAS_FLAG
            color = ORDERED_SEAT
            style = Paint.Style.FILL
            strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,1f,resources.displayMetrics)
        }
        notAvailableSeatPaint = Paint().apply {
            Paint.ANTI_ALIAS_FLAG
            color = NOT_AVAILABLE_SEAT
            style = Paint.Style.FILL
        }
        issuedSeatPaint = Paint().apply {
            Paint.ANTI_ALIAS_FLAG
            color = ISSUED_SEAT
            style = Paint.Style.FILL
        }
    }

    fun setSeats(seats : List<Seat>){
        this.seatList = seats
    }
//
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        val desiredWidth = 100 // Предполагаемая ширина View
//        val desiredHeight = 100 // Предполагаемая высота View
//
//        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
//        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
//        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
//        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
//
//        val width = when (widthMode) {
//            MeasureSpec.EXACTLY -> widthSize // Задан конкретный размер для ширины
//            MeasureSpec.AT_MOST -> min(desiredWidth, widthSize) // Размер не должен превышать заданный размер
//            else -> desiredWidth // Задать предпочтительный размер, если точного или максимального размера не задано
//        }
//
//        val height = when (heightMode) {
//            MeasureSpec.EXACTLY -> heightSize // Задан конкретный размер для высоты
//            MeasureSpec.AT_MOST -> min(desiredHeight, heightSize) // Размер не должен превышать заданный размер
//            else -> desiredHeight // Задать предпочтительный размер, если точного или максимального размера не задано
//        }
//
//        setMeasuredDimension(width, height)

        val minWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val minHeight = suggestedMinimumHeight + paddingTop + paddingBottom

        // calculating desired size of view
        val desiredCellSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f,
            resources.displayMetrics).toInt()

        val desiredWith = max(minWidth, column * desiredCellSizeInPixels + paddingLeft + paddingRight)
        val desiredHeight = max(minHeight, row * desiredCellSizeInPixels + paddingTop + paddingBottom)

        // submit view size
        setMeasuredDimension(
            resolveSize(desiredWith, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )

    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateViewSizes()
    }
    private fun updateViewSizes() {

        val maxX = seatList.maxBy { it.x }.x
        val minY = seatList.minBy { it.y }.y
        val safeWidth = width - paddingLeft - paddingRight
        val safeHeight = height - paddingTop - paddingBottom

        val cellWidth = safeWidth / maxX.toFloat()
        val cellHeight = safeHeight / minY.toFloat()

        cellSize = min(cellWidth, cellHeight)
        cellPadding = cellSize * 0.2f

        val fieldWidth = cellSize * 5
        val fieldHeight = cellSize * 5

        fieldRect.left = paddingLeft + (safeWidth - fieldWidth) / 2
        fieldRect.top = paddingTop + (safeHeight - fieldHeight) / 2
        fieldRect.right = fieldRect.left + fieldWidth
        fieldRect.bottom = fieldRect.top + fieldHeight
    }
    override fun onDraw(canvas: Canvas) {

        for (i in seatList){
            val cellRect = getCellRect(i.x,i.y)
            when(i.status){
                SeatStatus.FREE_SEAT -> canvas.apply {
//                    drawRect(cellRect.left,cellRect.top,cellRect.right,cellRect.bottom,freeSeatPaint)
//                    drawPoint(cellRect.left,cellRect.top,borderPaint)
                }
                SeatStatus.ORDERED_SEAT -> canvas.apply{
//                    drawLine(cellRect.left,cellRect.top,cellRect.right,cellRect.bottom,orderedSeatPaint)
                    drawPoint(cellRect.left,cellRect.top,borderPaint)
                }
                SeatStatus.NOT_AVAILABLE_SEAT -> canvas.apply{
//                    drawLine(cellRect.left,cellRect.top,cellRect.right,cellRect.bottom,freeSeatPaint)
                    drawPoint(cellRect.left,cellRect.top,borderPaint)
                }
                SeatStatus.ISSUED_SEAT -> canvas.apply{
//                    drawLine(cellRect.left,cellRect.top,cellRect.right,cellRect.bottom,notAvailableSeatPaint)
                    drawPoint(cellRect.left,cellRect.top,borderPaint)
                }
            }
        }
//        for (i in seatList){
//            when(i.status){
//                SeatStatus.FREE_SEAT -> canvas.apply {
//                    drawRect(5f,5f,5f,5f,freeSeatPaint)
//                    drawText("1",3f,3f,textPaint)
//                }
//                SeatStatus.ORDERED_SEAT -> canvas.drawRect(5f,5f,5f,5f,orderedSeatPaint)
//                SeatStatus.NOT_AVAILABLE_SEAT -> canvas.drawRect(5f,5f,5f,5f,notAvailableSeatPaint)
//                SeatStatus.ISSUED_SEAT -> canvas.drawRect(5f,5f,5f,5f,issuedSeatPaint)
//            }
//        }

//        canvas.drawRect(10f,10f,(width / seatList.size).toFloat() ,(height / seatList.size).toFloat(),orderedSeatPaint)
//        canvas.drawRect(10f,10f,(width / seatList.size).toFloat() ,(height / seatList.size).toFloat(),borderPaint)
//        canvas.drawRect(20f,20f,(width / seatList.size).toFloat() ,(height / seatList.size).toFloat(),orderedSeatPaint)
    }

    private fun getCellRect(x: Int, y: Int): RectF {
        cellRect.left = fieldRect.left + x * cellSize + cellPadding
        cellRect.top = fieldRect.top + y * cellSize + cellPadding
        cellRect.right = cellRect.left + cellSize - cellPadding * 2
        cellRect.left = cellRect.top + cellSize - cellPadding * 2
        return cellRect
    }

//
//    fun show() {
//        val layoutSeat = LinearLayout(context)
//        val params = LinearLayout.LayoutParams(
//            ViewGroup.LayoutParams.WRAP_CONTENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT
//        )
//        layoutSeat.orientation = LinearLayout.VERTICAL
//        layoutSeat.layoutParams = params
//        layoutSeat.setPadding(
//            layout_padding * seatGaping,
//            layout_padding * seatGaping,
//            layout_padding * seatGaping,
//            layout_padding * seatGaping
//        )
//        viewGroupLayout.addView(layoutSeat)
//
//        var layout: LinearLayout? = null
//        for (index in seats.indices) {
//            if (seats[index] == '/') {
//                layout = LinearLayout(context)
//                val paramsV = LinearLayout.LayoutParams(
//                    ViewGroup.LayoutParams.WRAP_CONTENT,
//                    ViewGroup.LayoutParams.WRAP_CONTENT
//                )
//                layout.layoutParams = paramsV
//                layout.orientation = LinearLayout.HORIZONTAL
//                layout.gravity = Gravity.CENTER
//
//                layoutSeat.addView(layout)
//            } else if (seats[index] == 'U') {
//                count++
//                val view = TextView(context)
//                setSeatAttrs(index, view, layout)
//                markAsBooked(view)
//
//            } else if (seats[index] == 'A') {
//                count++
//                val view = TextView(context)
//                setSeatAttrs(index, view, layout)
//                markAsAvailable(view)
//            } else if (seats[index] == 'R') {
//                count++
//                val view = TextView(context)
//                setSeatAttrs(index, view, layout)
//                markAsReserved(view)
//
//
//            } else if (seats[index] == 'S') {
//                count++
//                val view = TextView(context)
//                setSeatAttrs(index, view, layout)
//                markAsTransparentSeat(view)
//            } else if (seats[index] == '_') {
//                val view = TextView(context)
//                val layoutParams = LinearLayout.LayoutParams(seatSize, seatSize)
//                layoutParams.setMargins(seatGaping, seatGaping, seatGaping, seatGaping)
//                view.layoutParams = layoutParams
//                view.setBackgroundColor(Color.TRANSPARENT)
//                view.text = ""
//                layout!!.addView(view)
//            }
//        }
//
//    }

}