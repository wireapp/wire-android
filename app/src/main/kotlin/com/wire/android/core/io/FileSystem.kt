@file:Suppress("TooGenericExceptionCaught")
package com.wire.android.core.io

import android.content.Context
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FileDoesNotExist
import com.wire.android.core.exception.GeneralIOFailure
import com.wire.android.core.exception.IOAccessDenied
import com.wire.android.core.functional.Either
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

class FileSystem(private val appContext: Context) {

    private fun internalStorageDir() = appContext.filesDir

    fun createInternalFile(pathName: String): Either<Failure, File> = try {
        val file = File(internalStorageDir(), pathName)
        file.mkdirs()
        Either.Right(file)
    } catch (ex: SecurityException) {
        Either.Left(IOAccessDenied)
    } catch (ex: Exception) {
        Either.Left(GeneralIOFailure(ex))
    }

    fun internalFile(pathName: String): Either<Failure, File> =
        File(internalStorageDir(), pathName).let {
            if (!it.exists()) Either.Left(FileDoesNotExist)
            else Either.Right(it)
        }

    fun writeToFile(file: File, inputStream: InputStream): Either<Failure, File> = try {
        file.writeBytes(inputStream.readBytes())
        inputStream.close()
        Either.Right(file)
    } catch (ex: FileNotFoundException) {
        Either.Left(FileDoesNotExist)
    } catch (ex: Exception) {
        Either.Left(GeneralIOFailure(ex))
    }
}
