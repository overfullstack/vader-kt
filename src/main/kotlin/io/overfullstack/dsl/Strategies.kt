package io.overfullstack.dsl

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.identity
import arrow.core.left
import arrow.core.right
import arrow.fx.coroutines.parTraverse
import io.overfullstack.types.Validator

private typealias FailFastStrategy<ValidatableT, FailureT> = suspend (ValidatableT?) -> Either<FailureT, ValidatableT?>
private typealias AccumulationStrategy<ValidatableT, FailureT> = suspend (ValidatableT?) -> List<Either<FailureT, ValidatableT?>>

/* ---------------------------FAIL FAST--------------------------- */

fun <FailureT, ValidatableT> failFastStrategy(
    validations: List<Validator<ValidatableT, FailureT>>,
    throwableMapper: (Throwable) -> FailureT,
    invalidValidatable: FailureT,
): FailFastStrategy<ValidatableT, FailureT> = { validatable ->
    when (validatable) {
        null -> invalidValidatable.left()
        else -> {
            // Validations are run sequential for fail-fast
            validations.fold(validatable.right() as Either<FailureT, Any?>) { prevValidationResult, currentValidation ->
                prevValidationResult.flatMap {
                    fireValidation(currentValidation, validatable, throwableMapper)
                }
            }.map { validatable } // To put back the original validatable in place of `Any?` in right state.
        }
    }
}

fun <FailureT, ValidatableT> accumulationStrategy(
    validations: List<Validator<ValidatableT, FailureT>>,
    throwableMapper: (Throwable) -> FailureT,
    invalidValidatable: FailureT,
): AccumulationStrategy<ValidatableT, FailureT> = { validatable ->
    when (validatable) {
        null -> listOf(invalidValidatable.left())
        else -> {
            validations.map { fireValidation(it, validatable, throwableMapper) }
                .map { it.map { validatable } }
        }
    }
}

private suspend fun <FailureT, ValidatableT> fireValidation(
    validation: Validator<ValidatableT, FailureT>,
    validatable: ValidatableT,
    throwableMapper: (Throwable) -> FailureT,
): Either<FailureT, Any?> = Either.catch {
    validation(validatable)
}.fold({ throwableMapper(it).left() }, ::identity)

