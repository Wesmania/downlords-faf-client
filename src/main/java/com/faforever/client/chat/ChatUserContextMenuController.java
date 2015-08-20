package com.faforever.client.chat;

import com.faforever.client.fx.SceneFactory;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.GameService;
import com.faforever.client.legacy.GameStatus;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.player.PlayerService;
import com.faforever.client.util.Callback;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.lang.invoke.MethodHandles;

import static com.faforever.client.fx.WindowDecorator.WindowButtonType.CLOSE;

public class ChatUserContextMenuController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @FXML
  MenuItem joinGameItem;

  @FXML
  MenuItem addFriendItem;

  @FXML
  MenuItem removeFriendItem;

  @FXML
  MenuItem addFoeItem;

  @FXML
  MenuItem removeFoeItem;

  @FXML
  MenuItem watchGameItem;

  @FXML
  MenuItem viewReplaysItem;

  @FXML
  MenuItem inviteItem;

  @FXML
  SeparatorMenuItem moderatorActionSeparator;

  @FXML
  MenuItem kickItem;

  @FXML
  MenuItem banItem;

  @FXML
  ContextMenu contextMenu;

  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  SceneFactory sceneFactory;

  @Autowired
  PlayerService playerService;

  @Autowired
  GameService gameService;

  private PlayerInfoBean playerInfoBean;

  public ContextMenu getContextMenu() {
    return contextMenu;
  }

  public void setPlayerInfoBean(PlayerInfoBean playerInfoBean) {
    this.playerInfoBean = playerInfoBean;

    addFriendItem.visibleProperty().bind(playerInfoBean.friendProperty().not());
    removeFriendItem.visibleProperty().bind(playerInfoBean.friendProperty());
    addFoeItem.visibleProperty().bind(playerInfoBean.foeProperty().not());
    removeFoeItem.visibleProperty().bind(playerInfoBean.foeProperty());

    joinGameItem.visibleProperty().bind(playerInfoBean.gameStatusProperty().isEqualTo(GameStatus.LOBBY).or(playerInfoBean.gameStatusProperty().isEqualTo(GameStatus.HOST)));
    watchGameItem.visibleProperty().bind(playerInfoBean.gameStatusProperty().isEqualTo(GameStatus.PLAYING));
    inviteItem.visibleProperty().bind(playerInfoBean.gameStatusProperty().isNotEqualTo(GameStatus.PLAYING));

  }

  @FXML
  void onUserInfo() {
    UserInfoWindowController userInfoWindowController = applicationContext.getBean(UserInfoWindowController.class);
    userInfoWindowController.setPlayerInfoBean(playerInfoBean);

    Stage userInfoWindow = new Stage(StageStyle.TRANSPARENT);
    userInfoWindow.initModality(Modality.NONE);
    userInfoWindow.initOwner(contextMenu.getOwnerWindow());

    sceneFactory.createScene(userInfoWindow, userInfoWindowController.getRoot(), true, CLOSE);

    userInfoWindow.show();
  }

  @FXML
  void onSendPrivateMessage() {
  }

  @FXML
  void onAddFriend() {
    playerService.addFriend(playerInfoBean.getUsername());
  }

  @FXML
  void onRemoveFriend() {
    playerService.removeFriend(playerInfoBean.getUsername());
  }

  @FXML
  void onAddFoe() {
    playerService.addFoe(playerInfoBean.getUsername());
  }

  @FXML
  void onRemoveFoe() {
    playerService.removeFoe(playerInfoBean.getUsername());
  }

  @FXML
  void onWatchGame() {
    // FIXME implement
  }

  @FXML
  void onViewReplays() {
    // FIXME implement
  }

  @FXML
  void onInviteToGame() {
    //FIXME implement
  }

  @FXML
  void onKick() {
    // FIXME implement
  }

  @FXML
  void onBan() {
    // FIXME implement
  }

  @FXML
  void onJoinGame() {
    GameInfoBean gameInfoBean = gameService.getByUid(playerInfoBean.getGameUID());
    gameService.joinGame(gameInfoBean,null,new Callback<Void>() {
      @Override
      public void success(Void result) {
        // Cool.
      }

      @Override
      public void error(Throwable e) {
        // FIXME implement
        logger.warn("Game could not be joined", e);
      }
    });
  }
}
