package ru.petrikirche.orderticket

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates

class OrderTicketView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet?=null,
) :
    View(context, attributeSet, R.attr.orderTicketStyle,R.style.OrderTicketView) {
    private var seatList: List<Seat> = listOf()

    var selectedSeats = listOf<Seat>()

    var seatClickListener : SeatClickListener?=null

    private lateinit var textPaint: Paint
    private lateinit var borderPaint: Paint
    private lateinit var freeSeatPaint: Paint
    private lateinit var orderedSeatPaint: Paint
    private lateinit var notAvailableSeatPaint: Paint
    private lateinit var issuedSeatPaint: Paint


    private var freeSeatColor by Delegates.notNull<Int>()
    private var orderedSeatColor by Delegates.notNull<Int>()
    private var notAvailableSeatColor by Delegates.notNull<Int>()
    private var issuedSeatColor by Delegates.notNull<Int>()
    // safe zone rect where we can drawing
    private val fieldRect = RectF(0f, 0f, 0f, 0f)

    // size of one cell
    private var cellSize: Float = 0f

    // padding in the cell
    private var cellPadding: Float = 0f
    private var rows: Int = 44
    private var columns: Int = 44

    // helper variable to avoid object allocation in onDraw
    private val cellRect = RectF()
    private val rowTextRect = RectF()
    private val placeTextRect = RectF()

    init {
        if (attributeSet != null) {
            initAttributes(attributeSet)
        } else {
            initDefaultColors()
        }

        initPaints()
    }

    private fun initDefaultColors() {
        freeSeatColor = Color.GREEN
        orderedSeatColor = Color.MAGENTA
        notAvailableSeatColor = Color.GRAY
        issuedSeatColor = Color.CYAN
    }

    private fun initAttributes(attributeSet: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attributeSet,R.styleable.OrderTicketView)

        freeSeatColor = typedArray.getColor(R.styleable.OrderTicketView_free_seat_background, context.getColor(R.color.light_green))
        orderedSeatColor = typedArray.getColor(R.styleable.OrderTicketView_ordered_seat_background, context.getColor(R.color.pink))
        issuedSeatColor = typedArray.getColor(R.styleable.OrderTicketView_issued_seats_background, context.getColor(R.color.light_cyan))
        notAvailableSeatColor = typedArray.getColor(R.styleable.OrderTicketView_not_available_seat_background, context.getColor(R.color.gray))

        typedArray.recycle()
    }

    private fun initPaints() {
        textPaint = Paint().apply {
            Paint.ANTI_ALIAS_FLAG
            color = Color.BLACK
            style = Paint.Style.FILL

            textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                16.5f,
                resources.displayMetrics
            )
        }
        borderPaint = Paint().apply {
            Paint.ANTI_ALIAS_FLAG
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics)
        }
        freeSeatPaint = Paint().apply {
            Paint.ANTI_ALIAS_FLAG
            color = freeSeatColor
            style = Paint.Style.FILL
        }
        orderedSeatPaint = Paint().apply {
            Paint.ANTI_ALIAS_FLAG
            color = orderedSeatColor
            style = Paint.Style.FILL
            strokeWidth =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics)
        }
        notAvailableSeatPaint = Paint().apply {
            Paint.ANTI_ALIAS_FLAG
            color = notAvailableSeatColor
            style = Paint.Style.FILL
        }
        issuedSeatPaint = Paint().apply {
            Paint.ANTI_ALIAS_FLAG
            color = issuedSeatColor
            style = Paint.Style.FILL
        }
    }

    fun setSeats(seats: List<Seat>) {
        this.seatList = seats
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val minWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val minHeight = suggestedMinimumHeight + paddingTop + paddingBottom

        // calculating desired size of view
        val desiredCellSizeInPixels = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 90f,
            resources.displayMetrics
        ).toInt()

        val desiredWith =
            max(minWidth, columns * desiredCellSizeInPixels + paddingLeft + paddingRight)
        val desiredHeight =
            max(minHeight, rows * desiredCellSizeInPixels + paddingTop + paddingBottom)

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
        val safeWidth = width - paddingLeft - paddingRight
        val safeHeight = height - paddingTop - paddingBottom

        val cellWidth = safeWidth / columns.toFloat()
        val cellHeight = safeHeight / rows.toFloat()

        cellSize = min(cellWidth, cellHeight)
        cellPadding = cellSize * 0.09f

        val fieldWidth = cellSize * columns.toFloat()
        val fieldHeight = cellSize * rows.toFloat()

        fieldRect.left = paddingLeft + (safeWidth - fieldWidth) / 2
        fieldRect.top = paddingTop + (safeHeight - fieldHeight) / 2
        fieldRect.right = fieldRect.left + fieldWidth
        fieldRect.bottom = fieldRect.top + fieldHeight
    }

    override fun onDraw(canvas: Canvas) {
        drawSeats(canvas)
    }

    private fun drawSeats(canvas: Canvas) {
        for (seat in seatList) {
            val cellRect = getCellRect(seat.x, seat.y)
            val rowTextRect = getRowTextRect(cellRect)
            val placeTextRect = getPlaceTextRect(cellRect, seat.place.toString())
            when (seat.status) {
                SeatStatus.FREE_SEAT -> {
                    canvas.apply {
                        drawRect(
                            cellRect.left,
                            cellRect.top,
                            cellRect.right,
                            cellRect.bottom,
                            freeSeatPaint
                        )
                        drawText("${seat.row}", rowTextRect.left, rowTextRect.top, textPaint)
                        drawText(
                            "${seat.place}",
                            placeTextRect.right,
                            placeTextRect.bottom,
                            textPaint
                        )
                    }
                }

                SeatStatus.ORDERED_SEAT -> {
                    canvas.apply {
                        drawRect(
                            cellRect.left,
                            cellRect.top,
                            cellRect.right,
                            cellRect.bottom,
                            orderedSeatPaint
                        )
                        drawText("${seat.row}", rowTextRect.left, rowTextRect.top, textPaint)
                        drawText(
                            "${seat.place}",
                            placeTextRect.right,
                            placeTextRect.bottom,
                            textPaint
                        )
                    }
                }

                SeatStatus.NOT_AVAILABLE_SEAT -> {
                    canvas.apply {
                        drawRect(
                            cellRect.left,
                            cellRect.top,
                            cellRect.right,
                            cellRect.bottom,
                            notAvailableSeatPaint
                        )
                        drawText("${seat.row}", rowTextRect.left, rowTextRect.top, textPaint)
                        drawText(
                            "${seat.place}",
                            placeTextRect.right,
                            placeTextRect.bottom,
                            textPaint
                        )
                    }
                }

                SeatStatus.ISSUED_SEAT -> {
                    canvas.apply {
                        drawRect(
                            cellRect.left,
                            cellRect.top,
                            cellRect.right,
                            cellRect.bottom,
                            issuedSeatPaint
                        )
                        drawText("${seat.row}", rowTextRect.left, rowTextRect.top, textPaint)
                        drawText(
                            "${seat.place}",
                            placeTextRect.right,
                            placeTextRect.bottom,
                            textPaint
                        )
                    }
                }

                SeatStatus.EMPTY -> {}
            }
        }

    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.action){
            MotionEvent.ACTION_UP ->{
                val clickedSeat = getClickedSeat(event.x,event.y)
                clickedSeat?.let{
                    if (clickedSeat.status == SeatStatus.FREE_SEAT){
                        seatClickListener?.onSeatClicked(it)
                        invalidate()
                        return true
                    }
                }
            }
            MotionEvent.ACTION_DOWN ->{
                val clickedSeat = getClickedSeat(event.x,event.y)
                clickedSeat?.let{
                    if (clickedSeat.status == SeatStatus.FREE_SEAT){
                        seatClickListener?.onSeatClicked(it)
                        invalidate()
                        return true
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getClickedSeat(x: Float, y: Float): Seat? {
        for (seat in seatList){
            val cellRect = getCellRect(seat.x,seat.y)
            if (x>= cellRect.left && x<= cellRect.right && y>= cellRect.top && y<= cellRect.bottom){
                return seat
            }
        }
        return null
    }

    private fun getRowTextRect(cellRect: RectF): RectF {
        rowTextRect.top = cellRect.top + cellPadding * 2.3f
        rowTextRect.left = cellRect.left + cellPadding * 1f
        return rowTextRect
    }

    private fun getPlaceTextRect(cellRect: RectF, place: String): RectF {
        placeTextRect.right = cellRect.right - cellPadding * 1f - place.length * cellPadding
        placeTextRect.bottom = cellRect.bottom - cellPadding
        return placeTextRect
    }

    private fun getCellRect(row: Int, column: Int): RectF {
        cellRect.left = fieldRect.left + column * cellSize + cellPadding
        cellRect.top = fieldRect.top + row * cellSize + cellPadding
        cellRect.right = cellRect.left + cellSize - cellPadding * 2
        cellRect.bottom = cellRect.top + cellSize - cellPadding * 2
        return cellRect
    }


}