package com.wire.android.shared.user.name

import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase

class ValidateNameUseCase : UseCase<Unit, ValidateNameParams> {

    override suspend fun run(params: ValidateNameParams): Either<Failure, Unit> =
        if (isNameTooShort(params.name)) Either.Left(NameTooShort)
        else Either.Right(Unit)

    private fun isNameTooShort(name: String) = name.trim().length <= NAME_MIN_LENGTH

    companion object {
        private const val NAME_MIN_LENGTH = 1
    }
}

data class ValidateNameParams(val name: String)

sealed class ValidateNameFailure : FeatureFailure()
object NameTooShort : ValidateNameFailure()
