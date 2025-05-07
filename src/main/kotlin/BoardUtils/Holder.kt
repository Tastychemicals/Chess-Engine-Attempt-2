package BoardUtils

/**
 * Holds a [T] value
 */

class Holder<T> {
    constructor()
    private var value: T? = null
    private var lastValue: T? = null


    private constructor(value: T?, lastValue: T?) {
        this.value = value
        this.lastValue = lastValue
    }

    /**
     * Holds a [value].
     */
    fun hold(value: T?) {
        lastValue = this.value
        this.value = value
    }

    /**
     * @return [value].
     */
    fun show(): T? {
        return value
    }

    /**
     * Removes [value].
     */
    fun drop(): T? {
        val dropped = value
        value = null
        return dropped
    }

    /**
     * Interchanges the Holder's [value] and [lastValue].
     * @return The [value] after the swap.
     */
    fun swap(): T? {
        val newValue = lastValue
        lastValue = value
        value = newValue
        return newValue
    }

    /**
     * Clears the Holder's [value] and [lastValue].
     */
    fun forget() {
        value = null
        lastValue = null
    }

    /**
     * @return Returns true if the Holder currently has no value.
     */
    fun isEmpty(): Boolean {
        return value == null
    }

    /**
     * Checks if the Holder is holding a different [T].
     */
    fun hasValueChanged(): Boolean {
        return this.value != this.lastValue
    }

    override fun toString(): String {
        return "Holder(value=$value, lastValue=$lastValue)"
    }

    override fun equals(other: Any?): Boolean {
        if (other is Holder<T>) {
            return this.value == other.value
        }
        return false
    }

    override fun hashCode(): Int {
        var result = value?.hashCode() ?: 0
        result = 31 * result + (lastValue?.hashCode() ?: 0)
        return result
    }

    /**
     * returns a clone of this Holder.
     */
    fun clone(): Holder<T> = Holder<T>(value, lastValue)
}