package com.jitelecom.productadviser.data.seed

import com.jitelecom.productadviser.data.local.RequirementEntity
import com.jitelecom.productadviser.data.local.SoftwareEntity
import com.jitelecom.productadviser.domain.model.RequirementType
import com.jitelecom.productadviser.domain.model.VerificationStatus

data class SoftwareRequirementTemplate(
    val ramGB: Int? = null,
    val storageGB: Int? = null,
    val cpuTier: Int? = null,
    val gpuTier: Int? = null,
    val vramGB: Double? = null,
    val operatingSystems: Set<String>,
    val notes: String,
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
    val platform: String = ""
) {
    fun entity(softwareId: Long, type: RequirementType, id: Long = 0) = RequirementEntity(
        id = id,
        softwareId = softwareId,
        type = type,
        minimumRamGB = ramGB,
        minimumStorageGB = storageGB,
        minimumCpuTier = cpuTier,
        minimumGpuTier = gpuTier,
        minimumVramGB = vramGB,
        supportedOperatingSystems = operatingSystems,
        notes = notes,
        verificationStatus = verificationStatus,
        platform = platform
    )
}

data class BundledSoftwareDefinition(
    val name: String,
    val version: String,
    val category: String,
    val developer: String,
    val platform: String,
    val description: String,
    val officialWebsite: String,
    val requirementsSourceUrl: String,
    val minimum: SoftwareRequirementTemplate,
    val recommended: SoftwareRequirementTemplate? = null
) {
    fun entity(id: Long = 0) = SoftwareEntity(
        id = id,
        name = name,
        developer = developer,
        version = version,
        category = category,
        platform = platform,
        description = description,
        officialWebsite = officialWebsite,
        requirementsSourceUrl = requirementsSourceUrl,
        lastVerified = "2026-09-26",
        verificationStatus = VerificationStatus.VERIFIED
    )

    fun requirements(softwareId: Long, existing: List<RequirementEntity> = emptyList()): List<RequirementEntity> =
        listOfNotNull(
            minimum.entity(softwareId, RequirementType.MINIMUM, existing.firstOrNull { it.type == RequirementType.MINIMUM && it.platform == minimum.platform }?.id ?: 0),
            recommended?.entity(softwareId, RequirementType.RECOMMENDED, existing.firstOrNull { it.type == RequirementType.RECOMMENDED && it.platform == recommended.platform }?.id ?: 0)
        )
}

object BundledSoftwareCatalog {
    const val VERSION = "2026.09.26.1"

    private fun requirement(
        ram: Int? = null,
        storage: Int? = null,
        cpu: Int? = null,
        gpu: Int? = null,
        vram: Double? = null,
        os: Set<String>,
        note: String,
        status: VerificationStatus = VerificationStatus.VERIFIED,
        platform: String = ""
    ) = SoftwareRequirementTemplate(ram, storage, cpu, gpu, vram, os, note, status, platform)

    private val windowsMac = setOf("Windows 10", "Windows 11", "macOS")
    private val windowsMacLinux = setOf("Windows 10", "Windows 11", "macOS", "Linux")
    private val windowsOnly = setOf("Windows 10", "Windows 11")
    private val macOnly = setOf("macOS")
    private val mobile = setOf("Android", "iOS", "iPadOS")

    val entries = listOf(
        BundledSoftwareDefinition(
            "AutoCAD", "2026", "CAD", "Autodesk", "Windows • macOS",
            "2D drafting and 3D CAD for Windows and Mac computers.", "https://www.autodesk.com/products/autocad/overview",
            "https://help.autodesk.com/view/ACD/2026/ENU/?caas=caas/sfdcarticles/sfdcarticles/System-requirements-for-AutoCAD-2026-including-Specialized-Toolsets.html",
            requirement(8, 10, 3, 2, 2.0, windowsMac, "Official basic requirements; internal CPU/GPU tiers are JI comparison bands."),
            requirement(32, 20, 5, 4, 8.0, windowsMac, "Official recommended memory and graphics targets for desktop workloads.")
        ),
        BundledSoftwareDefinition(
            "SketchUp", "2026", "Architecture", "Trimble", "Windows • macOS",
            "3D modeling for architecture, interiors and design.", "https://www.sketchup.com/",
            "https://help.sketchup.com/en/sketchup/system-requirements",
            requirement(8, 6, 3, 2, 1.0, setOf("Windows 11", "macOS"), "Official platform, memory, storage and graphics guidance."),
            requirement(16, 12, 5, 4, 4.0, setOf("Windows 11", "macOS"), "Comfortable JI target for larger SketchUp models.")
        ),
        BundledSoftwareDefinition(
            "Adobe Photoshop", "2026", "Graphic Design", "Adobe", "Windows • macOS",
            "Photo editing and graphic design on Windows and Mac.", "https://www.adobe.com/products/photoshop.html",
            "https://helpx.adobe.com/photoshop/desktop/get-started/technical-requirements-installation/adobe-photoshop-on-desktop-technical-requirements.html",
            requirement(8, 10, 3, 2, 1.5, windowsMac, "Adobe desktop minimum requirements."),
            requirement(16, 100, 5, 4, 2.0, windowsMac, "Adobe recommended memory, scratch space and GPU target.")
        ),
        BundledSoftwareDefinition(
            "Adobe Premiere Pro", "2026", "Video Editing", "Adobe", "Windows • macOS",
            "Professional video editing on Windows and Mac.", "https://www.adobe.com/products/premiere.html",
            "https://helpx.adobe.com/premiere/desktop/get-started/technical-requirements/adobe-premiere-pro-technical-requirements.html",
            requirement(8, 8, 4, 4, 4.0, setOf("Windows 11", "macOS"), "Adobe minimum target for HD editing."),
            requirement(32, 100, 6, 5, 8.0, setOf("Windows 11", "macOS"), "Adobe recommended target for 4K and higher workloads.")
        ),
        BundledSoftwareDefinition(
            "Microsoft Office", "Microsoft 365", "Office", "Microsoft", "Windows • macOS",
            "Word, Excel, PowerPoint and other Microsoft 365 desktop apps.", "https://www.microsoft.com/microsoft-365",
            "https://support.microsoft.com/en-us/office/system-requirements/system-requirements-for-microsoft-365-for-business-education-and-government-use",
            requirement(4, 4, 2, 1, os = setOf("Windows 11", "macOS"), note = "Microsoft desktop platform and baseline requirements."),
            requirement(8, 8, 3, 2, os = setOf("Windows 11", "macOS"), note = "Comfortable JI multitasking target.")
        ),
        BundledSoftwareDefinition(
            "Visual Studio Code", "Current", "Programming", "Microsoft", "Windows • macOS • Linux",
            "Lightweight code editor for Windows, Mac and Linux.", "https://code.visualstudio.com/",
            "https://code.visualstudio.com/docs/supporting/requirements",
            requirement(1, 1, 1, os = windowsMacLinux, note = "Microsoft lists a 1.6 GHz CPU and 1 GB RAM."),
            requirement(4, 2, 2, 1, os = windowsMacLinux, note = "Comfortable JI target for extensions and development tools.")
        ),
        BundledSoftwareDefinition(
            "Blender", "4.5 LTS", "3D Creation", "Blender Foundation", "Windows • macOS • Linux",
            "3D modeling, animation, rendering and video tools.", "https://www.blender.org/",
            "https://www.blender.org/download/requirements/",
            requirement(8, 10, 4, 3, 2.0, windowsMacLinux, "Blender baseline desktop target."),
            requirement(32, 50, 6, 5, 8.0, windowsMacLinux, "JI target for demanding scenes and rendering.")
        ),
        BundledSoftwareDefinition(
            "Valorant", "Current", "Gaming", "Riot Games", "Windows",
            "Competitive game available for Windows PCs; no native macOS, Android or iOS version.", "https://playvalorant.com/",
            "https://support-valorant.riotgames.com/hc/en-us/articles/360044136134-Minimum-Recommended-PC-Specs",
            requirement(4, 30, 3, 2, os = windowsOnly, note = "Riot Windows PC minimum target."),
            requirement(8, 30, 4, 4, os = windowsOnly, note = "JI target for smoother competitive play.")
        ),
        BundledSoftwareDefinition(
            "OBS Studio", "Current", "Streaming", "OBS Project", "Windows • macOS • Linux",
            "Recording and live-streaming software for desktop computers.", "https://obsproject.com/",
            "https://obsproject.com/kb/system-requirements",
            requirement(4, 2, 3, 2, os = windowsMacLinux, note = "OBS basic platform and GPU requirements."),
            requirement(8, 4, 5, 3, os = windowsMacLinux, note = "Actual needs vary by encoder, resolution, FPS and scene complexity.")
        ),
        BundledSoftwareDefinition(
            "Google Chrome", "Current desktop", "Productivity", "Google", "Windows • macOS • Linux",
            "Desktop web browser for Windows, Mac and supported Linux distributions.", "https://www.google.com/chrome/",
            "https://support.google.com/chrome/answer/95346",
            requirement(4, 1, 2, os = windowsMacLinux, note = "Google desktop platform support; RAM target is a practical JI baseline."),
            requirement(8, 2, 3, 1, os = windowsMacLinux, note = "Comfortable JI target for many browser tabs.")
        ),
        BundledSoftwareDefinition(
            "Autodesk Revit", "2026", "Architecture", "Autodesk", "Windows",
            "Building information modeling software for Windows workstations.", "https://www.autodesk.com/products/revit/overview",
            "https://www.autodesk.com/support/technical/article/caas/sfdcarticles/sfdcarticles/System-requirements-for-Autodesk-Revit-products.html",
            requirement(16, 30, 4, 3, 4.0, windowsOnly, "Revit is a Windows desktop application."),
            requirement(32, 50, 6, 5, 8.0, windowsOnly, "JI target for larger BIM projects.")
        ),
        BundledSoftwareDefinition(
            "SOLIDWORKS", "2026", "Engineering", "Dassault Systèmes", "Windows",
            "Professional mechanical CAD for supported Windows computers.", "https://www.solidworks.com/",
            "https://www.solidworks.com/support/system-requirements",
            requirement(16, 25, 4, 4, 4.0, windowsOnly, "Windows workstation baseline."),
            requirement(32, 50, 6, 5, 8.0, windowsOnly, "JI target for complex assemblies and simulation.")
        ),
        BundledSoftwareDefinition(
            "Final Cut Pro", "Current", "Video Editing", "Apple", "macOS",
            "Apple professional video editor for Mac.", "https://www.apple.com/final-cut-pro/",
            "https://www.apple.com/final-cut-pro/specs/",
            requirement(8, 10, 4, 3, os = macOnly, note = "Mac-only application; feature support varies by Apple silicon generation."),
            requirement(16, 50, 6, 5, os = macOnly, note = "JI target for advanced effects and larger projects.")
        ),
        BundledSoftwareDefinition(
            "Xcode", "26", "Programming", "Apple", "macOS",
            "Apple development environment for apps on Apple platforms.", "https://developer.apple.com/xcode/",
            "https://developer.apple.com/support/xcode/",
            requirement(8, 20, 4, 2, os = macOnly, note = "Xcode is available on supported macOS versions."),
            requirement(16, 50, 6, 4, os = macOnly, note = "JI target for simulators and larger builds.")
        ),
        BundledSoftwareDefinition(
            "DaVinci Resolve", "20", "Video Editing", "Blackmagic Design", "Windows • macOS • Linux",
            "Professional editing, color, visual effects and audio software.", "https://www.blackmagicdesign.com/products/davinciresolve",
            "https://www.blackmagicdesign.com/support/family/davinci-resolve-and-fusion",
            requirement(16, 20, 5, 4, 4.0, windowsMacLinux, "Desktop baseline; exact GPU support differs by platform."),
            requirement(32, 100, 6, 6, 8.0, windowsMacLinux, "JI target for 4K editing and effects.")
        ),
        BundledSoftwareDefinition(
            "Zoom Workplace", "Current desktop", "Communication", "Zoom", "Windows • macOS • Linux",
            "Meetings and collaboration on desktop computers.", "https://zoom.us/download",
            "https://support.zoom.com/hc/en/article?id=zm_kb&sysparm_article=KB0079800",
            requirement(4, 1, 2, 1, os = windowsMacLinux, note = "Zoom provides desktop clients for Windows, Mac and Linux."),
            requirement(8, 2, 3, 2, os = windowsMacLinux, note = "Comfortable JI target for calls and screen sharing.")
        ),
        BundledSoftwareDefinition(
            "Microsoft 365 Mobile", "Current", "Office", "Microsoft", "Android • iOS • iPadOS",
            "Microsoft productivity apps for Android phones/tablets, iPhone and iPad.", "https://www.microsoft.com/microsoft-365/mobile",
            "https://support.microsoft.com/en-us/accounts-billing/subscriptions/microsoft-365-system-requirements",
            requirement(os = mobile, note = "Official mobile platform availability. Microsoft does not publish a comparable CPU/GPU tier for every supported phone and tablet.")
        ),
        BundledSoftwareDefinition(
            "Canva Mobile", "Current", "Graphic Design", "Canva", "Android • iOS • iPadOS",
            "Design and mobile video editing on Android, iPhone and iPad.", "https://www.canva.com/mobile/",
            "https://www.canva.com/video-editor/mobile-app/",
            requirement(os = mobile, note = "Canva confirms Android and iOS availability but does not publish universal CPU/GPU tiers for all devices.")
        ),
        BundledSoftwareDefinition(
            "CapCut Mobile", "Current", "Video Editing", "ByteDance", "Android • iOS • iPadOS",
            "Mobile video editor for Android phones/tablets, iPhone and iPad.", "https://www.capcut.com/",
            "https://www.capcut.com/tools/video-editor-download",
            requirement(os = mobile, note = "Official mobile platform availability; no invented universal CPU/GPU minimum is applied.")
        ),
        BundledSoftwareDefinition(
            "Zoom Workplace Mobile", "Current", "Communication", "Zoom", "Android • iOS • iPadOS",
            "Meetings and collaboration on Android, iPhone and iPad.", "https://zoom.us/download",
            "https://support.zoom.com/hc/en/article?id=zm_kb&sysparm_article=KB0079800",
            requirement(os = mobile, note = "Official Android and iOS availability; actual call features depend on the device and app release.")
        ),
        BundledSoftwareDefinition(
            "Roblox Mobile", "Current", "Gaming", "Roblox Corporation", "Android • iOS • iPadOS",
            "Roblox experiences on supported Android, iPhone and iPad devices.", "https://www.roblox.com/mobile",
            "https://en.help.roblox.com/hc/en-us/articles/203625474-Roblox-Mobile-System-Requirements",
            requirement(os = setOf("Android 8", "iOS 14", "iPadOS 14"), note = "Publisher minimum operating-system versions. Roblox does not provide one comparable CPU/GPU tier covering every mobile device.")
        ),
        BundledSoftwareDefinition(
            "Google Chrome Mobile", "Current", "Productivity", "Google", "Android • iOS • iPadOS",
            "Chrome browser for Android phones/tablets, iPhone and iPad.", "https://www.google.com/chrome/mobile/",
            "https://support.google.com/chrome/answer/95346",
            requirement(os = mobile, note = "Official mobile platform availability; no invented universal CPU/GPU minimum is applied.")
        )
    )
}
