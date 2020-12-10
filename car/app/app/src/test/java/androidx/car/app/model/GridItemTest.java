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

package androidx.car.app.model;

import static androidx.car.app.model.CarIcon.ALERT;
import static androidx.car.app.model.CarIcon.BACK;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.os.RemoteException;

import androidx.car.app.OnDoneCallback;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link GridItem}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class GridItemTest {

    @Test
    public void create_defaultValues() {
        GridItem gridItem = GridItem.builder().setTitle("Title").setImage(BACK).build();

        assertThat(BACK).isEqualTo(gridItem.getImage());
        assertThat(gridItem.getImageType()).isEqualTo(GridItem.IMAGE_TYPE_LARGE);
        assertThat(gridItem.getTitle()).isNotNull();
        assertThat(gridItem.getText()).isNull();
    }

    @Test
    public void create_isLoading() {
        GridItem gridItem = GridItem.builder().setTitle("Title").setLoading(true).build();
        assertThat(gridItem.isLoading()).isTrue();
    }

    @Test
    public void title_charSequence() {
        String title = "foo";
        GridItem gridItem = GridItem.builder().setTitle(title).setImage(BACK).build();

        assertThat(CarText.create(title)).isEqualTo(gridItem.getTitle());
    }

    @Test
    public void title_throwsIfNotSet() {
        // Not set
        assertThrows(IllegalStateException.class, () -> GridItem.builder().setImage(BACK).build());

        // Not set
        assertThrows(
                IllegalArgumentException.class, () -> GridItem.builder().setTitle("").setImage(
                        BACK).build());
    }

    @Test
    public void text_charSequence() {
        String text = "foo";
        GridItem gridItem = GridItem.builder().setTitle("title").setText(text).setImage(
                BACK).build();

        assertThat(CarText.create(text)).isEqualTo(gridItem.getText());
    }

    @Test
    public void textWithoutTitle_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> GridItem.builder().setText("text").setImage(BACK).build());
    }

    @Test
    public void setIsLoading_contentsSet_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> GridItem.builder().setLoading(true).setTitle("foo").setImage(BACK).build());
    }

    @Test
    public void create_noImage_throws() {
        assertThrows(IllegalStateException.class, () -> GridItem.builder().setTitle("foo").build());
    }

    @Test
    public void equals() {
        String title = "title";
        String text = "text";
        GridItem gridItem = GridItem.builder().setTitle(title).setText(text).setImage(BACK).build();

        assertThat(GridItem.builder().setTitle(title).setText(text).setImage(BACK).build())
                .isEqualTo(gridItem);
    }

    @Test
    public void notEquals_differentTitle() {
        String title = "title";
        GridItem gridItem = GridItem.builder().setTitle(title).setImage(BACK).build();

        assertThat(GridItem.builder().setTitle("foo").setImage(BACK).build()).isNotEqualTo(
                gridItem);
    }

    @Test
    public void notEquals_differentText() {
        String title = "title";
        String text = "text";
        GridItem gridItem = GridItem.builder().setTitle(title).setText(text).setImage(BACK).build();

        assertThat(GridItem.builder().setTitle(title).setText("foo").setImage(BACK).build())
                .isNotEqualTo(gridItem);
    }

    @Test
    public void notEquals_differentImage() {
        GridItem gridItem = GridItem.builder().setTitle("Title").setImage(BACK).build();

        assertThat(GridItem.builder().setImage(ALERT).setTitle("Title").build()).isNotEqualTo(
                gridItem);
    }

    @Test
    public void clickListener() throws RemoteException {
        OnClickListener onClickListener = mock(OnClickListener.class);
        GridItem gridItem =
                GridItem.builder().setTitle("Title").setImage(BACK).setOnClickListener(
                        onClickListener).build();
        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        gridItem.getOnClickListener().onClick(onDoneCallback);
        verify(onClickListener).onClick();
        verify(onDoneCallback).onSuccess(null);
    }
}
