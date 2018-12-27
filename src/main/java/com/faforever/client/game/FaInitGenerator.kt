package com.faforever.client.game

import com.faforever.client.preferences.ForgedAlliancePrefs
import com.faforever.client.preferences.PreferencesService
import com.faforever.commons.mod.MountInfo
import com.google.common.base.Joiner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.lang.invoke.MethodHandles
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import java.util.stream.Collectors

import com.github.nocatch.NoCatch.noCatch
import java.nio.charset.StandardCharsets.UTF_8

/**
 * Generates the Forged Alliance init file that is required to mount all necessary files and directories.
 */

@Lazy
@Component
class FaInitGenerator @Inject
constructor(private val preferencesService: PreferencesService) {

    /**
     * Generates the Forged Alliance init file.
     *
     * @return the path of the generated init file.
     */
    fun generateInitFile(mountInfos: List<MountInfo>, hookDirectories: Set<String>): Path {
        val initFile = preferencesService.fafBinDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME)
        val faPath = preferencesService.preferences!!.forgedAlliance.path.toAbsolutePath().toString().replace("[/\\\\]".toRegex(), "\\\\\\\\")

        logger.debug("Generating init file at {}", initFile)

        val mountPointStrings = mountInfos.stream()
                .map<String>(Function<MountInfo, String> { this.toMountPointFormat(it) })
                .collect<List<String>, Any>(Collectors.toList())
        val mountPointsLuaTable = Joiner.on(",\r\n    ").join(mountPointStrings)

        val hookStrings = hookDirectories.stream()
                .sorted()
                .map { s -> "'$s'" }
                .collect<List<String>, Any>(Collectors.toList())
        val hooksLuaTable = Joiner.on(",\r\n    ").join(hookStrings)

        noCatch {
            Files.createDirectories(initFile.parent)
            BufferedReader(InputStreamReader(INIT_TEMPLATE.inputStream)).use { reader ->
                Files.newBufferedWriter(initFile, UTF_8).use { writer ->
                    var line: String
                    while ((line = reader.readLine()) != null) {
                        line = line.replace("((fa_path))", faPath)
                        writer.write(line.replace("--[[ \${mountSpecsTable} --]]", mountPointsLuaTable)
                                .replace("--[[ \${hooksTable} --]]", hooksLuaTable) + "\r\n")
                    }
                }
            }
        }
        return initFile
    }

    private fun toMountPointFormat(mountInfo: MountInfo): String {
        val source = Optional.ofNullable(mountInfo.baseDir)
                .map { path -> path.resolve(mountInfo.file).toAbsolutePath().toString() }
                .orElse(mountInfo.file.toAbsolutePath().toString())
                .replace("[/\\\\]".toRegex(), "\\\\\\\\")

        return String.format("{'%s', '%s'}", source, mountInfo.mountPoint)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        private val INIT_TEMPLATE = ClassPathResource("/fa/init_template.lua")
    }
}
