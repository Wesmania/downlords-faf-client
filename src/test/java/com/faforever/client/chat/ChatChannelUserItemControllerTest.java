package com.faforever.client.chat;

import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.clan.Clan;
import com.faforever.client.clan.ClanService;
import com.faforever.client.clan.ClanTooltipController;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.google.common.eventbus.EventBus;
import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatChannelUserItemControllerTest extends AbstractPlainJavaFxTest {

  private ChatUserItemController instance;
  @Mock
  private AvatarService avatarService;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private EventBus eventBus;
  @Mock
  private ClanService clanService;
  @Mock
  private PlayerService playerService;
  @Mock
  private PlatformService platformService;
  @Mock
  private TimeService timeService;
  private ClanTooltipController clanTooltipControllerMock;
  private Clan testClan;

  @Before
  public void setUp() throws Exception {
    Preferences preferences = new Preferences();
    when(preferencesService.getPreferences()).thenReturn(preferences);

    when(i18n.get(eq("user.status.hosting"), anyString())).thenReturn("Hosting");
    when(i18n.get(eq("user.status.waiting"), anyString())).thenReturn("Waiting");
    when(i18n.get(eq("user.status.playing"), anyString())).thenReturn("Playing");
    testClan = new Clan();
    testClan.setTag("e");
    testClan.setLeader(PlayerBuilder.create("test_player").defaultValues().get());
    when(clanService.getClanByTag(anyString())).thenReturn(CompletableFuture.completedFuture(Optional.of(testClan)));
    when(countryFlagService.loadCountryFlag("US")).thenReturn(Optional.of(mock(Image.class)));
    when(playerService.isOnline(anyInt())).thenReturn(false);
    clanTooltipControllerMock = mock(ClanTooltipController.class);
    when(uiService.loadFxml("theme/chat/clan_tooltip.fxml")).thenReturn(clanTooltipControllerMock);
    when(clanTooltipControllerMock.getRoot()).thenReturn(new Pane());

    instance = new ChatUserItemController(preferencesService, avatarService, countryFlagService, i18n, uiService, eventBus, clanService, playerService, platformService, timeService);
    loadFxml("theme/chat/chat_user_item.fxml", param -> instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.getRoot()));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testSetChatUser() throws Exception {
    Player player = PlayerBuilder.create("junit")
        .defaultValues()
        .clan("e")
        .avatar(new AvatarBean(new URL("http://example.com/avatar.png"), "dog"))
        .get();
    instance.setChatUser(ChatChannelUserBuilder.create("junit").defaultValues().setPlayer(player).get());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getClanMenu().getText(), is("[e]"));
    assertThat(instance.getClanMenu().isVisible(), is(true));
    verify(countryFlagService).loadCountryFlag("US");
    assertThat(instance.getCountryTooltip(), CoreMatchers.notNullValue());
    assertThat(instance.getAvatarTooltip(), CoreMatchers.notNullValue());
    assertThat(instance.getUserTooltip(), CoreMatchers.notNullValue());
    verify(clanTooltipControllerMock).setClan(testClan);
  }

  @Test
  public void testGetPlayer() {
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit").defaultValues().get();
    instance.setChatUser(chatUser);

    assertThat(instance.getChatUser(), is(chatUser));
  }

  @Test
  public void testOpenGameSetsStatusToWaiting() {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .setPlayer(player)
        .get();
    instance.setChatUser(chatUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getStatusLabel().getText(), is(""));

    player.setGame(GameBuilder.create().defaultValues().get());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getStatusLabel().getText(), is("Waiting"));
  }

  @Test
  public void testHostedGameSetsStatusToHosting() {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .setPlayer(player)
        .get();

    instance.setChatUser(chatUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getStatusLabel().getText(), is(""));

    player.setGame(GameBuilder.create().defaultValues().host("junit").state(GameStatus.OPEN).get());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getStatusLabel().getText(), is("Hosting"));
  }

  @Test
  public void testActiveGameSetsStatusToPlaying() {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .setPlayer(player)
        .get();
    instance.setChatUser(chatUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getStatusLabel().getText(), is(""));

    player.setGame(GameBuilder.create().defaultValues().host("junit").state(GameStatus.PLAYING).get());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getStatusLabel().getText(), is("Playing"));
  }

  @Test
  public void testNullGameSetsStatusToNothing() {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .setPlayer(player)
        .get();
    instance.setChatUser(chatUser);
    player.setGame(GameBuilder.create().defaultValues().host("junit").state(GameStatus.PLAYING).get());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getStatusLabel().getText(), is("Playing"));
    player.setGame(null);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getStatusLabel().getText(), is(""));
  }

  @Test
  public void testSingleClickDoesNotInitiatePrivateChat() {
    instance.onItemClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 1));

    verify(eventBus, never()).post(CoreMatchers.any(InitiatePrivateChatEvent.class));
  }

  @Test
  public void testDoubleClickInitiatesPrivateChat() {
    instance.setChatUser(ChatChannelUserBuilder.create("junit").defaultValues().get());
    WaitForAsyncUtils.waitForFxEvents();

    instance.onItemClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 2));

    ArgumentCaptor<InitiatePrivateChatEvent> captor = ArgumentCaptor.forClass(InitiatePrivateChatEvent.class);
    verify(eventBus).post(captor.capture());

    assertThat(captor.getValue().getUsername(), is("junit"));
  }

  @Test
  public void testOnContextMenuRequested() {
    WaitForAsyncUtils.asyncFx(() -> getRoot().getChildren().setAll(instance.getRoot()));

    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit").defaultValues().get();
    instance.setChatUser(chatUser);
    WaitForAsyncUtils.waitForFxEvents();

    ChatUserContextMenuController contextMenuController = mock(ChatUserContextMenuController.class);
    ContextMenu contextMenu = mock(ContextMenu.class);
    when(contextMenuController.getContextMenu()).thenReturn(contextMenu);
    when(uiService.loadFxml("theme/chat/chat_user_context_menu.fxml")).thenReturn(contextMenuController);

    ContextMenuEvent event = mock(ContextMenuEvent.class);
    instance.onContextMenuRequested(event);

    verify(uiService).loadFxml("theme/chat/chat_user_context_menu.fxml");
    verify(contextMenuController).setChatUser(chatUser);
    verify(contextMenu).show(any(Pane.class), anyDouble(), anyDouble());
  }

  @Test
  public void testSetVisible() {
    instance.setVisible(true);
    assertThat(instance.getRoot().isVisible(), is(true));
    assertThat(instance.getRoot().isManaged(), is(true));

    instance.setVisible(false);
    assertThat(instance.getRoot().isVisible(), is(false));
    assertThat(instance.getRoot().isManaged(), is(false));

    instance.setVisible(true);
    assertThat(instance.getRoot().isVisible(), is(true));
    assertThat(instance.getRoot().isManaged(), is(true));
  }
}
