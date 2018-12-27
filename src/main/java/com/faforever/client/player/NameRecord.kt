package com.faforever.client.player


import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

import java.time.OffsetDateTime

class NameRecord private constructor() {
    private val name: StringProperty
    private val changeDate: ObjectProperty<OffsetDateTime>

    init {
        this.name = SimpleStringProperty()
        this.changeDate = SimpleObjectProperty()
    }

    fun getName(): String {
        return name.get()
    }

    fun setName(name: String) {
        this.name.set(name)
    }

    fun nameProperty(): StringProperty {
        return name
    }

    fun getChangeDate(): OffsetDateTime {
        return changeDate.get()
    }

    fun setChangeDate(changeDate: OffsetDateTime) {
        this.changeDate.set(changeDate)
    }

    fun changeDateProperty(): ObjectProperty<OffsetDateTime> {
        return changeDate
    }

    companion object {

        fun fromDto(dto: com.faforever.client.api.dto.NameRecord): NameRecord {
            val nameRecord = NameRecord()
            nameRecord.setName(dto.getName())
            nameRecord.setChangeDate(dto.getChangeTime())
            return nameRecord
        }
    }
}
