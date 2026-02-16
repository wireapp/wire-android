/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
includeBuild("kalium") {
    // This dependency substitution should not be done on release mode once the Kalium library has been published to Maven repo
    dependencySubstitution {
        substitute(module("com.wire.kalium:kalium-logic")).using(project(":logic"))
        substitute(module("com.wire.kalium:kalium-util")).using(project(":core:util"))
        substitute(module("com.wire.kalium:kalium-data")).using(project(":core:data"))
        substitute(module("com.wire.kalium:kalium-common")).using(project(":core:common"))
        substitute(module("com.wire.kalium:kalium-cells")).using(project(":domain:cells"))
        substitute(module("com.wire.kalium:kalium-nomaddevice")).using(project(":domain:nomaddevice"))
        // test modules
        substitute(module("com.wire.kalium:kalium-mocks")).using(project(":test:mocks"))
        substitute(module("com.wire.kalium:kalium-network")).using(project(":data:network"))
    }
}
