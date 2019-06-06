package tech.bigfig.roma.util

import java.lang.NullPointerException
import java.util.*

class Optional<T> {

    private val optionalValue: T?
    val value: T
        get() = optionalValue ?: throw NullPointerException()

    private constructor() {
        this.optionalValue = null
    }

    private constructor(optionalValue: T) {
        this.optionalValue = Objects.requireNonNull(optionalValue)
    }

    interface Action<T> {
        fun apply(optionalValue: T)
    }

    fun ifPresent(action: Action<T>) {
        if (optionalValue != null) {
            action.apply(optionalValue)
        }
    }

    fun isEmpty(): Boolean = optionalValue == null

    companion object {

        fun <T> empty(): Optional<T> {
            return Optional()
        }

        fun <T> of(optionalValue: T?): Optional<T> {
            return if (optionalValue == null)
                empty()
            else
                Optional(optionalValue)
        }
    }

}