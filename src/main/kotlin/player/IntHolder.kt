package player

class IntHolder {
    private var value = 0
    private var lastHeld = 0

    fun hold(value: Int) {
        lastHeld = this.value
        this.value = value
    }

    fun retrieve(): Int {
        return value
    }

    fun hasValueChanged():Boolean {
        return this.value != this.lastHeld
    }
}