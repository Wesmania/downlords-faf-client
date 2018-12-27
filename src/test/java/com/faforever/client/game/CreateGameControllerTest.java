package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapBuilder;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.ModService;
import com.faforever.client.mod.ModVersion;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateGameControllerTest extends AbstractPlainJavaFxTest {


  private static final KeyEvent keyUpPressed = new KeyEvent(KeyEvent.KEY_PRESSED, "0", "", KeyCode.UP, false, false, false, false);
  private static final KeyEvent keyUpReleased = new KeyEvent(KeyEvent.KEY_RELEASED, "0", "", KeyCode.UP, false, false, false, false);
  private static final KeyEvent keyDownPressed = new KeyEvent(KeyEvent.KEY_PRESSED, "0", "", KeyCode.DOWN, false, false, false, false);
  private static final KeyEvent keyDownReleased = new KeyEvent(KeyEvent.KEY_RELEASED, "0", "", KeyCode.DOWN, false, false, false, false);

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private MapService mapService;
  @Mock
  private ModService modService;
  @Mock
  private
  GameService gameService;
  @Mock
  private
  NotificationService notificationService;
  @Mock
  private
  ReportingService reportingService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private FafService fafService;

  private Preferences preferences;
  private CreateGameController instance;
  private ObservableList<MapBean> mapList;

  @Before
  public void setUp() throws Exception {
    instance = new CreateGameController(fafService, mapService, modService, gameService, preferencesService, i18n, notificationService, reportingService);

    mapList = FXCollections.observableArrayList();

    preferences = new Preferences();
    preferences.getForgedAlliance().setPath(Paths.get("."));
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(mapService.getInstalledMaps()).thenReturn(mapList);
    when(modService.getFeaturedMods()).thenReturn(CompletableFuture.completedFuture(emptyList()));
    when(modService.getInstalledModVersions()).thenReturn(FXCollections.observableList(emptyList()));
    when(mapService.loadPreview(anyString(), any())).thenReturn(new Image("/theme/images/close.png"));
    when(fafService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>(ConnectionState.CONNECTED));

    loadFxml("theme/play/create_game.fxml", clazz -> instance);
  }

  @Test
  public void testMapSearchTextFieldFilteringEmpty() {
    instance.getMapSearchTextField().setText("Test");

    assertThat(instance.getFilteredMapBeans().getSource(), empty());
  }

  @Test
  public void testMapSearchTextFieldFilteringPopulated() {
    mapList.add(MapBuilder.create().defaultValues().displayName("Test1").get());
    mapList.add(MapBuilder.create().defaultValues().folderName("test2").get());
    mapList.add(MapBuilder.create().defaultValues().displayName("foo").get());

    instance.getMapSearchTextField().setText("Test");

    assertThat(instance.getFilteredMapBeans().get(0).getFolderName(), is("test2"));
    assertThat(instance.getFilteredMapBeans().get(1).getDisplayName(), is("Test1"));
  }

  @Test
  public void testMapSearchTextFieldKeyPressedUpForEmpty() {
    instance.getMapSearchTextField().setText("Test");

    instance.getMapSearchTextField().getOnKeyPressed().handle(keyUpPressed);
    instance.getMapSearchTextField().getOnKeyPressed().handle(keyUpReleased);

    assertThat(instance.getMapListView().getSelectionModel().getSelectedIndex(), is(-1));
  }

  @Test
  public void testMapSearchTextFieldKeyPressedDownForEmpty() {
    instance.getMapSearchTextField().setText("Test");

    instance.getMapSearchTextField().getOnKeyPressed().handle(keyDownPressed);
    instance.getMapSearchTextField().getOnKeyPressed().handle(keyDownReleased);

    assertThat(instance.getMapListView().getSelectionModel().getSelectedIndex(), is(-1));
  }

  @Test
  public void testMapSearchTextFieldKeyPressedUpForPopulated() {
    mapList.add(MapBuilder.create().defaultValues().displayName("Test1").get());
    mapList.add(MapBuilder.create().defaultValues().displayName("Test1").get());
    instance.getMapSearchTextField().setText("Test");

    instance.getMapSearchTextField().getOnKeyPressed().handle(keyDownPressed);
    instance.getMapSearchTextField().getOnKeyPressed().handle(keyDownReleased);
    instance.getMapSearchTextField().getOnKeyPressed().handle(keyUpPressed);
    instance.getMapSearchTextField().getOnKeyPressed().handle(keyUpReleased);

    assertThat(instance.getMapListView().getSelectionModel().getSelectedIndex(), is(0));
  }

  @Test
  public void testMapSearchTextFieldKeyPressedDownForPopulated() {
    mapList.add(MapBuilder.create().defaultValues().displayName("Test1").get());
    mapList.add(MapBuilder.create().defaultValues().displayName("Test1").get());
    instance.getMapSearchTextField().setText("Test");

    instance.getMapSearchTextField().getOnKeyPressed().handle(keyDownPressed);
    instance.getMapSearchTextField().getOnKeyPressed().handle(keyDownReleased);

    assertThat(instance.getMapListView().getSelectionModel().getSelectedIndex(), is(1));
  }

  @Test
  public void testSetLastGameTitle() {
    preferences.setLastGameTitle("testGame");
    preferences.getForgedAlliance().setPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getTitleTextField().getText(), is("testGame"));
  }


  @Test
  public void testButtonBindingIfFeaturedModNotSet() {
    preferences.setLastGameTitle("123");
    when(i18n.get("game.create.featuredModMissing")).thenReturn("Mod missing");
    preferences.getForgedAlliance().setPath(Paths.get(""));
    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getTitleTextField().getText(), is("123"));
    assertThat(instance.getCreateGameButton().getText(), is("Mod missing"));
  }

  @Test
  public void testButtonBindingIfTitleNotSet() {

    when(i18n.get("game.create.titleMissing")).thenReturn("title missing");
    preferences.getForgedAlliance().setPath(Paths.get(""));
    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getTitleTextField().getText(), is(""));
    assertThat(instance.getCreateGameButton().getText(), is("title missing"));
  }

  @Test
  public void testButtonBindingIfNotConnected() {
    when(fafService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>(ConnectionState.DISCONNECTED));
    when(i18n.get("game.create.disconnected")).thenReturn("disconnected");
    preferences.getForgedAlliance().setPath(Paths.get(""));
    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getTitleTextField().getText(), is(""));
    assertThat(instance.getCreateGameButton().getText(), is("disconnected"));
  }

  @Test
  public void testButtonBindingIfNotConnecting() {
    when(fafService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>(ConnectionState.CONNECTING));
    when(i18n.get("game.create.connecting")).thenReturn("connecting");
    preferences.getForgedAlliance().setPath(Paths.get(""));
    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getTitleTextField().getText(), is(""));
    assertThat(instance.getCreateGameButton().getText(), is("connecting"));
  }

  @Test
  public void testSelectLastMap() {
    MapBean lastMapBean = MapBuilder.create().defaultValues().folderName("foo").get();
    preferences.setLastMap("foo");

    mapList.add(MapBuilder.create().defaultValues().folderName("Test1").get());
    mapList.add(lastMapBean);

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getMapListView().getSelectionModel().getSelectedItem(), is(lastMapBean));
  }

  @Test
  public void testInitGameTypeComboBoxEmpty() throws Exception {
    instance = new CreateGameController(fafService, mapService, modService, gameService, preferencesService, i18n, notificationService, reportingService);
    loadFxml("theme/play/create_game.fxml", clazz -> instance);

    assertThat(instance.getFeaturedModListView().getItems(), empty());
  }

  @Test
  public void testInitGameTypeComboBoxPostPopulated() {
    FeaturedMod featuredMod = FeaturedModBeanBuilder.create().defaultValues().get();
    when(modService.getFeaturedMods()).thenReturn(completedFuture(singletonList(featuredMod)));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getFeaturedModListView().getItems(), hasSize(1));
    assertThat(instance.getFeaturedModListView().getItems().get(0), is(featuredMod));
  }

  @Test
  public void testSelectLastOrDefaultSelectDefault() {
    FeaturedMod featuredMod = FeaturedModBeanBuilder.create().defaultValues().technicalName("something").get();
    FeaturedMod featuredMod2 = FeaturedModBeanBuilder.create().defaultValues().technicalName(KnownFeaturedMod.Companion.getDEFAULT().getTechnicalName()).get();

    preferences.setLastGameType(null);
    when(modService.getFeaturedMods()).thenReturn(completedFuture(asList(featuredMod, featuredMod2)));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getFeaturedModListView().getSelectionModel().getSelectedItem(), is(featuredMod2));
  }

  @Test
  public void testSelectLastOrDefaultSelectLast() {
    FeaturedMod featuredMod = FeaturedModBeanBuilder.create().defaultValues().technicalName("last").get();
    FeaturedMod featuredMod2 = FeaturedModBeanBuilder.create().defaultValues().technicalName(KnownFeaturedMod.Companion.getDEFAULT().getTechnicalName()).get();

    preferences.setLastGameType("last");
    when(modService.getFeaturedMods()).thenReturn(completedFuture(asList(featuredMod, featuredMod2)));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getFeaturedModListView().getSelectionModel().getSelectedItem(), is(featuredMod));
  }

  @Test
  public void testInitModListPopulated() {
    assertThat(instance.getModListView().getItems(), empty());

    ModVersion modVersion1 = mock(ModVersion.class);
    ModVersion modVersion2 = mock(ModVersion.class);

    when(modService.getInstalledModVersions()).thenReturn(FXCollections.observableArrayList(
        modVersion1, modVersion2
    ));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getModListView().getItems(), hasSize(2));
    assertThat(instance.getModListView().getItems(), contains(modVersion1, modVersion2));
  }

  @Test
  public void testOnlyFriendsBinding() {
    instance.getOnlyForFriendsCheckBox().setSelected(true);
    assertThat(preferences.isLastGameOnlyFriends(), is(true));
    instance.getOnlyForFriendsCheckBox().setSelected(false);
    assertThat(preferences.isLastGameOnlyFriends(), is(false));
  }
}
