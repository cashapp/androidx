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

package androidx.compose.plugins.kotlin

abstract class AbstractControlFlowTransformTests : ComposeIrTransformTest() {
    protected fun controlFlow(
        source: String,
        expectedTransformed: String,
        dumpTree: Boolean = false
    ) = verifyComposeIrTransform(
        """
            import androidx.compose.Composable
            import androidx.compose.key
            import androidx.compose.ComposableContract

            $source
        """.trimIndent(),
        expectedTransformed,
        """
            import androidx.compose.Composable

            inline class InlineClass(val value: Int)

            @Composable fun A() {}
            @Composable fun A(x: Int) { }
            @Composable fun B(): Boolean { return true }
            @Composable fun B(x: Int): Boolean { return true }
            @Composable fun R(): Int { return 10 }
            @Composable fun R(x: Int): Int { return 10 }
            @Composable fun P(x: Int) { }
            @Composable fun Int.A() { }
            @Composable fun L(): List<Int> { return listOf(1, 2, 3) }
            @Composable fun W(content: @Composable () -> Unit) { content() }
            @Composable inline fun IW(content: @Composable () -> Unit) { content() }
            fun NA() { }
            fun NB(): Boolean { return true }
            fun NR(): Int { return 10 }
            var a = 1
            var b = 2
            var c = 3
        """.trimIndent(),
        dumpTree
    )
}