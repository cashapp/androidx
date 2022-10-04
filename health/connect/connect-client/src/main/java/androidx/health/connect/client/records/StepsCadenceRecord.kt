/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.connect.client.records

import androidx.annotation.FloatRange
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregateMetric.AggregationType.AVERAGE
import androidx.health.connect.client.aggregate.AggregateMetric.AggregationType.MAXIMUM
import androidx.health.connect.client.aggregate.AggregateMetric.AggregationType.MINIMUM
import androidx.health.connect.client.aggregate.AggregateMetric.Companion.doubleMetric
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

/** Captures the user's steps cadence. Each record represents a series of measurements. */
class StepsCadenceRecord(
    override val startTime: Instant,
    override val startZoneOffset: ZoneOffset?,
    override val endTime: Instant,
    override val endZoneOffset: ZoneOffset?,
    override val samples: List<Sample>,
    override val metadata: Metadata = Metadata.EMPTY,
) : SeriesRecord<StepsCadenceRecord.Sample> {

    init {
        require(!startTime.isAfter(endTime)) { "startTime must not be after endTime." }
    }

    /*
     * Generated by the IDE: Code -> Generate -> "equals() and hashCode()".
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StepsCadenceRecord) return false

        if (startTime != other.startTime) return false
        if (startZoneOffset != other.startZoneOffset) return false
        if (endTime != other.endTime) return false
        if (endZoneOffset != other.endZoneOffset) return false
        if (samples != other.samples) return false
        if (metadata != other.metadata) return false

        return true
    }

    /*
     * Generated by the IDE: Code -> Generate -> "equals() and hashCode()".
     */
    override fun hashCode(): Int {
        var result = startTime.hashCode()
        result = 31 * result + (startZoneOffset?.hashCode() ?: 0)
        result = 31 * result + endTime.hashCode()
        result = 31 * result + (endZoneOffset?.hashCode() ?: 0)
        result = 31 * result + samples.hashCode()
        result = 31 * result + metadata.hashCode()
        return result
    }

    companion object {
        private const val TYPE = "StepsCadenceSeries"
        private const val RATE_FIELD = "rate"

        /**
         * Metric identifier to retrieve average steps cadence from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField val RATE_AVG: AggregateMetric<Double> = doubleMetric(TYPE, AVERAGE, RATE_FIELD)

        /**
         * Metric identifier to retrieve minimum steps cadence from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField val RATE_MIN: AggregateMetric<Double> = doubleMetric(TYPE, MINIMUM, RATE_FIELD)

        /**
         * Metric identifier to retrieve maximum steps cadence from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField val RATE_MAX: AggregateMetric<Double> = doubleMetric(TYPE, MAXIMUM, RATE_FIELD)
    }

    /**
     * Represents a single measurement of the steps cadence.
     *
     * @param time The point in time when the measurement was taken.
     * @param rate Rate in steps per minute. Valid range: 0-10000.
     */
    class Sample(
        val time: Instant,
        @FloatRange(from = 0.0, to = 10_000.0) val rate: Double,
    ) {

        init {
            requireNonNegative(value = rate, name = "rate")
        }

        /*
         * Generated by the IDE: Code -> Generate -> "equals() and hashCode()".
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Sample) return false

            if (time != other.time) return false
            if (rate != other.rate) return false

            return true
        }

        /*
         * Generated by the IDE: Code -> Generate -> "equals() and hashCode()".
         */
        override fun hashCode(): Int {
            var result = time.hashCode()
            result = 31 * result + rate.hashCode()
            return result
        }
    }
}
