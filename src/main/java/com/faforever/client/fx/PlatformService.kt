package com.faforever.client.fx

import com.sun.jna.Platform
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.platform.win32.WinUser.WINDOWPLACEMENT
import javafx.application.HostServices

import java.nio.file.Path

import com.github.nocatch.NoCatch.noCatch
import org.bridj.Platform.show

class PlatformService(private val hostServices: HostServices) {

    private val isWindows: Boolean

    private val foregroundWindowTitle: String?
        get() {
            if (!isWindows) {
                return null
            }

            val window = User32.INSTANCE.GetForegroundWindow() ?: return null

            val textBuffer = CharArray(255)
            User32.INSTANCE.GetWindowText(window, textBuffer, 255)
            return String(textBuffer).trim { it <= ' ' }
        }

    init {
        isWindows = Platform.isWindows()
    }

    /**
     * Opens the specified URI in a new browser window or tab.
     */

    fun showDocument(url: String) {
        hostServices.showDocument(url)
    }

    /**
     * Show a file in its parent directory, if possible selecting the file (not possible on all platforms).
     */

    fun reveal(path: Path) {
        noCatch { show(path.toFile()) }
    }


    /**
     * Show a Window, restore it to it's state before minimizing (normal/restored or maximized) and move it to foreground
     * will only work on windows systems
     */

    fun focusWindow(windowTitle: String) {
        if (!isWindows) {
            return
        }

        val user32 = User32.INSTANCE
        val window = user32.FindWindow(null, windowTitle)

        // Does only set the window to visible, does not restore/bring it to foreground
        user32.ShowWindow(window, User32.SW_SHOW)

        val windowplacement = WINDOWPLACEMENT()
        user32.GetWindowPlacement(window, windowplacement)

        if (windowplacement.showCmd == User32.SW_SHOWMINIMIZED) {
            // Bit 2 in flags (bitmask 0x2) signals that window should be maximized when restoring
            if (windowplacement.flags and WINDOWPLACEMENT.WPF_RESTORETOMAXIMIZED == WINDOWPLACEMENT.WPF_RESTORETOMAXIMIZED) {
                user32.ShowWindow(window, User32.SW_SHOWMAXIMIZED)
            } else {
                user32.ShowWindow(window, User32.SW_SHOWNORMAL)
            }
        }

        val foregroundWindowTitle = foregroundWindowTitle
        if (foregroundWindowTitle == null || foregroundWindowTitle != windowTitle.trim { it <= ' ' }) {
            user32.SetForegroundWindow(window)
        }
    }


    fun startFlashingWindow(windowTitle: String) {
        if (!isWindows) {
            return
        }

        val window = User32.INSTANCE.FindWindow(null, windowTitle)

        val flashwinfo = WinUser.FLASHWINFO()
        flashwinfo.hWnd = window
        flashwinfo.dwFlags = WinUser.FLASHW_TRAY
        flashwinfo.uCount = Integer.MAX_VALUE
        flashwinfo.dwTimeout = 500
        flashwinfo.cbSize = flashwinfo.size()

        User32.INSTANCE.FlashWindowEx(flashwinfo)
    }


    fun stopFlashingWindow(windowTitle: String) {
        if (!isWindows) {
            return
        }

        val window = User32.INSTANCE.FindWindow(null, windowTitle)

        val flashwinfo = WinUser.FLASHWINFO()
        flashwinfo.hWnd = window
        flashwinfo.dwFlags = WinUser.FLASHW_STOP
        flashwinfo.cbSize = flashwinfo.size()

        User32.INSTANCE.FlashWindowEx(flashwinfo)
    }


    fun isWindowFocused(windowTitle: String): Boolean {
        return windowTitle == foregroundWindowTitle
    }
}
