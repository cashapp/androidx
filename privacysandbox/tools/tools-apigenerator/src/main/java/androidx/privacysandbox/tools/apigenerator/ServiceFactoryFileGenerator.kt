/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.apigenerator

import androidx.privacysandbox.tools.core.AnnotatedInterface
import androidx.privacysandbox.tools.core.poet.build
import com.squareup.kotlinpoet.FileSpec

internal class ServiceFactoryFileGenerator(private val service: AnnotatedInterface) {
    private val proxyGenerator = ClientProxyTypeGenerator(service)

    fun generate(): FileSpec =
        FileSpec.builder(service.packageName, "${service.name}Factory").build {
            addKotlinDefaultImports(includeJvm = false, includeJs = false)

            // TODO: Add factory and SDK loading methods.

            addType(proxyGenerator.generate())
        }
}