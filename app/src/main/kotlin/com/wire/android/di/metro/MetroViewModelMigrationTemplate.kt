/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.di.metro

/**
 * Template for migrating Android ViewModels toward the Metro + native iOS shape proven in WireOne.
 *
 * The migration should move creation responsibility out of Android-specific APIs while keeping current Android runtime
 * creation on the current Android runtime until the Metro graph is wired into the app. Prefer this shape:
 *
 * ```
 * @Inject
 * class ExampleViewModelFactory(
 *     private val dependency: Dependency,
 *     private val lazyFeature: Provider<FeatureContract>,
 * ) {
 *     fun create(
 *         args: ExampleArgs,
 *         navigator: ExampleNavigator,
 *         flowStateHolder: ExampleFlowStateHolder,
 *         coroutineScope: CoroutineScope? = null,
 *     ): ExampleViewModel = ExampleViewModel(
 *         args = args,
 *         navigator = navigator,
 *         flowStateHolder = flowStateHolder,
 *         dependency = dependency,
 *         lazyFeature = lazyFeature,
 *         coroutineScope = coroutineScope,
 *     )
 * }
 * ```
 *
 * Rules for each migrated ViewModel:
 * - keep the ViewModel constructor platform-neutral: no `SavedStateHandle`, `NavController`, Compose destination args,
 *   Android `Context`, or direct platform-only creation contract;
 * - keep runtime/session args explicit in `create(...)`, especially values previously pulled from navigation state;
 * - keep long-lived dependencies injected into the factory by Metro;
 * - use `Provider<T>` for dependencies that can be cyclic or should stay lazy;
 * - pass a nullable/testable `CoroutineScope` only when the ViewModel already supports external scope injection;
 * - keep Android behavior unchanged while both Android and Metro creation coexist.
 *
 * For iOS, add a small bridge next to the feature instead of exporting Android lifecycle concepts:
 *
 * ```
 * fun createExampleIosViewModel(
 *     navigator: ExampleNavigator,
 *     flowStateHolder: ExampleFlowStateHolder,
 *     exampleViewModelFactory: ExampleViewModelFactory,
 * ): IosViewModel<ExampleState, ExampleEffect, ExampleIntent> {
 *     val vm = exampleViewModelFactory.create(
 *         navigator = navigator,
 *         flowStateHolder = flowStateHolder,
 *         args = ExampleArgs(...),
 *     )
 *
 *     return IosViewModel(
 *         state = vm.state,
 *         effects = vm.effects,
 *         onIntent = vm::sendIntent,
 *     )
 * }
 * ```
 *
 * For flows spanning multiple screens, mirror WireOne's login flow:
 * - create a feature-level navigator abstraction for screen-to-screen transitions;
 * - create a feature-level state holder for values shared across steps;
 * - create all step ViewModels from factories using the same navigator/state holder;
 * - expose the iOS bridge from the Metro graph as graph properties, not through Android screens.
 *
 * Do not enable Metro Dagger interop for this pass. Existing Android runtime creation stays until the bridge is ready.
 */
internal object MetroViewModelMigrationTemplate
