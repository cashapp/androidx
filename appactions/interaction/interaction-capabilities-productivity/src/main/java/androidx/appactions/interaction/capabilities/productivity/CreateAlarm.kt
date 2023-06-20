/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.productivity

import androidx.appactions.builtintypes.types.Alarm
import androidx.appactions.builtintypes.types.GenericErrorStatus
import androidx.appactions.builtintypes.types.ObjectCreationLimitReachedStatus
import androidx.appactions.builtintypes.types.Schedule
import androidx.appactions.builtintypes.types.SuccessStatus
import androidx.appactions.interaction.capabilities.core.BaseExecutionSession
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.CapabilityFactory
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.converters.UnionTypeSpec
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecRegistry
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.core.properties.StringValue
import androidx.appactions.interaction.capabilities.serializers.types.ALARM_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.types.GENERIC_ERROR_STATUS_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.types.OBJECT_CREATION_LIMIT_REACHED_STATUS_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.types.SCHEDULE_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.types.SUCCESS_STATUS_TYPE_SPEC

/** A capability corresponding to actions.intent.CREATE_ALARM */
@CapabilityFactory(name = CreateAlarm.CAPABILITY_NAME)
class CreateAlarm private constructor() {
    internal enum class SlotMetadata(val path: String) {
        SCHEDULE("alarm.alarmSchedule"),
        NAME("alarm.name"),
        IDENTIFIER("alarm.identifier")
    }

    class CapabilityBuilder :
        Capability.Builder<
            CapabilityBuilder,
            Arguments,
            Output,
            Confirmation,
            ExecutionSession
            >(ACTION_SPEC) {
        fun setIdentifierProperty(
            identifier: Property<StringValue>
        ): CapabilityBuilder = setProperty(
            SlotMetadata.IDENTIFIER.path,
            identifier,
            TypeConverters.STRING_VALUE_ENTITY_CONVERTER
        )

        fun setNameProperty(name: Property<StringValue>): CapabilityBuilder = setProperty(
            SlotMetadata.NAME.path,
            name,
            TypeConverters.STRING_VALUE_ENTITY_CONVERTER
        )

        fun setScheduleProperty(
            alarmSchedule: Property<Schedule>
        ): CapabilityBuilder = setProperty(
            SlotMetadata.SCHEDULE.path,
            alarmSchedule,
            EntityConverter.of(SCHEDULE_TYPE_SPEC)
        )
    }

    class Arguments internal constructor(
        val identifier: String?,
        val name: String?,
        val schedule: Schedule?
    ) {
        override fun toString(): String {
            return "Arguments(identifier=$identifier,name=$name,schedule=$schedule)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Arguments

            if (identifier != other.identifier) return false
            if (name != other.name) return false
            if (schedule != other.schedule) return false

            return true
        }

        override fun hashCode(): Int {
            var result = identifier.hashCode()
            result += 31 * name.hashCode()
            result += 31 * schedule.hashCode()
            return result
        }

        class Builder {
            private var identifier: String? = null
            private var name: String? = null
            private var schedule: Schedule? = null

            fun setIdentifier(identifier: String): Builder = apply { this.identifier = identifier }

            fun setName(name: String): Builder = apply { this.name = name }

            fun setSchedule(schedule: Schedule): Builder = apply { this.schedule = schedule }

            fun build(): Arguments = Arguments(identifier, name, schedule)
        }
    }

    class Output internal constructor(val alarm: Alarm?, val executionStatus: ExecutionStatus?) {
        override fun toString(): String {
            return "Output(alarm=$alarm,executionStatus=$executionStatus)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Output

            if (alarm != other.alarm) return false
            if (executionStatus != other.executionStatus) return false

            return true
        }

        override fun hashCode(): Int {
            var result = alarm.hashCode()
            result += 31 * executionStatus.hashCode()
            return result
        }

        class Builder {
            private var alarm: Alarm? = null
            private var executionStatus: ExecutionStatus? = null

            fun setAlarm(alarm: Alarm): Builder = apply { this.alarm = alarm }

            fun setExecutionStatus(executionStatus: ExecutionStatus) = apply {
                this.executionStatus = executionStatus
            }

            fun setExecutionStatus(successStatus: SuccessStatus) = apply {
                this.setExecutionStatus(ExecutionStatus(successStatus))
            }

            fun setExecutionStatus(genericErrorStatus: GenericErrorStatus) = apply {
                this.setExecutionStatus(ExecutionStatus(genericErrorStatus))
            }

            fun setExecutionStatus(
                objectCreationLimitReachedStatus: ObjectCreationLimitReachedStatus
            ) = apply {
                    this.setExecutionStatus(ExecutionStatus(objectCreationLimitReachedStatus))
            }

            fun build(): Output = Output(alarm, executionStatus)
        }
    }

    class ExecutionStatus {
        private var successStatus: SuccessStatus? = null
        private var genericErrorStatus: GenericErrorStatus? = null
        private var objectCreationLimitReachedStatus: ObjectCreationLimitReachedStatus? = null

        constructor(successStatus: SuccessStatus) {
            this.successStatus = successStatus
        }

        constructor(genericErrorStatus: GenericErrorStatus) {
            this.genericErrorStatus = genericErrorStatus
        }

        constructor(objectCreationLimitReachedStatus: ObjectCreationLimitReachedStatus) {
            this.objectCreationLimitReachedStatus = objectCreationLimitReachedStatus
        }

        companion object {
            private val TYPE_SPEC = UnionTypeSpec.Builder<ExecutionStatus>()
                .bindMemberType(
                    memberGetter = ExecutionStatus::successStatus,
                    ctor = { ExecutionStatus(it) },
                    typeSpec = SUCCESS_STATUS_TYPE_SPEC
                ).bindMemberType(
                    memberGetter = ExecutionStatus::genericErrorStatus,
                    ctor = { ExecutionStatus(it) },
                    typeSpec = GENERIC_ERROR_STATUS_TYPE_SPEC
                ).bindMemberType(
                    memberGetter = ExecutionStatus::objectCreationLimitReachedStatus,
                    ctor = { ExecutionStatus(it) },
                    typeSpec = OBJECT_CREATION_LIMIT_REACHED_STATUS_TYPE_SPEC
                ).build()
            internal val PARAM_VALUE_CONVERTER = ParamValueConverter.of(TYPE_SPEC)
        }
    }

    sealed interface ExecutionSession : BaseExecutionSession<Arguments, Output>
    class Confirmation internal constructor()

    companion object {
        /** Canonical name for [CreateAlarm] capability */
        const val CAPABILITY_NAME = "actions.intent.CREATE_ALARM"
        private val ACTION_SPEC =
            ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
                .setArguments(Arguments::class.java, Arguments::Builder, Arguments.Builder::build)
                .setOutput(Output::class.java)
                .bindParameter(
                    SlotMetadata.IDENTIFIER.path,
                    Arguments::identifier,
                    Arguments.Builder::setIdentifier,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER
                )
                .bindParameter(
                    SlotMetadata.NAME.path,
                    Arguments::name,
                    Arguments.Builder::setName,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER
                )
                .bindParameter(
                    SlotMetadata.SCHEDULE.path,
                    Arguments::schedule,
                    Arguments.Builder::setSchedule,
                    ParamValueConverter.of(SCHEDULE_TYPE_SPEC)
                )
                .bindOutput(
                    "alarm",
                    Output::alarm,
                    ParamValueConverter.of(ALARM_TYPE_SPEC)
                )
                .bindOutput(
                    "executionStatus",
                    Output::executionStatus,
                    ExecutionStatus.PARAM_VALUE_CONVERTER
                )
                .build()
        init {
            ActionSpecRegistry.registerArgumentsClass(Arguments::class, ACTION_SPEC)
        }
    }
}