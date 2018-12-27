package com.faforever.client.patch


import com.faforever.commons.mod.MountInfo
import lombok.AccessLevel
import lombok.AllArgsConstructor
import lombok.Data
import org.apache.maven.artifact.versioning.ComparableVersion

import java.nio.file.Path

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class PatchResult {
    /** Only set if mod_info.lua was available.  */
    private val version: ComparableVersion? = null

    /** Only set if mod_info.lua was available.  */
    private val mountInfos: List<MountInfo>? = null

    /** Only set if mod_info.lua was available.  */
    private val hookDirectories: List<String>? = null

    /** Only set if no mod_info.lua was available.  */
    private val legacyInitFile: Path? = null

    companion object {

        fun fromModInfo(version: ComparableVersion, mountInfos: List<MountInfo>, hookDirectories: List<String>): PatchResult {
            return PatchResult(version, mountInfos, hookDirectories, null)
        }

        fun withLegacyInitFile(version: ComparableVersion, legacyInitFile: Path): PatchResult {
            return PatchResult(version, null, null, legacyInitFile)
        }
    }
}
