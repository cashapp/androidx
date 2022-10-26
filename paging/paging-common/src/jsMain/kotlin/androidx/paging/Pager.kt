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

package androidx.paging

actual internal inline fun <Key : Any, Value : Any> Pager<Key, Value>.suspendingPagingSourceFactoryAdapter(
    noinline pagingSourceFactory: () -> PagingSource<Key, Value>
): suspend () -> PagingSource<Key, Value> {
    // cannot pass it as is since it is not a suspend function. Hence, we wrap it in {}
    // which means we are calling the original factory inside a suspend function
    return {
        pagingSourceFactory()
    }
}
