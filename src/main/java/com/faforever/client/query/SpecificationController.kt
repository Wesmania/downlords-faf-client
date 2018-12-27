package com.faforever.client.query

import com.faforever.client.fx.Controller
import com.faforever.client.i18n.I18n
import com.faforever.client.util.ProgrammingError
import com.faforever.client.util.ReflectionUtil
import com.github.rutledgepaulv.qbuilders.builders.QBuilder
import com.github.rutledgepaulv.qbuilders.conditions.Condition
import com.github.rutledgepaulv.qbuilders.operators.ComparisonOperator
import com.github.rutledgepaulv.qbuilders.properties.concrete.BooleanProperty
import com.github.rutledgepaulv.qbuilders.properties.concrete.EnumProperty
import com.github.rutledgepaulv.qbuilders.properties.concrete.InstantProperty
import com.github.rutledgepaulv.qbuilders.properties.concrete.StringProperty
import com.github.rutledgepaulv.qbuilders.properties.virtual.EquitableProperty
import com.github.rutledgepaulv.qbuilders.properties.virtual.ListableProperty
import com.github.rutledgepaulv.qbuilders.properties.virtual.NumberProperty
import com.github.rutledgepaulv.qbuilders.properties.virtual.Property
import com.google.common.collect.ImmutableMap
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.collections.transformation.FilteredList
import javafx.scene.Node
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.layout.HBox
import javafx.util.StringConverter
import lombok.SneakyThrows
import org.apache.maven.artifact.versioning.ComparableVersion
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.util.Assert
import org.springframework.util.ClassUtils

import java.lang.reflect.ParameterizedType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.Temporal
import java.util.ArrayList
import java.util.Arrays
import java.util.Optional
import java.util.stream.Collectors

import com.github.rutledgepaulv.qbuilders.operators.ComparisonOperator.EQ
import com.github.rutledgepaulv.qbuilders.operators.ComparisonOperator.GT
import com.github.rutledgepaulv.qbuilders.operators.ComparisonOperator.GTE
import com.github.rutledgepaulv.qbuilders.operators.ComparisonOperator.IN
import com.github.rutledgepaulv.qbuilders.operators.ComparisonOperator.LT
import com.github.rutledgepaulv.qbuilders.operators.ComparisonOperator.LTE
import com.github.rutledgepaulv.qbuilders.operators.ComparisonOperator.NE
import com.github.rutledgepaulv.qbuilders.operators.ComparisonOperator.NIN
import com.github.rutledgepaulv.qbuilders.operators.ComparisonOperator.RE

/**
 * Controller for building a specification in the sense of Domain Driven Design, e.g. `login == "Someone"` or
 * `rating > 500`. The specification can then be converted into a condition to be used in a [QBuilder].
 * Before it can be used, the type of the object to query has to be defined first by using [.setRootType].
 * This controller consists of a property ChoiceBox, an operator ChoiceBox and a value field. The items available in the
 * operator ChoiceBox depend on the selected property and its respective type in the target class.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class SpecificationController(private val i18n: I18n) : Controller<Node> {
    private val comparisonOperators: FilteredList<ComparisonOperator>
    var propertyField: ComboBox<String>? = null
    var operationField: ComboBox<ComparisonOperator>? = null
    var valueField: ComboBox<Any>? = null
    var specificationRoot: HBox? = null
    var datePicker: DatePicker? = null
    private var rootType: Class<*>? = null
    private var properties: Map<String, String>? = null

    override val root: Node?
        get() = specificationRoot

    init {
        comparisonOperators = FilteredList(FXCollections.observableArrayList(operatorToI18nKey.keys))
    }

    override fun initialize() {
        datePicker!!.value = LocalDate.now()
        datePicker!!.managedProperty().bind(datePicker!!.visibleProperty())
        datePicker!!.isVisible = false

        valueField!!.managedProperty().bind(valueField!!.visibleProperty())
        // JFXComboBox throws an exception if the field is bound bidirectionally but editable (or so...)
        valueField!!.editableProperty().bind(valueField!!.visibleProperty())

        operationField!!.setItems(comparisonOperators)
        operationField!!.setConverter(object : StringConverter() {
            fun toString(`object`: ComparisonOperator?): String {
                return if (`object` == null) {
                    ""
                } else i18n.get(operatorToI18nKey[`object`])
            }

            override fun fromString(string: String): ComparisonOperator {
                throw UnsupportedOperationException("Not supported")
            }
        })

        propertyField!!.setConverter(object : StringConverter<String>() {
            override fun toString(`object`: String): String {
                return i18n.get(properties!![`object`])
            }

            override fun fromString(string: String): String {
                throw UnsupportedOperationException("Not supported")
            }
        })
        propertyField!!.valueProperty().addListener { observable, oldValue, newValue ->
            comparisonOperators.setPredicate { comparisonOperator -> isOperatorApplicable(comparisonOperator, newValue) }
            if (!isOperatorApplicable(operationField!!.value, newValue)) {
                operationField!!.selectionModel.select(0)
            }
            populateValueField(getPropertyClass(newValue))
        }
    }

    /**
     * If there are predefined values for a property (like an enum or boolean), the value field is populated with the
     * possible values. If its an instant, a date picker will be displayed and the value field will be hidden. The
     * selected date will then be populated to the hidden value field in its proper format.
     */
    private fun populateValueField(propertyClass: Class<*>) {
        valueField!!.isVisible = true
        valueField!!.items.clear()
        valueField!!.valueProperty().unbind()
        valueField!!.setValue(null)
        datePicker!!.isVisible = false

        if (ClassUtils.isAssignable(Boolean::class.java, propertyClass)) {
            valueField!!.items.setAll(java.lang.Boolean.TRUE, java.lang.Boolean.FALSE)
            valueField!!.selectionModel.select(0)
        } else if (ClassUtils.isAssignable(Enum<*>::class.java, propertyClass)) {
            valueField!!.items.setAll(*propertyClass.enumConstants)
            valueField!!.selectionModel.select(0)
        } else if (ClassUtils.isAssignable(Temporal::class.java, propertyClass)) {
            datePicker!!.isVisible = true
            valueField!!.isVisible = false
            valueField!!.valueProperty()
                    .bind(Bindings.createStringBinding({
                        datePicker!!.value.atStartOfDay(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ISO_INSTANT)
                    }, datePicker!!.valueProperty()))
        }
    }

    private fun isOperatorApplicable(comparisonOperator: ComparisonOperator, propertyName: String): Boolean {
        var propertyClass = ClassUtils.resolvePrimitiveIfNecessary(getPropertyClass(propertyName))

        for (aClass in VALID_OPERATORS.keys) {
            if (aClass.isAssignableFrom(propertyClass)) {
                propertyClass = aClass
            }
        }

        if (!VALID_OPERATORS.containsKey(propertyClass)) {
            throw IllegalStateException("No valid operators specified for property: $propertyName")
        }

        return VALID_OPERATORS[propertyClass].contains(comparisonOperator)
    }

    /**
     * Sets the properties that can be queried. These must match the field names of the specified type.
     *
     * @see .setRootType
     */
    fun setProperties(properties: Map<String, String>) {
        this.properties = properties
        propertyField!!.setItems(FXCollections.observableList(ArrayList(properties.keys)))
        propertyField!!.selectionModel.select(0)
        operationField!!.selectionModel.select(0)
    }

    /**
     * Sets the type this controller can build queries for (including types used in relationships).
     */
    fun setRootType(rootType: Class<*>) {
        this.rootType = rootType
    }

    private fun splitInts(value: String): Collection<Int> {
        return Arrays.stream(value.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                .map<Int>(Function<String, Int> { Integer.parseInt(it) })
                .collect<List<Int>, Any>(Collectors.toList())
    }

    fun appendTo(qBuilder: QBuilder<*>): Optional<Condition<*>> {
        val propertyName = propertyField!!.value ?: return Optional.empty()

        val comparisonOperator = operationField!!.value ?: return Optional.empty()

        val value = Optional.ofNullable(valueField!!.value).map<String>(Function<Any, String> { it.toString() }).orElse("")
        if (value.isEmpty()) {
            return Optional.empty()
        }

        val propertyClass = getPropertyClass(propertyName)
        val property = getProperty(qBuilder, propertyName, propertyClass)

        if (property is NumberProperty<*, *>) {
            return Optional.ofNullable(getNumberCondition(comparisonOperator, value, propertyClass, property))
        }
        if (property is BooleanProperty<*>) {
            return Optional.ofNullable(getBooleanCondition(comparisonOperator, value, propertyClass, property))
        }
        if (property is InstantProperty<*>) {
            return Optional.ofNullable(getInstantCondition(comparisonOperator, value, propertyClass, property))
        }
        return if (property is EnumProperty<*, *>) {
            Optional.ofNullable(getEquitableCondition(comparisonOperator, value, propertyClass, property))
        } else Optional.ofNullable(getStringCondition(comparisonOperator, value, propertyClass, property as StringProperty<*>))

    }

    @SneakyThrows
    private fun getPropertyClass(propertyName: String): Class<*> {
        Assert.state(rootType != null, "rootType has not been set")
        var targetClass: Class<*> = rootType

        val path = ArrayList(Arrays.asList(*propertyName.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))

        var fieldName: String
        while (!path.isEmpty()) {
            fieldName = path.removeAt(0)
            val clazz = ReflectionUtil.getDeclaredField(fieldName, targetClass)

            if (Iterable<*>::class.java.isAssignableFrom(clazz)) {
                val genericType = targetClass.getDeclaredField(fieldName).genericType as ParameterizedType
                targetClass = genericType.actualTypeArguments[0] as Class<*>
            } else {
                targetClass = clazz
            }
        }
        return targetClass
    }

    private fun getProperty(qBuilder: QBuilder<*>, property: String, fieldType: Class<*>): Property<*> {
        val prop: Property<*>
        if (ClassUtils.isAssignable(Number::class.java, fieldType)) {
            prop = qBuilder.intNum(property)
        } else if (ClassUtils.isAssignable(Float::class.java, fieldType) || ClassUtils.isAssignable(Double::class.java, fieldType)) {
            prop = qBuilder.doubleNum(property)
        } else if (ClassUtils.isAssignable(Boolean::class.java, fieldType)) {
            prop = qBuilder.bool(property)
        } else if (ClassUtils.isAssignable(String::class.java, fieldType)) {
            prop = qBuilder.string(property)
        } else if (ClassUtils.isAssignable(Temporal::class.java, fieldType)) {
            prop = qBuilder.instant(property)
        } else if (ClassUtils.isAssignable(Enum<*>::class.java, fieldType)) {
            prop = qBuilder.enumeration<S>(property)
        } else if (ClassUtils.isAssignable(ComparableVersion::class.java, fieldType)) {
            prop = qBuilder.string(property)
        } else {
            throw IllegalStateException("Unsupported target type: $fieldType")
        }
        return prop
    }

    private fun <T : EquitableProperty<*, *>> getEquitableCondition(
            comparisonOperator: ComparisonOperator, value: String, fieldType: Class<*>, prop: T): Condition<*> where T : ListableProperty<*, *> {

        if (comparisonOperator === EQ) {
            return prop.eq(value)
        }
        if (comparisonOperator === NE) {
            return prop.ne(value)
        }
        if (comparisonOperator === IN) {
            return prop.`in`(*value.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() as Array<Any>)
        }
        if (comparisonOperator === NIN) {
            return prop.nin(*value.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() as Array<Any>)
        }
        throw ProgrammingError("Operator '$comparisonOperator' should not have been allowed for type: $fieldType")
    }


    private fun getStringCondition(comparisonOperator: ComparisonOperator, value: String, propertyClass: Class<*>, prop: StringProperty<*>): Condition<*> {

        if (comparisonOperator === EQ) {
            return prop.eq(value)
        }
        if (comparisonOperator === NE) {
            return prop.ne(value)
        }
        if (comparisonOperator === IN) {
            return prop.`in`(*value.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() as Array<Any>)
        }
        if (comparisonOperator === NIN) {
            return prop.nin(*value.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() as Array<Any>)
        }
        if (comparisonOperator === RE) {
            return prop.eq("*$value*")
        }
        throw ProgrammingError("Operator '$comparisonOperator' should not have been allowed for type: $propertyClass")
    }

    private fun getInstantCondition(comparisonOperator: ComparisonOperator, value: String, fieldType: Class<*>, prop: InstantProperty<*>): Condition<*> {
        val instant = Instant.parse(value)
        if (comparisonOperator === EQ) {
            return prop.eq(instant)
        }
        if (comparisonOperator === NE) {
            return prop.ne(instant)
        }
        if (comparisonOperator === GT || comparisonOperator === GTE) {
            return prop.after(instant, comparisonOperator === GT)
        }
        if (comparisonOperator === LT || comparisonOperator === LTE) {
            return prop.before(instant, comparisonOperator === LT)
        }
        throw ProgrammingError("Operator '$comparisonOperator' should not have been allowed for type: $fieldType")
    }

    private fun getBooleanCondition(comparisonOperator: ComparisonOperator, value: String, fieldType: Class<*>, prop: BooleanProperty<*>): Condition<*> {
        val booleanValue = java.lang.Boolean.parseBoolean(value)

        if (comparisonOperator === EQ && booleanValue || comparisonOperator === NE && !booleanValue) {
            return prop.isTrue()
        }

        if (comparisonOperator === EQ || comparisonOperator === NE) {
            return prop.isFalse()
        }
        throw ProgrammingError("Operator '$comparisonOperator' should not have been allowed for type: $fieldType")
    }

    private fun getNumberCondition(comparisonOperator: ComparisonOperator, value: String, fieldType: Class<*>, prop: NumberProperty<*, *>): Condition<*> {
        if (comparisonOperator === EQ) {
            return prop.eq(Integer.parseInt(value))
        }
        if (comparisonOperator === NE) {
            return prop.ne(Integer.parseInt(value))
        }
        if (comparisonOperator === GT) {
            return prop.gt(Integer.parseInt(value))
        }
        if (comparisonOperator === GTE) {
            return prop.gte(Integer.parseInt(value))
        }
        if (comparisonOperator === LT) {
            return prop.lt(Integer.parseInt(value))
        }
        if (comparisonOperator === LTE) {
            return prop.lte(Integer.parseInt(value))
        }
        if (comparisonOperator === IN) {
            return prop.`in`(splitInts(value))
        }
        if (comparisonOperator === NIN) {
            return prop.nin(splitInts(value))
        }
        throw ProgrammingError("Operator '$comparisonOperator' should not have been allowed for type: $fieldType")
    }

    companion object {

        private val operatorToI18nKey = ImmutableMap.builder<ComparisonOperator, String>()
                .put(RE, "query.contains")
                .put(EQ, "query.equals")
                .put(NE, "query.notEquals")
                .put(GT, "query.greaterThan")
                .put(GTE, "query.greaterThanEquals")
                .put(LT, "query.lessThan")
                .put(LTE, "query.lessThanEquals")
                .put(IN, "query.in")
                .put(NIN, "query.notIn")
                .build()

        private val VALID_OPERATORS = ImmutableMap.builder<Class<*>, Collection<ComparisonOperator>>()
                .put(Number::class.java, Arrays.asList(EQ, NE, GT, GTE, LT, LTE, IN, NIN))
                .put(Temporal::class.java, Arrays.asList(EQ, NE, GT, GTE, LT, LTE))
                .put(String::class.java, Arrays.asList(EQ, NE, IN, NIN, RE))
                .put(Boolean::class.java, Arrays.asList(EQ, NE))
                .put(Enum<*>::class.java, Arrays.asList(EQ, NE, IN, NIN))
                .put(ComparableVersion::class.java, Arrays.asList(EQ, NE, GT, GTE, LT, LTE, IN, NIN))
                .build()
    }
}
