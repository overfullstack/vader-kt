package io.overfullstack.types

import arrow.core.Either

typealias Validator<ValidatableT, FailureT> = suspend (ValidatableT) -> Either<FailureT, Any?>

