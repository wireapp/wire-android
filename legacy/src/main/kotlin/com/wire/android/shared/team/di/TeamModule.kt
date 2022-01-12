package com.wire.android.shared.team.di

import com.wire.android.shared.team.usecase.GetUserTeamUseCase
import org.koin.dsl.module

val teamModule = module {
    factory { GetUserTeamUseCase() }
}
