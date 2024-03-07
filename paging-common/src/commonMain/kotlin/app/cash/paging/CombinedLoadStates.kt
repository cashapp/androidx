/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.cash.paging

import kotlinx.coroutines.flow.Flow
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSuppressWildcards

expect class CombinedLoadStates(
  refresh: LoadState,
  prepend: LoadState,
  append: LoadState,
  source: LoadStates,
  /* default = null */
  mediator: LoadStates?,
) {
  val refresh: LoadState
  val prepend: LoadState
  val append: LoadState
  val source: LoadStates
  val mediator: LoadStates?

  val isIdle: Boolean

  @get:JvmName("hasError")
  val hasError: Boolean
}

fun createCombinedLoadStates(
  refresh: LoadState,
  prepend: LoadState,
  append: LoadState,
  source: LoadStates,
  mediator: LoadStates? = null,
): CombinedLoadStates {
  return CombinedLoadStates(
    refresh,
    prepend,
    append,
    source,
    mediator,
  )
}

expect suspend fun Flow<CombinedLoadStates>.awaitNotLoading(): @JvmSuppressWildcards CombinedLoadStates?
