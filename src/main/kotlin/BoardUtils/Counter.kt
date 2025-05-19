package BoardUtils

class Counter(private var c: Int = 0) {
    fun inc() = c++
    fun get(): Int = c
    override fun toString(): String = c.toString()
}