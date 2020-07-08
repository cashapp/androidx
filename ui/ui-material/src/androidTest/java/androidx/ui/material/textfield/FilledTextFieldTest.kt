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

package androidx.ui.material.textfield

import android.os.Build
import androidx.compose.Providers
import androidx.compose.state
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.core.TextInputServiceAmbient
import androidx.ui.core.onPositioned
import androidx.ui.core.positionInRoot
import androidx.ui.core.testTag
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.foundation.contentColor
import androidx.ui.foundation.currentTextStyle
import androidx.ui.foundation.drawBackground
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.graphics.RectangleShape
import androidx.ui.graphics.compositeOver
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.input.PasswordVisualTransformation
import androidx.ui.input.TextFieldValue
import androidx.ui.input.TextInputService
import androidx.ui.layout.Column
import androidx.ui.layout.Stack
import androidx.ui.layout.height
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredSize
import androidx.ui.material.FilledTextField
import androidx.ui.material.MaterialTheme
import androidx.ui.material.runOnIdleComposeWithDensity
import androidx.ui.material.setMaterialContent
import androidx.ui.material.setMaterialContentForSizeAssertions
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.doGesture
import androidx.ui.test.doSendImeAction
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.sendClick
import androidx.ui.test.waitForIdle
import androidx.ui.text.FirstBaseline
import androidx.ui.text.SoftwareKeyboardController
import androidx.ui.unit.IntSize
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@MediumTest
@RunWith(JUnit4::class)
class FilledTextFieldTest {

    private val ExpectedMinimumTextFieldHeight = 56.dp
    private val ExpectedPadding = 16.dp
    private val IconPadding = 12.dp
    private val ExpectedBaselineOffset = 20.dp
    private val TopPaddingFilledTextfield = 3.dp
    private val IconColorAlpha = 0.54f
    private val TextfieldTag = "textField"

    @get:Rule
    val testRule = createComposeRule()

    @Test
    fun testFilledTextField_minimumHeight() {
        testRule.setMaterialContentForSizeAssertions {
            FilledTextField(
                value = "input",
                onValueChange = {},
                label = {},
                modifier = Modifier.preferredHeight(20.dp)
            )
        }
            .assertHeightIsEqualTo(ExpectedMinimumTextFieldHeight)
    }

    @Test
    fun testTextFields_singleFocus() {
        var textField1Focused = false
        val textField1Tag = "TextField1"

        var textField2Focused = false
        val textField2Tag = "TextField2"

        testRule.setMaterialContent {
            Column {
                FilledTextField(
                    modifier = Modifier.testTag(textField1Tag),
                    value = "input1",
                    onValueChange = {},
                    label = {},
                    onFocusChange = { textField1Focused = it }
                )
                FilledTextField(
                    modifier = Modifier.testTag(textField2Tag),
                    value = "input2",
                    onValueChange = {},
                    label = {},
                    onFocusChange = { textField2Focused = it }
                )
            }
        }

        findByTag(textField1Tag).doClick()

        runOnIdleCompose {
            assertThat(textField1Focused).isTrue()
            assertThat(textField2Focused).isFalse()
        }

        findByTag(textField2Tag).doClick()

        runOnIdleCompose {
            assertThat(textField1Focused).isFalse()
            assertThat(textField2Focused).isTrue()
        }
    }

    @Test
    fun testFilledTextField_getFocus_whenClickedOnSurfaceArea() {
        var focused = false
        testRule.setMaterialContent {
            Box {
                FilledTextField(
                    modifier = Modifier.testTag(TextfieldTag),
                    value = "input",
                    onValueChange = {},
                    label = {},
                    onFocusChange = { focused = it }
                )
            }
        }

        // Click on (2, 2) which is Surface area and outside input area
        findByTag(TextfieldTag).doGesture {
            sendClick(Offset(2f, 2f))
        }

        testRule.runOnIdleComposeWithDensity {
            assertThat(focused).isTrue()
        }
    }

    @Test
    fun testFilledTextField_labelPosition_initial_withDefaultHeight() {
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        testRule.setMaterialContent {
            Box {
                FilledTextField(
                    value = "",
                    onValueChange = {},
                    label = {
                        Text(
                            text = "label",
                            fontSize = 10.sp,
                            modifier = Modifier
                                .onPositioned {
                                    labelPosition.value = it.positionInRoot
                                    labelSize.value = it.size
                                }
                        )
                    },
                    modifier = Modifier.preferredHeight(56.dp)
                )
            }
        }

        testRule.runOnIdleComposeWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width).isGreaterThan(0)
            // centered position
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
            assertThat(labelPosition.value?.y).isEqualTo(
                ((ExpectedMinimumTextFieldHeight.toIntPx() - labelSize.value!!.height) / 2f)
                    .roundToInt().toFloat()
            )
        }
    }

    @Test
    fun testFilledTextField_labelPosition_initial_withCustomHeight() {
        val height = 80.dp
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        testRule.setMaterialContent {
            Box {
                FilledTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.preferredHeight(height),
                    label = {
                        Text(text = "label", modifier = Modifier.onPositioned {
                            labelPosition.value = it.positionInRoot
                            labelSize.value = it.size
                        })
                    }
                )
            }
        }

        testRule.runOnIdleComposeWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width).isGreaterThan(0)
            // centered position
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
            assertThat(labelPosition.value?.y).isEqualTo(
                ((height.toIntPx() - labelSize.value!!.height) / 2f).roundToInt().toFloat()
            )
        }
    }

    @Test
    fun testFilledTextField_labelPosition_whenFocused() {
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        val baseline = Ref<Float>()
        testRule.setMaterialContent {
            Box {
                FilledTextField(
                    modifier = Modifier.testTag(TextfieldTag),
                    value = "",
                    onValueChange = {},
                    label = {
                        Text(text = "label", modifier = Modifier.onPositioned {
                            labelPosition.value = it.positionInRoot
                            labelSize.value = it.size
                            baseline.value = it[FirstBaseline].toFloat() + labelPosition.value!!.y
                        })
                    }
                )
            }
        }

        // click to focus
        clickAndAdvanceClock(TextfieldTag, 200)

        testRule.runOnIdleComposeWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width).isGreaterThan(0)
            // label's top position
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
            assertThat(baseline.value).isEqualTo(
                ExpectedBaselineOffset.toIntPx().toFloat()
            )
        }
    }

    @Test
    fun testFilledTextField_labelPosition_whenInput() {
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        val baseline = Ref<Float>()
        testRule.setMaterialContent {
            Box {
                FilledTextField(
                    value = "input",
                    onValueChange = {},
                    label = {
                        Text(text = "label", modifier = Modifier.onPositioned {
                            labelPosition.value = it.positionInRoot
                            labelSize.value = it.size
                            baseline.value =
                                it[FirstBaseline].toFloat() + labelPosition.value!!.y
                        })
                    }
                )
            }
        }

        testRule.runOnIdleComposeWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width).isGreaterThan(0)
            // label's top position
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
            assertThat(baseline.value).isEqualTo(
                ExpectedBaselineOffset.toIntPx().toFloat()
            )
        }
    }

    @Test
    fun testFilledTextField_placeholderPosition_withLabel() {
        val placeholderSize = Ref<IntSize>()
        val placeholderPosition = Ref<Offset>()
        testRule.setMaterialContent {
            Box {
                FilledTextField(
                    modifier = Modifier
                        .preferredHeight(60.dp)
                        .testTag(TextfieldTag),
                    value = "",
                    onValueChange = {},
                    label = { Text("label") },
                    placeholder = {
                        Text(text = "placeholder", modifier = Modifier.onPositioned {
                            placeholderPosition.value = it.positionInRoot
                            placeholderSize.value = it.size
                        })
                    }
                )
            }
        }
        // click to focus
        clickAndAdvanceClock(TextfieldTag, 200)

        testRule.runOnIdleComposeWithDensity {
            // size
            assertThat(placeholderSize.value).isNotNull()
            assertThat(placeholderSize.value?.height).isGreaterThan(0)
            assertThat(placeholderSize.value?.width).isGreaterThan(0)
            // placeholder's position
            assertThat(placeholderPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
            assertThat(placeholderPosition.value?.y)
                .isEqualTo(
                    (ExpectedBaselineOffset.toIntPx() + TopPaddingFilledTextfield.toIntPx())
                        .toFloat()
                )
        }
    }

    @Test
    fun testFilledTextField_placeholderPosition_whenNoLabel() {
        val placeholderSize = Ref<IntSize>()
        val placeholderPosition = Ref<Offset>()
        val height = 60.dp
        testRule.setMaterialContent {
            Box {
                FilledTextField(
                    modifier = Modifier.preferredHeight(height).testTag(TextfieldTag),
                    value = "",
                    onValueChange = {},
                    label = {},
                    placeholder = {
                        Text(text = "placeholder", modifier = Modifier.height(20.dp).onPositioned {
                            placeholderPosition.value = it.positionInRoot
                            placeholderSize.value = it.size
                        })
                    }
                )
            }
        }
        // click to focus
        clickAndAdvanceClock(TextfieldTag, 200)

        testRule.runOnIdleComposeWithDensity {
            // size
            assertThat(placeholderSize.value).isNotNull()
            assertThat(placeholderSize.value?.height).isEqualTo(20.dp.toIntPx())
            assertThat(placeholderSize.value?.width).isGreaterThan(0)
            // centered position
            assertThat(placeholderPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
            assertThat(placeholderPosition.value?.y).isEqualTo(
                ((height.toIntPx().toFloat() - placeholderSize.value!!.height) / 2f)
                    .roundToInt()
                    .toFloat()
            )
        }
    }

    @Test
    fun testFilledTextField_noPlaceholder_whenInputNotEmpty() {
        val placeholderSize = Ref<IntSize>()
        val placeholderPosition = Ref<Offset>()
        testRule.setMaterialContent {
            Column {
                FilledTextField(
                    modifier = Modifier.testTag(TextfieldTag),
                    value = "input",
                    onValueChange = {},
                    label = {},
                    placeholder = {
                        Text(text = "placeholder", modifier = Modifier.onPositioned {
                            placeholderPosition.value = it.positionInRoot
                            placeholderSize.value = it.size
                        })
                    }
                )
            }
        }

        // click to focus
        clickAndAdvanceClock(TextfieldTag, 200)

        testRule.runOnIdleComposeWithDensity {
            assertThat(placeholderSize.value).isNull()
            assertThat(placeholderPosition.value).isNull()
        }
    }

    @Test
    fun testFilledTextField_placeholderColorAndTextStyle() {
        testRule.setMaterialContent {
            FilledTextField(
                modifier = Modifier.testTag(TextfieldTag),
                value = "",
                onValueChange = {},
                label = {},
                placeholder = {
                    Text("placeholder")
                    assertThat(contentColor())
                        .isEqualTo(
                            MaterialTheme.colors.onSurface.copy(
                                0.6f
                            )
                        )
                    assertThat(currentTextStyle()).isEqualTo(MaterialTheme.typography.subtitle1)
                }
            )
        }

        // click to focus
        findByTag(TextfieldTag).doClick()
    }

    @Test
    fun testFilledTextField_trailingAndLeading_sizeAndPosition() {
        val textFieldHeight = 60.dp
        val textFieldWidth = 300.dp
        val size = 30.dp
        val leadingPosition = Ref<Offset>()
        val leadingSize = Ref<IntSize>()
        val trailingPosition = Ref<Offset>()
        val trailingSize = Ref<IntSize>()

        testRule.setMaterialContent {
            FilledTextField(
                value = "text",
                onValueChange = {},
                modifier = Modifier.preferredSize(textFieldWidth, textFieldHeight),
                label = {},
                leadingIcon = {
                    Box(Modifier.preferredSize(size).onPositioned {
                        leadingPosition.value = it.positionInRoot
                        leadingSize.value = it.size
                    })
                },
                trailingIcon = {
                    Box(Modifier.preferredSize(size).onPositioned {
                        trailingPosition.value = it.positionInRoot
                        trailingSize.value = it.size
                    })
                }
            )
        }

        testRule.runOnIdleComposeWithDensity {
            // leading
            assertThat(leadingSize.value).isEqualTo(IntSize(size.toIntPx(), size.toIntPx()))
            assertThat(leadingPosition.value?.x).isEqualTo(IconPadding.toIntPx().toFloat())
            assertThat(leadingPosition.value?.y).isEqualTo(
                ((textFieldHeight.toIntPx() - leadingSize.value!!.height) / 2f).roundToInt()
                    .toFloat()
            )
            // trailing
            assertThat(trailingSize.value).isEqualTo(IntSize(size.toIntPx(), size.toIntPx()))
            assertThat(trailingPosition.value?.x).isEqualTo(
                (textFieldWidth.toIntPx() - IconPadding.toIntPx() - trailingSize.value!!.width)
                    .toFloat()
            )
            assertThat(trailingPosition.value?.y)
                .isEqualTo(
                    ((textFieldHeight.toIntPx() - trailingSize.value!!.height) / 2f)
                        .roundToInt().toFloat()
                )
        }
    }

    @Test
    fun testFilledTextField_labelPositionX_initial_withTrailingAndLeading() {
        val height = 60.dp
        val iconSize = 30.dp
        val labelPosition = Ref<Offset>()
        testRule.setMaterialContent {
            Box {
                FilledTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.preferredHeight(height),
                    label = {
                        Text(text = "label", modifier = Modifier.onPositioned {
                            labelPosition.value = it.positionInRoot
                        })
                    },
                    trailingIcon = { Box(Modifier.preferredSize(iconSize)) },
                    leadingIcon = { Box(Modifier.preferredSize(iconSize)) }
                )
            }
        }

        testRule.runOnIdleComposeWithDensity {
            assertThat(labelPosition.value?.x).isEqualTo(
                (ExpectedPadding.toIntPx() + IconPadding.toIntPx() + iconSize.toIntPx())
                    .toFloat()
            )
        }
    }

    @Test
    fun testFilledTextField_labelPositionX_initial_withEmptyTrailingAndLeading() {
        val height = 60.dp
        val labelPosition = Ref<Offset>()
        testRule.setMaterialContent {
            Box {
                FilledTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.preferredHeight(height),
                    label = {
                        Text(text = "label", modifier = Modifier.onPositioned {
                            labelPosition.value = it.positionInRoot
                        })
                    },
                    trailingIcon = {},
                    leadingIcon = {}
                )
            }
        }

        testRule.runOnIdleComposeWithDensity {
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
        }
    }

    @Test
    fun testFilledTextField_colorInLeadingTrailing_whenValidInput() {
        testRule.setMaterialContent {
            FilledTextField(
                value = "",
                onValueChange = {},
                label = {},
                isErrorValue = false,
                leadingIcon = {
                    assertThat(contentColor())
                        .isEqualTo(
                            MaterialTheme.colors.onSurface.copy(
                                IconColorAlpha
                            )
                        )
                },
                trailingIcon = {
                    assertThat(contentColor())
                        .isEqualTo(
                            MaterialTheme.colors.onSurface.copy(
                                IconColorAlpha
                            )
                        )
                }
            )
        }
    }

    @Test
    fun testFilledTextField_colorInLeadingTrailing_whenInvalidInput() {
        testRule.setMaterialContent {
            FilledTextField(
                value = "",
                onValueChange = {},
                label = {},
                isErrorValue = true,
                leadingIcon = {
                    assertThat(contentColor())
                        .isEqualTo(
                            MaterialTheme.colors.onSurface.copy(
                                IconColorAlpha
                            )
                        )
                },
                trailingIcon = {
                    assertThat(contentColor()).isEqualTo(MaterialTheme.colors.error)
                }
            )
        }
    }

    @Test
    fun testFilledTextField_imeActionAndKeyboardTypePropagatedDownstream() {
        val textInputService = mock<TextInputService>()
        testRule.setContent {
            Providers(
                TextInputServiceAmbient provides textInputService
            ) {
                var text = state { TextFieldValue("") }
                FilledTextField(
                    modifier = Modifier.testTag(TextfieldTag),
                    value = text.value,
                    onValueChange = { text.value = it },
                    label = {},
                    imeAction = ImeAction.Go,
                    keyboardType = KeyboardType.Email
                )
            }
        }

        clickAndAdvanceClock(TextfieldTag, 200)

        runOnIdleCompose {
            verify(textInputService, atLeastOnce()).startInput(
                value = any(),
                keyboardType = eq(KeyboardType.Email),
                imeAction = eq(ImeAction.Go),
                onEditCommand = any(),
                onImeActionPerformed = any()
            )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testFilledTextField_visualTransformationPropagated() {
        testRule.setMaterialContent {
            FilledTextField(
                modifier = Modifier.testTag(TextfieldTag),
                value = "qwerty",
                onValueChange = {},
                label = {},
                visualTransformation = PasswordVisualTransformation('\u0020'),
                backgroundColor = Color.White,
                shape = RectangleShape
            )
        }

        findByTag(TextfieldTag)
            .captureToBitmap()
            .assertShape(
                density = testRule.density,
                backgroundColor = Color.White,
                shapeColor = Color.White,
                shape = RectangleShape,
                // avoid elevation artifacts
                shapeOverlapPixelCount = with(testRule.density) { 3.dp.toPx() }
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testFilledTextField_alphaNotSet_toBackgroundColorAndTransparentColors() {
        val latch = CountDownLatch(1)

        testRule.setMaterialContent {
            Stack(Modifier.drawBackground(Color.White)) {
                FilledTextField(
                    modifier = Modifier.testTag(TextfieldTag),
                    value = "",
                    onValueChange = {},
                    label = {},
                    shape = RectangleShape,
                    backgroundColor = Color.Blue,
                    activeColor = Color.Transparent,
                    inactiveColor = Color.Transparent,
                    onFocusChange = { focused ->
                        if (focused) latch.countDown()
                    }
                )
            }
        }

        val expectedColor = Color.Blue.copy(alpha = 0.12f).compositeOver(Color.White)

        findByTag(TextfieldTag)
            .captureToBitmap()
            .assertShape(
                density = testRule.density,
                backgroundColor = Color.White,
                shapeColor = expectedColor,
                shape = RectangleShape,
                // avoid elevation artifacts
                shapeOverlapPixelCount = with(testRule.density) { 1.dp.toPx() }
            )

        findByTag(TextfieldTag).doClick()
        assert(latch.await(1, TimeUnit.SECONDS))

        findByTag(TextfieldTag)
            .captureToBitmap()
            .assertShape(
                density = testRule.density,
                backgroundColor = Color.White,
                shapeColor = expectedColor,
                shape = RectangleShape,
                // avoid elevation artifacts
                shapeOverlapPixelCount = with(testRule.density) { 1.dp.toPx() }
            )
    }

    @Test
    fun testFilledTextField_onTextInputStartedCallback() {
        var controller: SoftwareKeyboardController? = null

        testRule.setMaterialContent {
            FilledTextField(
                modifier = Modifier.testTag(TextfieldTag),
                value = "",
                onValueChange = {},
                label = {},
                onTextInputStarted = {
                    controller = it
                }
            )
        }
        assertThat(controller).isNull()

        findByTag(TextfieldTag)
            .doClick()

        runOnIdleCompose {
            assertThat(controller).isNotNull()
        }
    }

    @Test
    fun testFilledTextField_imeActionCallback_withSoftwareKeyboardController() {
        var controller: SoftwareKeyboardController? = null

        testRule.setMaterialContent {
            FilledTextField(
                modifier = Modifier.testTag(TextfieldTag),
                value = "",
                onValueChange = {},
                label = {},
                imeAction = ImeAction.Go,
                onImeActionPerformed = { _, softwareKeyboardController ->
                    controller = softwareKeyboardController
                }
            )
        }
        assertThat(controller).isNull()

        findByTag(TextfieldTag)
            .doSendImeAction()

        runOnIdleCompose {
            assertThat(controller).isNotNull()
        }
    }

    private fun clickAndAdvanceClock(tag: String, time: Long) {
        findByTag(tag).doClick()
        waitForIdle()
        testRule.clockTestRule.pauseClock()
        testRule.clockTestRule.advanceClock(time)
    }
}