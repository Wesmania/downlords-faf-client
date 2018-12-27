package com.faforever.client.achievements;

import com.faforever.client.achievements.AchievementService.AchievementState;
import com.faforever.client.api.dto.AchievementDefinition;
import com.faforever.client.api.dto.AchievementType;
import com.faforever.client.api.dto.PlayerAchievement;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.faforever.client.theme.UiService.DEFAULT_ACHIEVEMENT_IMAGE;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

public class AchievementItemControllerTest extends AbstractPlainJavaFxTest {

  private AchievementItemController instance;

  @Mock
  private I18n i18n;
  @Mock
  private AchievementService achievementService;

  @Before
  public void setUp() throws Exception {
    instance = new AchievementItemController(i18n, achievementService);
    when(i18n.number(anyInt())).thenAnswer(invocation -> String.format("%d", (int) invocation.getArgument(0)));

    loadFxml("theme/achievement_item.fxml", clazz -> instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.getAchievementItemRoot()));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testSetAchievementDefinition() throws Exception {
    AchievementDefinition achievementDefinition = AchievementDefinitionBuilder.create().defaultValues().get();
    when(achievementService.getImage(achievementDefinition, AchievementState.REVEALED)).thenReturn(new Image(getThemeFile(Companion.getDEFAULT_ACHIEVEMENT_IMAGE())));

    instance.setAchievementDefinition(achievementDefinition);

    assertThat(instance.getNameLabel().getText(), is(achievementDefinition.getName()));
    assertThat(instance.getDescriptionLabel().getText(), is(achievementDefinition.getDescription()));
    assertThat(instance.getPointsLabel().getText(), is(String.format("%d", achievementDefinition.getExperiencePoints())));
    assertThat(instance.getImageView().getImage(), notNullValue());
    assertThat(instance.getImageView().getEffect(), is(instanceOf(ColorAdjust.class)));
    assertThat(instance.getImageView().getOpacity(), is(0.5));
    assertThat(instance.getProgressBar().isVisible(), is(true));
  }

  @Test
  public void testSetAchievementDefinitionStandardHasNoProgress() throws Exception {
    instance.setAchievementDefinition(AchievementDefinitionBuilder.create().defaultValues()
        .type(AchievementType.STANDARD)
        .get());

    assertThat(instance.getProgressBar().isVisible(), is(false));
    assertThat(instance.getProgressLabel().isVisible(), is(false));
  }

  @Test
  public void testSetPlayerAchievementStandardDoesntUpdateProgress() throws Exception {
    instance.setAchievementDefinition(AchievementDefinitionBuilder.create().defaultValues()
        .type(AchievementType.STANDARD)
        .get());

    PlayerAchievement playerAchievement = PlayerAchievementBuilder.create().defaultValues()
        .state(com.faforever.client.api.dto.AchievementState.UNLOCKED)
        .currentSteps(50)
        .get();

    instance.setPlayerAchievement(playerAchievement);

    assertThat(instance.getProgressBar().getProgress(), is(0.0));
  }

  @Test(expected = IllegalStateException.class)
  public void testSetPlayerAchievementWithUnsetAchievementThrowsIse() throws Exception {
    instance.setPlayerAchievement(new PlayerAchievement());
  }

  @Test(expected = IllegalStateException.class)
  public void testSetPlayerAchievementIdDoesntMatch() throws Exception {
    instance.setAchievementDefinition(AchievementDefinitionBuilder.create().defaultValues().get());

    PlayerAchievement playerAchievement = PlayerAchievementBuilder.create().defaultValues()
        .achievementId("foobar")
        .get();

    instance.setPlayerAchievement(playerAchievement);
  }

  @Test
  public void testSetPlayerAchievementRevealed() throws Exception {
    instance.setAchievementDefinition(AchievementDefinitionBuilder.create().defaultValues().get());

    PlayerAchievement playerAchievement = PlayerAchievementBuilder.create().defaultValues()
        .state(com.faforever.client.api.dto.AchievementState.REVEALED)
        .get();

    instance.setPlayerAchievement(playerAchievement);
    assertThat(instance.getImageView().getEffect(), is(instanceOf(ColorAdjust.class)));
    assertThat(instance.getImageView().getOpacity(), is(0.5));
  }

  @Test
  public void testSetPlayerAchievementUnlocked() throws Exception {
    instance.setAchievementDefinition(AchievementDefinitionBuilder.create().defaultValues().get());

    PlayerAchievement playerAchievement = PlayerAchievementBuilder.create().defaultValues()
        .state(com.faforever.client.api.dto.AchievementState.UNLOCKED)
        .currentSteps(50)
        .get();

    instance.setPlayerAchievement(playerAchievement);
    assertThat(instance.getImageView().getEffect(), is(nullValue()));
    assertThat(instance.getImageView().getOpacity(), is(1.0));
    assertThat(instance.getProgressBar().isVisible(), is(true));
    assertThat(instance.getProgressBar().getProgress(), is(0.5));
  }
}
