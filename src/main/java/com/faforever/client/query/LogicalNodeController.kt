package com.faforever.client.query

import com.faforever.client.fx.Controller
import com.faforever.client.i18n.I18n
import com.faforever.client.util.ProgrammingError
import com.github.rutledgepaulv.qbuilders.conditions.Condition
import javafx.collections.FXCollections
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.layout.Pane
import javafx.util.StringConverter
import lombok.Setter
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.util.Optional

/**
 * Constructs an `AND` or `OR` query.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class LogicalNodeController(private val i18n: I18n) : Controller<Node> {
    var logicalOperatorField: ComboBox<LogicalOperator>? = null
    var specificationController: SpecificationController? = null
    var logicalNodeRoot: Pane? = null
    var removeCriteriaButton: Button? = null

    @Setter
    private val removeCriteriaButtonListener: Runnable? = null

    override val root: Node?
        get() = logicalNodeRoot

    override fun initialize() {
        logicalOperatorField!!.setItems(FXCollections.observableArrayList(LogicalOperator.AND, LogicalOperator.OR))
        logicalOperatorField!!.selectionModel.select(0)
        logicalOperatorField!!.setConverter(object : StringConverter() {
            fun toString(`object`: LogicalOperator?): String {
                return if (`object` == null) {
                    ""
                } else i18n.get(`object`.i18nKey)
            }

            override fun fromString(string: String): LogicalOperator {
                throw UnsupportedOperationException("Not supported")
            }
        })
    }

    fun appendTo(condition: Condition<*>): Optional<Condition<*>> {
        val operator = logicalOperatorField!!.value ?: return Optional.empty()
        when (operator) {
            LogicalNodeController.LogicalOperator.AND -> return specificationController!!.appendTo(condition.and())
            LogicalNodeController.LogicalOperator.OR -> return specificationController!!.appendTo(condition.or())
            else -> throw ProgrammingError("Uncovered operator: $operator")
        }
    }

    fun setType(type: Class<*>) {
        specificationController!!.setRootType(type)
    }

    fun onRemoveCriteriaButtonClicked() {
        removeCriteriaButtonListener!!.run()
    }

    internal enum class LogicalOperator private constructor(private val i18nKey: String) {
        AND("query.and"),
        OR("query.or")
    }
}
