package com.faforever.client.patch

import com.faforever.client.game.FaInitGenerator
import com.faforever.client.game.KnownFeaturedMod
import com.faforever.client.mod.FeaturedMod
import com.faforever.client.mod.ModService
import com.faforever.client.preferences.ForgedAlliancePrefs
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.remote.FafService
import com.faforever.client.task.TaskService
import com.faforever.client.util.ProgrammingError
import com.faforever.commons.mod.MountInfo
import lombok.SneakyThrows
import lombok.extern.slf4j.Slf4j
import org.apache.maven.artifact.versioning.ComparableVersion
import org.springframework.context.ApplicationContext

import javax.inject.Inject
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.ArrayList
import java.util.Objects
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors
import java.util.stream.Stream

import com.faforever.client.game.KnownFeaturedMod.BALANCE_TESTING
import com.faforever.client.game.KnownFeaturedMod.FAF
import com.faforever.client.game.KnownFeaturedMod.FAF_BETA
import com.faforever.client.game.KnownFeaturedMod.FAF_DEVELOP
import com.faforever.client.game.KnownFeaturedMod.LADDER_1V1

@Slf4j
class GameUpdaterImpl @Inject
constructor(private val modService: ModService, private val applicationContext: ApplicationContext, private val taskService: TaskService,
            private val fafService: FafService, private val faInitGenerator: FaInitGenerator, private val preferencesService: PreferencesService) : GameUpdater {

    private val featuredModUpdaters: MutableList<FeaturedModUpdater>

    override val featuredMods: CompletableFuture<List<FeaturedMod>>
        get() = fafService.featuredMods

    init {
        featuredModUpdaters = ArrayList()
    }

    override fun addFeaturedModUpdater(featuredModUpdater: FeaturedModUpdater): GameUpdater {
        featuredModUpdaters.add(featuredModUpdater)
        return this
    }

    override fun update(featuredMod: FeaturedMod, version: Int?, featuredModVersions: Map<String, Int>, simModUids: Set<String>): CompletableFuture<Void> {
        // The following ugly code is sponsored by the featured-mod-mess. FAF and Coop are both featured mods - but others,
        // (except fafbeta and fafdevelop) implicitly depend on FAF. So if a non-base mod is being played, make sure FAF is
        // installed.
        val patchResults = ArrayList<PatchResult>()

        var future = updateFeaturedMod(featuredMod, version)
                .thenAccept(Consumer<PatchResult> { patchResults.add(it) })
                .thenCompose { s -> downloadMissingSimMods(simModUids) }

        if (!NAMES_OF_FEATURED_BASE_MODS.contains(featuredMod.technicalName)) {
            future = future.thenCompose { aVoid -> modService.getFeaturedMod(FAF.technicalName) }
                    .thenCompose { baseMod -> updateFeaturedMod(baseMod, null) }
                    .thenAccept(Consumer<PatchResult> { patchResults.add(it) })
        }

        verifyUniformModFormat(patchResults)

        return future
                .thenCompose { s -> updateGameBinaries(patchResults[patchResults.size - 1].getVersion()) }
                .thenRun {
                    if (patchResults.stream().noneMatch { patchResult -> patchResult.getLegacyInitFile() != null }) {
                        generateInitFile(patchResults)
                    } else {
                        val initFile = patchResults.stream()
                                .map(Function<PatchResult, Any> { getLegacyInitFile() })
                                .filter(Predicate<Any> { Objects.nonNull(it) })
                                .findFirst()
                                .orElseThrow({ ProgrammingError("No legacy init file is available") })

                        createFaPathLuaFile(initFile.getParent().getParent())
                        copyLegacyInitFile(initFile)
                    }
                }
    }

    @SneakyThrows
    private fun createFaPathLuaFile(parent: Path) {
        val path = preferencesService.preferences!!.forgedAlliance.path
        val content = String.format("fa_path = \"%s\"", path.toString().replace("\\", "/"))
        Files.write(parent.resolve("fa_path.lua"), content.toByteArray(StandardCharsets.UTF_8))
    }

    private fun generateInitFile(patchResults: List<PatchResult>) {
        val mountPoints = patchResults.stream()
                .flatMap<Any> { patchResult -> Optional.ofNullable(patchResult.getMountInfos()).orElseThrow({ ProgrammingError("No mount infos where available") }).stream() }
                .collect<List<MountInfo>, Any>(Collectors.toList<Any>())

        val hookDirectories = patchResults.stream()
                .flatMap<Any> { patchResult -> Optional.ofNullable(patchResult.getHookDirectories()).orElseThrow({ ProgrammingError("No mount infos where available") }).stream() }
                .collect<Set<String>, Any>(Collectors.toSet<Any>())

        faInitGenerator.generateInitFile(mountPoints, hookDirectories)
    }

    @SneakyThrows
    private fun copyLegacyInitFile(initFile: Path) {
        Files.copy(initFile, initFile.resolveSibling(ForgedAlliancePrefs.INIT_FILE_NAME), StandardCopyOption.REPLACE_EXISTING)
    }

    /**
     * Older versions of featured mods didn't provide a mod info file but provided an init file instead. New featured mod
     * versions provide an mod info file from which an init file is generated. Mixing these two kind of mods doesn't work,
     * which is what this method ensures.
     */
    private fun verifyUniformModFormat(patchResults: List<PatchResult>) {
        val modsWithLegacyInitFile = patchResults.stream().filter { patchResult -> patchResult.getLegacyInitFile() != null }.count()
        if (modsWithLegacyInitFile != 0L && modsWithLegacyInitFile != patchResults.size.toLong()) {
            throw IllegalStateException("Legacy and non-legacy mods can't be mixed.")
        }
    }

    private fun downloadMissingSimMods(simModUids: Set<String>?): CompletableFuture<Void> {
        if (simModUids == null || simModUids.isEmpty()) {
            return CompletableFuture.completedFuture(null)
        }

        val simModFutures = simModUids.stream()
                .filter { uid -> !modService.isModInstalled(uid) }
                .map<CompletableFuture<Void>>(Function<String, CompletableFuture<Void>> { modService.downloadAndInstallMod(it) })
                .collect<List<CompletableFuture<Void>>, Any>(Collectors.toList())
        return CompletableFuture.allOf(*simModFutures.toTypedArray<CompletableFuture>())
    }

    private fun updateFeaturedMod(featuredMod: FeaturedMod, version: Int?): CompletableFuture<PatchResult> {
        for (featuredModUpdater in featuredModUpdaters) {
            if (featuredModUpdater.canUpdate(featuredMod)) {
                return featuredModUpdater.updateMod(featuredMod, version)
            }
        }
        throw UnsupportedOperationException("No updater available for featured mod: " + featuredMod
                + " with version:" + version)
    }

    private fun updateGameBinaries(version: ComparableVersion): CompletableFuture<Void> {
        val binariesUpdateTask = applicationContext.getBean(GameBinariesUpdateTaskImpl::class.java)
        binariesUpdateTask.setVersion(version)
        return taskService.submitTask<GameBinariesUpdateTask>(binariesUpdateTask).future
    }

    companion object {

        private val NAMES_OF_FEATURED_BASE_MODS = Stream.of(FAF, FAF_BETA, FAF_DEVELOP, BALANCE_TESTING, LADDER_1V1)
                .map<String>(Function<KnownFeaturedMod, String> { it.getTechnicalName() })
                .collect<List<String>, Any>(Collectors.toList())
    }
}
