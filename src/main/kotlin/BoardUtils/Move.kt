package BoardUtils

private const val START_SQUARE_BIT = 0
private const val END_SQUARE_BIT = 6
private const val FLAGS_BIT = 12


private const val START_SQUARE_SELECTOR =   0b000000000000000111111
private const val END_SQUARE_SELECTOR =     0b000000000111111000000
private const val FLAGS_SELECTOR =          0b111111111000000000000

// 000000 000000 000000

private const val CAPTURE_BIT = 0
private const val CASTLE_BIT = 1
private const val PROMOTION_BIT = 2
private const val ENPASSANT_BIT = 3
private const val CHECK_BIT = 4
private const val PROMOTION_TYPE_BIT = 5

private const val CAPTURE_FLAG =    1 shl CAPTURE_BIT               // 0b00000001
private const val CASTLE_FLAG =     1 shl CASTLE_BIT                // 0b00000010
private const val PROMOTION_FLAG =  1 shl PROMOTION_BIT             // 0b00000100
private const val ENPASSANT_FLAG =  1 shl ENPASSANT_BIT             // 0b00001000
private const val CHECK_FLAG =      1 shl CHECK_BIT                 // 0b00010000
private const val PROMOTION_TYPE_FLAG = 0b111 shl PROMOTION_TYPE_BIT// 0b11100000


    object Move {
        fun encode(startSquare: Int, endSquare: Int, flags: Int = 0): move {
            return  (startSquare shl START_SQUARE_BIT) or (endSquare shl END_SQUARE_BIT) or  (flags shl FLAGS_BIT)
        }
        fun encodeFlags(
            capture: Boolean = false,
            castle: Boolean = false,
            promotion: Boolean = false,
            enpassant: Boolean = false,
            check: Boolean  = false,
            promotionType: Int = EMPTY
        ): Int {
            return ((if (capture) 1 else 0) shl CAPTURE_BIT) or
                    ((if (castle) 1 else 0) shl CASTLE_BIT) or
                    ((if (promotion) 1 else 0) shl PROMOTION_BIT) or
                    ((if (enpassant) 1 else 0) shl ENPASSANT_BIT) or
                    ((if (check) 1 else 0) shl CHECK_BIT) or
                    ((if (promotionType != EMPTY) promotionType else EMPTY) shl PROMOTION_TYPE_BIT)
        }

        fun addFlags(move: move, flags: Int = 0): move = (move) or  (flags shl FLAGS_BIT)
        fun addPromotionType(flags: Int, type: Int): Int = flags or (type shl PROMOTION_TYPE_BIT)
        fun getPromotionType(flags: Int): Int = (flags and START_SQUARE_SELECTOR) shr START_SQUARE_BIT
    }




    fun move.start(): Int = (this and START_SQUARE_SELECTOR) shr START_SQUARE_BIT
    fun move.end(): Int = (this and END_SQUARE_SELECTOR) shr END_SQUARE_BIT
    fun move.flags(): Int = (this and FLAGS_SELECTOR) shr FLAGS_BIT

    fun move.getPromotion(): Int = (this.flags() and PROMOTION_TYPE_FLAG) shr PROMOTION_TYPE_BIT
    fun move.getString(): String = getFlagNames().joinToString() + " From ${this.start()} to ${this.end()}"

    fun move.isCapture (): Boolean = this.flags() and CAPTURE_FLAG != 0
    fun move.isCastle (): Boolean = this.flags() and CASTLE_FLAG != 0
    fun move.isPromotion (): Boolean = this.flags() and PROMOTION_FLAG != 0
    fun move.isEnPassant(): Boolean  = this.flags() and ENPASSANT_FLAG != 0
    fun move.isCheck(): Boolean  = this.flags() and CHECK_FLAG != 0
    fun move.isQuiet(): Boolean = this.flags() and (CAPTURE_FLAG or PROMOTION_FLAG or CASTLE_FLAG or ENPASSANT_FLAG or CHECK_FLAG) == 0



    fun move.getFlagNames(): List<String> {
        val names = mutableListOf<String>()
        if (isCapture()) names.add("Capture")
        if (isEnPassant()) names.add("En Passant")
        if (isPromotion()) names.add("Promotion to ${typeNames[getPromotion()]}")
        if (isCastle()) names.add("Castle")
        if (isCheck()) names.add("Check")
        if (names.isEmpty()) names.add("Quiet Move")

        return names
    }




