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

package androidx.camera.core.processing;

import static androidx.camera.core.ImageProcessingUtil.writeJpegBytesToSurface;
import static androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;
import static androidx.core.util.Preconditions.checkState;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.camera.core.Logger;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceProcessor;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Supplier;

import com.google.common.util.concurrent.ListenableFuture;

import kotlin.Triple;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A default implementation of {@link SurfaceProcessor}.
 *
 * <p> This implementation simply copies the frame from the source to the destination with the
 * transformation defined in {@link SurfaceOutput#updateTransformMatrix}.
 */
@RequiresApi(21)
public class DefaultSurfaceProcessor implements SurfaceProcessorInternal,
        SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "DefaultSurfaceProcessor";

    private final OpenGlRenderer mGlRenderer;
    @VisibleForTesting
    final HandlerThread mGlThread;
    private final Executor mGlExecutor;
    @VisibleForTesting
    final Handler mGlHandler;
    private final AtomicBoolean mIsReleaseRequested = new AtomicBoolean(false);
    private final float[] mTextureMatrix = new float[16];
    private final float[] mSurfaceOutputMatrix = new float[16];
    // Map of current set of available outputs. Only access this on GL thread.
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Map<SurfaceOutput, Surface> mOutputSurfaces = new LinkedHashMap<>();

    // Only access this on GL thread.
    private int mInputSurfaceCount = 0;
    // Only access this on GL thread.
    private boolean mIsReleased = false;
    // Only access this on GL thread.
    private final List<CallbackToFutureAdapter.Completer<Void>> mPendingSnapshots =
            new ArrayList<>();

    /** Constructs {@link DefaultSurfaceProcessor} with default shaders. */
    DefaultSurfaceProcessor() {
        this(ShaderProvider.DEFAULT);
    }

    /**
     * Constructs {@link DefaultSurfaceProcessor} with custom shaders.
     *
     * @param shaderProvider custom shader provider for OpenGL rendering.
     * @throws IllegalArgumentException if the shaderProvider provides invalid shader.
     */
    DefaultSurfaceProcessor(@NonNull ShaderProvider shaderProvider) {
        mGlThread = new HandlerThread("GL Thread");
        mGlThread.start();
        mGlHandler = new Handler(mGlThread.getLooper());
        mGlExecutor = CameraXExecutors.newHandlerExecutor(mGlHandler);
        mGlRenderer = new OpenGlRenderer();
        try {
            initGlRenderer(shaderProvider);
        } catch (RuntimeException e) {
            release();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInputSurface(@NonNull SurfaceRequest surfaceRequest) {
        if (mIsReleaseRequested.get()) {
            surfaceRequest.willNotProvideSurface();
            return;
        }
        executeSafely(() -> {
            mInputSurfaceCount++;
            SurfaceTexture surfaceTexture = new SurfaceTexture(mGlRenderer.getTextureName());
            surfaceTexture.setDefaultBufferSize(surfaceRequest.getResolution().getWidth(),
                    surfaceRequest.getResolution().getHeight());
            Surface surface = new Surface(surfaceTexture);
            surfaceRequest.provideSurface(surface, mGlExecutor, result -> {
                surfaceTexture.setOnFrameAvailableListener(null);
                surfaceTexture.release();
                surface.release();
                mInputSurfaceCount--;
                checkReadyToRelease();
            });
            surfaceTexture.setOnFrameAvailableListener(this, mGlHandler);
        }, surfaceRequest::willNotProvideSurface);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOutputSurface(@NonNull SurfaceOutput surfaceOutput) {
        if (mIsReleaseRequested.get()) {
            surfaceOutput.close();
            return;
        }
        executeSafely(() -> {
            Surface surface = surfaceOutput.getSurface(mGlExecutor, event -> {
                surfaceOutput.close();
                Surface removedSurface = mOutputSurfaces.remove(surfaceOutput);
                if (removedSurface != null) {
                    mGlRenderer.unregisterOutputSurface(removedSurface);
                }
            });
            mGlRenderer.registerOutputSurface(surface);
            mOutputSurfaces.put(surfaceOutput, surface);
        }, surfaceOutput::close);
    }

    /**
     * Release the {@link DefaultSurfaceProcessor}.
     */
    @Override
    public void release() {
        if (mIsReleaseRequested.getAndSet(true)) {
            return;
        }
        executeSafely(() -> {
            mIsReleased = true;
            checkReadyToRelease();
        });
    }

    @NonNull
    @Override
    public ListenableFuture<Void> snapshot() {
        return Futures.nonCancellationPropagating(CallbackToFutureAdapter.getFuture(
                completer -> {
                    executeSafely(() -> mPendingSnapshots.add(completer),
                            () -> completer.setException(
                                    new Exception(
                                            "Failed to snapshot: OpenGLRenderer not ready.")));
                    return "DefaultSurfaceProcessor#snapshot";
                }));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFrameAvailable(@NonNull SurfaceTexture surfaceTexture) {
        if (mIsReleaseRequested.get()) {
            // Ignore frame update if released.
            return;
        }

        surfaceTexture.updateTexImage();
        surfaceTexture.getTransformMatrix(mTextureMatrix);
        // Surface, size and transform matrix for JPEG Surface if exists
        Triple<Surface, Size, float[]> jpegOutput = null;

        for (Map.Entry<SurfaceOutput, Surface> entry : mOutputSurfaces.entrySet()) {
            Surface surface = entry.getValue();
            SurfaceOutput surfaceOutput = entry.getKey();
            surfaceOutput.updateTransformMatrix(mSurfaceOutputMatrix, mTextureMatrix);
            if (surfaceOutput.getFormat() == INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE) {
                // Render GPU output directly.
                mGlRenderer.render(surfaceTexture.getTimestamp(), mSurfaceOutputMatrix, surface);
            } else {
                checkState(surfaceOutput.getFormat() == ImageFormat.JPEG,
                        "Unsupported format: " + surfaceOutput.getFormat());
                checkState(jpegOutput == null, "Only one JPEG output is supported.");
                jpegOutput = new Triple<>(surface, surfaceOutput.getSize(),
                        mSurfaceOutputMatrix.clone());
            }
        }

        // Execute all pending snapshots.
        takeSnapshotAndDrawJpeg(jpegOutput);
    }

    /**
     * Takes a snapshot of the current frame and draws it to given JPEG surface.
     *
     * @param jpegOutput The <Surface, Surface size, transform matrix> tuple for drawing.
     */
    private void takeSnapshotAndDrawJpeg(@Nullable Triple<Surface, Size, float[]> jpegOutput) {
        if (mPendingSnapshots.isEmpty()) {
            // No pending snapshot requests, do nothing.
            return;
        }

        // No JPEG Surface, fail all snapshot requests.
        if (jpegOutput == null) {
            for (CallbackToFutureAdapter.Completer<Void> completer : mPendingSnapshots) {
                completer.setException(
                        new Exception("Failed to snapshot: no JPEG Surface."));
            }
            mPendingSnapshots.clear();
            return;
        }

        // Get JPEG bytes.
        byte[] jpegBytes = getJpegByteArray(jpegOutput.getSecond(), jpegOutput.getThird());
        // Write to JPEG surface, once for each snapshot request.
        for (CallbackToFutureAdapter.Completer<Void> completer : mPendingSnapshots) {
            writeJpegBytesToSurface(jpegOutput.getFirst(), jpegBytes);
            completer.set(null);
        }
        mPendingSnapshots.clear();
    }

    @NonNull
    private byte[] getJpegByteArray(@NonNull Size size, @NonNull float[] textureTransform) {
        // Flip the snapshot. This is for reverting the GL transform added in SurfaceOutputImpl.
        float[] snapshotTransform = new float[16];
        // TODO(b/278109696): move GL flipping to MatrixExt.
        Matrix.setIdentityM(snapshotTransform, 0);
        Matrix.translateM(snapshotTransform, 0, 0f, 1f, 0f);
        Matrix.scaleM(snapshotTransform, 0, 1f, -1f, 1f);
        Matrix.multiplyMM(snapshotTransform, 0, snapshotTransform, 0, textureTransform, 0);
        // Take a snapshot Bitmap and compress it to JPEG.
        Bitmap bitmap = mGlRenderer.snapshot(size, snapshotTransform);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // TODO: Use the JPEG quality from SessionConfig.
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        return outputStream.toByteArray();
    }

    @WorkerThread
    private void checkReadyToRelease() {
        if (mIsReleased && mInputSurfaceCount == 0) {
            // Once release is called, we can stop sending frame to output surfaces.
            for (SurfaceOutput surfaceOutput : mOutputSurfaces.keySet()) {
                surfaceOutput.close();
            }
            for (CallbackToFutureAdapter.Completer<Void> completer : mPendingSnapshots) {
                completer.setException(
                        new Exception("Failed to snapshot: DefaultSurfaceProcessor is released."));
            }
            mOutputSurfaces.clear();
            mGlRenderer.release();
            mGlThread.quit();
        }
    }

    private void initGlRenderer(@NonNull ShaderProvider shaderProvider) {
        ListenableFuture<Void> initFuture = CallbackToFutureAdapter.getFuture(completer -> {
            executeSafely(() -> {
                try {
                    mGlRenderer.init(shaderProvider);
                    completer.set(null);
                } catch (RuntimeException e) {
                    completer.setException(e);
                }
            });
            return "Init GlRenderer";
        });
        try {
            initFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            // If the cause is a runtime exception, throw it directly. Otherwise convert to runtime
            // exception and throw.
            Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new IllegalStateException("Failed to create DefaultSurfaceProcessor", cause);
            }
        }
    }

    private void executeSafely(@NonNull Runnable runnable) {
        executeSafely(runnable, () -> {
            // Do nothing.
        });
    }

    private void executeSafely(@NonNull Runnable runnable, @NonNull Runnable onFailure) {
        try {
            mGlExecutor.execute(() -> {
                if (mIsReleased) {
                    onFailure.run();
                } else {
                    runnable.run();
                }
            });
        } catch (RejectedExecutionException e) {
            Logger.w(TAG, "Unable to executor runnable", e);
            onFailure.run();
        }
    }

    /**
     * Factory class that produces {@link DefaultSurfaceProcessor}.
     *
     * <p> This is for working around the limit that OpenGL cannot be initialized in unit tests.
     */
    public static class Factory {
        private Factory() {
        }

        private static Supplier<SurfaceProcessorInternal> sSupplier = DefaultSurfaceProcessor::new;

        /**
         * Creates a new {@link DefaultSurfaceProcessor} with no-op shader.
         */
        @NonNull
        public static SurfaceProcessorInternal newInstance() {
            return sSupplier.get();
        }

        /**
         * Overrides the {@link DefaultSurfaceProcessor} supplier for testing.
         */
        @VisibleForTesting
        public static void setSupplier(@NonNull Supplier<SurfaceProcessorInternal> supplier) {
            sSupplier = supplier;
        }
    }
}
