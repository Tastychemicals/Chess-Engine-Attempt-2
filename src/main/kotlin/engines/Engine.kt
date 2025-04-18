package engines

import BoardUtils.Holder

interface Engine {
    fun makePlay(timeLimit: Long, listener: Holder<Int>)

    fun getName(): String
}