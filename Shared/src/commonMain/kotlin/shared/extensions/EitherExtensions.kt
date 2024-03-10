package shared.extensions

import arrow.core.Either
import arrow.core.getOrElse

fun <T> Either<Throwable, T>.getOrThrow() = getOrElse { throw it }