package com.faforever.client.vault.search

import com.faforever.client.fx.Controller
import com.faforever.client.i18n.I18n
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.query.LogicalNodeController
import com.faforever.client.query.SpecificationController
import com.faforever.client.theme.UiService
import com.github.rutledgepaulv.qbuilders.builders.QBuilder
import com.github.rutledgepaulv.qbuilders.conditions.Condition
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor
import javafx.beans.InvalidationListener
import javafx.beans.property.ObjectProperty
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.util.StringConverter
import lombok.AllArgsConstructor
import lombok.Data
import lombok.Getter
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.util.ArrayList
import java.util.Optional
import java.util.function.Consumer

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class SearchController(private val uiService: UiService, private val i18n: I18n, private val preferencesService: PreferencesService) : Controller<Pane> {
    /**
     * The first query element.
     */
    var initialLogicalNodeController: LogicalNodeController? = null
    var criteriaPane: Pane? = null
    var queryTextField: TextField? = null
    var displayQueryCheckBox: CheckBox? = null
    var searchButton: Button? = null
    override var root: Pane? = null
    var sortPropertyComboBox: ComboBox<String>? = null
    var sortOrderChoiceBox: ComboBox<SortOrder>? = null
    var sortBox: HBox? = null

    private val queryNodes: MutableList<LogicalNodeController>
    private var queryInvalidationListener: InvalidationListener? = null
    /**
     * Called with the query string when the user hits "search".
     */
    private var searchListener: Consumer<SearchConfig>? = null
    private var searchableProperties: Map<String, String>? = null
    /**
     * Type of the searchable entity.
     */
    private var rootType: Class<*>? = null

    private val currentEntityKey: String
        get() = searchableProperties!!.entries.stream()
                .filter { stringStringEntry -> stringStringEntry.value == sortPropertyComboBox!!.value }
                .findFirst()
                .get()
                .key

    init {
        queryNodes = ArrayList()
    }

    override fun initialize() {
        queryTextField!!.managedProperty().bind(queryTextField!!.visibleProperty())
        queryTextField!!.visibleProperty().bind(displayQueryCheckBox!!.selectedProperty())

        searchButton!!.disableProperty().bind(queryTextField!!.textProperty().isEmpty)

        initialLogicalNodeController!!.logicalOperatorField!!.managedProperty()
                .bind(initialLogicalNodeController!!.logicalOperatorField!!.visibleProperty())
        initialLogicalNodeController!!.removeCriteriaButton!!.managedProperty()
                .bind(initialLogicalNodeController!!.removeCriteriaButton!!.visibleProperty())

        initialLogicalNodeController!!.logicalOperatorField!!.setValue(null)
        initialLogicalNodeController!!.logicalOperatorField!!.isDisable = true
        initialLogicalNodeController!!.logicalOperatorField!!.isVisible = false
        initialLogicalNodeController!!.removeCriteriaButton!!.isVisible = false

        queryInvalidationListener = { observable -> queryTextField!!.text = buildQuery(initialLogicalNodeController!!.specificationController!!, queryNodes) }
        addInvalidationListener(initialLogicalNodeController!!)
        initSorting()
    }

    private fun initSorting() {
        sortPropertyComboBox!!.setConverter(object : StringConverter<String>() {
            override fun toString(name: String): String {
                return i18n.get(name)
            }

            override fun fromString(string: String): String {
                throw UnsupportedOperationException("Not supported")
            }
        })
        sortOrderChoiceBox!!.setConverter(object : StringConverter<SortOrder>() {
            override fun toString(order: SortOrder): String {
                return i18n.get(order.getI18nKey())
            }

            override fun fromString(string: String): SortOrder {
                throw UnsupportedOperationException("Not supported")
            }
        })
        sortOrderChoiceBox!!.items.addAll(*SortOrder.values())
    }

    fun setSearchableProperties(searchableProperties: Map<String, String>) {
        this.searchableProperties = searchableProperties
        initialLogicalNodeController!!.specificationController!!.setProperties(searchableProperties)
    }

    fun setSortConfig(sortConfigObjectProperty: ObjectProperty<SortConfig>) {
        sortPropertyComboBox!!.items.addAll(searchableProperties!!.values)
        sortOrderChoiceBox!!.selectionModel.select(sortConfigObjectProperty.get().getSortOrder())
        sortPropertyComboBox!!.selectionModel.select(searchableProperties!![sortConfigObjectProperty.get().getSortProperty()])
        sortPropertyComboBox!!.valueProperty().addListener { observable, oldValue, newValue ->
            sortConfigObjectProperty.set(SortConfig(currentEntityKey, sortOrderChoiceBox!!.value))
            preferencesService.storeInBackground()
        }
        sortOrderChoiceBox!!.valueProperty().addListener { observable, oldValue, newValue ->
            sortConfigObjectProperty.set(SortConfig(currentEntityKey, newValue))
            preferencesService.storeInBackground()
        }
    }

    private fun addInvalidationListener(logicalNodeController: LogicalNodeController) {
        logicalNodeController.specificationController!!.propertyField!!.valueProperty().addListener(queryInvalidationListener)
        logicalNodeController.specificationController!!.operationField!!.valueProperty().addListener(queryInvalidationListener)
        logicalNodeController.specificationController!!.valueField!!.valueProperty().addListener(queryInvalidationListener)
        logicalNodeController.specificationController!!.valueField!!.editor.textProperty().addListener { observable ->
            if (!logicalNodeController.specificationController!!.valueField!!.valueProperty().isBound) {
                logicalNodeController.specificationController!!.valueField!!.value = logicalNodeController.specificationController!!.valueField!!.editor.text
            }
        }
        logicalNodeController.specificationController!!.valueField!!.setOnKeyReleased { event ->
            if (event.code == KeyCode.ENTER) {
                searchButton!!.fire()
            }
        }
    }

    fun onSearchButtonClicked() {
        val sortPropertyKey = currentEntityKey
        searchListener!!.accept(SearchConfig(SortConfig(sortPropertyKey, sortOrderChoiceBox!!.value), queryTextField!!.text))
    }

    fun onAddCriteriaButtonClicked() {
        val controller = uiService.loadFxml<LogicalNodeController>("theme/vault/search/logical_node.fxml")
        controller.logicalOperatorField!!.valueProperty().addListener(queryInvalidationListener)
        controller.specificationController!!.setRootType(rootType)
        controller.specificationController!!.setProperties(searchableProperties)
        controller.setRemoveCriteriaButtonListener {
            criteriaPane!!.children.remove(controller.root)
            queryNodes.remove(controller)
            if (queryNodes.isEmpty()) {
                initialLogicalNodeController!!.logicalOperatorField!!.isVisible = false
            }
        }
        addInvalidationListener(controller)

        criteriaPane!!.children.add(controller.root)
        queryNodes.add(controller)
        initialLogicalNodeController!!.logicalOperatorField!!.isVisible = true
    }

    fun onResetButtonClicked() {
        ArrayList(queryNodes).forEach { logicalNodeController -> logicalNodeController.removeCriteriaButton!!.fire() }
        initialLogicalNodeController!!.specificationController!!.propertyField!!.selectionModel.select(0)
        initialLogicalNodeController!!.specificationController!!.operationField!!.selectionModel.select(0)
        initialLogicalNodeController!!.specificationController!!.valueField!!.value = null
    }

    /**
     * Builds the query string if possible, returns empty string if not. A query string can not be built if the user
     * selected no or invalid values.
     */
    private fun buildQuery(initialSpecification: SpecificationController, queryNodes: List<LogicalNodeController>): String {
        val qBuilder = QBuilder()

        var condition: Optional<Condition<*>> = initialSpecification.appendTo(qBuilder)
        if (!condition.isPresent) {
            return ""
        }
        for (queryNode in queryNodes) {
            val currentCondition = queryNode.appendTo(condition.get())
            if (!currentCondition.isPresent) {
                break
            }
            condition = currentCondition
        }
        return condition.get().query(RSQLVisitor()) as String
    }

    fun setSearchListener(searchListener: Consumer<SearchConfig>) {
        this.searchListener = searchListener
    }

    fun setRootType(rootType: Class<*>) {
        this.rootType = rootType
        initialLogicalNodeController!!.specificationController!!.setRootType(rootType)
    }

    @Getter
    enum class SortOrder private constructor(private val query: String, private val i18nKey: String) {
        DESC("-", "search.sort.descending"),
        ASC("", "search.sort.ascending")
    }

    @Data
    @AllArgsConstructor
    class SortConfig {
        private val sortProperty: String? = null
        private val sortOrder: SortOrder? = null

        fun toQuery(): String {
            return sortOrder!!.getQuery() + sortProperty!!
        }
    }

    @Data
    @AllArgsConstructor
    class SearchConfig {
        private val sortConfig: SortConfig? = null
        private val searchQuery: String? = null

        fun hasQuery(): Boolean {
            return searchQuery != null && !searchQuery.isEmpty()
        }
    }
}
