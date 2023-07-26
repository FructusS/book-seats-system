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
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates
typealias OnCellActionListener = (seatField : SeatField, seat: Seat) -> Unit
class OrderTicketView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet?=null,
) :
    View(context, attributeSet, R.attr.orderTicketStyle,R.style.OrderTicketView) {
    var field: SeatField? = null
        set(value) {
            // removing listener from old field
            field?.listeners?.remove(listener)
            field = value
            // assigning listener to a new field
            value?.listeners?.add(listener)
            // new field may have another number of rows/columns, another cells, so need to update:
            updateViewSizes() // safe zone rect, cell size, cell padding
            requestLayout() // in case of using wrap_content, view size may also be changed
            invalidate() // redraw view
        }

    private val listener: OnFieldChangedListener = {
        invalidate()
    }

    var selectedSeats = listOf<Seat>()
    var actionListener: OnCellActionListener? = null
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

    private var currentRow: Int = -1
    private var currentColumn: Int = -1
    init {
        if (attributeSet != null) {
            initAttributes(attributeSet)
        } else {
            initDefaultColors()
        }

        initPaints()
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // start listening field data changes
        field?.listeners?.add(listener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // stop listening field data changes
        field?.listeners?.remove(listener)
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
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics)
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

//    fun setSeats(seats: List<Seat>) {
//        this.seatList = seats
//    }

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
        for (seat in field?.seats!!) {
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

                SeatStatus.SELECTED -> {
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
                        drawRect(
                            cellRect.left,
                            cellRect.top ,
                            cellRect.right,
                            cellRect.bottom,
                            borderPaint
                        )
                    }
                }
                else -> {

                }
            }
        }

    }



    private fun getClickedSeat(x: Int, y: Int): Seat? {
        for (seat in field?.seats!!){
            if (seat.x == x && seat.y == y){
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.action) {
            MotionEvent.ACTION_DOWN -> {
                updateCurrentCell(event)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                updateCurrentCell(event)
                return true
            }
            MotionEvent.ACTION_UP -> {
                return performClick()
            }
        }
        return false
    }
    override fun performClick(): Boolean {
        super.performClick()
        val field = this.field ?: return false
        val row = currentRow
        val column = currentColumn
        val seat = getClickedSeat(currentRow,currentColumn)
        if (row >= 0 && column >= 0 && row < field.rows && column < field.columns && seat != null) {
            actionListener?.invoke(field, seat)
            return true
        }
        return false
    }


    private fun updateCurrentCell(event: MotionEvent) {
        val field = this.field ?: return
        val row = getRow(event)
        val column = getColumn(event)
        if (row >= 0 && column >= 0 && row < field.rows && column < field.columns) {
            if (currentRow != row || currentColumn != column) {
                currentRow = row
                currentColumn = column
                invalidate()
            }
        } else {
            // clearing current cell if user moves out from the view
            currentRow = -1
            currentColumn = -1
            invalidate()
        }
    }

    private fun getRow(event: MotionEvent): Int {
        // floor is better then simple rounding to int in our case
        // because it rounds to an integer towards negative infinity
        // examples:
        // 1) -0.3.toInt() = 0
        // 2) floor(-0.3) = -1
        return floor((event.y - fieldRect.top) / cellSize).toInt()
    }

    private fun getColumn(event: MotionEvent): Int {
        return floor((event.x - fieldRect.left) / cellSize).toInt()
    }

}