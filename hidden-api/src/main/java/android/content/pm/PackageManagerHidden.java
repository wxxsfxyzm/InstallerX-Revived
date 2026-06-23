/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.content.pm;

import android.content.IntentSender;
import android.content.res.Resources;

@SuppressWarnings("JavadocReference")
public final class PackageManagerHidden {
    /**
     * @hide
     */
    public static final String EXTRA_NOT_UNKNOWN_SOURCE =
            "android.intent.extra.NOT_UNKNOWN_SOURCE";

    /**
     * @hide
     */
    public static final int INSTALL_UNKNOWN = 0;

    /**
     * Flag parameter for package installation to replace an existing package if present.
     *
     * @hide
     */
    public static final int INSTALL_REPLACE_EXISTING = 0x00000002;

    /**
     * Flag parameter for package installation to allow test packages.
     *
     * @hide
     */
    public static final int INSTALL_ALLOW_TEST = 0x00000004;

    /**
     * Flag parameter for package installation to install on internal storage.
     *
     * @hide
     */
    public static final int INSTALL_INTERNAL = 0x00000010;

    /**
     * Flag parameter for package installation to install on external storage.
     *
     * @hide
     */
    public static final int INSTALL_EXTERNAL = 0x00000008;

    /**
     * Flag parameter for package installation originating from adb.
     *
     * @hide
     */
    public static final int INSTALL_FROM_ADB = 0x00000020;

    /**
     * Flag parameter for package installation for all users.
     *
     * @hide
     */
    public static final int INSTALL_ALL_USERS = 0x00000040;

    /**
     * Flag parameter for package installation to request downgrade handling.
     *
     * @hide
     */
    public static final int INSTALL_REQUEST_DOWNGRADE = 0x00000080;

    /**
     * Flag parameter for package installation to grant all requested permissions.
     *
     * @hide
     */
    public static final int INSTALL_GRANT_ALL_REQUESTED_PERMISSIONS = 0x00000100;

    /**
     * Flag parameter for package installation as an instant app.
     *
     * @hide
     */
    public static final int INSTALL_INSTANT_APP = 0x00000800;

    /**
     * Flag parameter for package installation to avoid killing the app.
     *
     * @hide
     */
    public static final int INSTALL_DONT_KILL_APP = 0x00001000;

    /**
     * Flag parameter for package installation as a full app.
     *
     * @hide
     */
    public static final int INSTALL_FULL_APP = 0x00004000;

    /**
     * Flag parameter for package installation to allocate storage aggressively.
     *
     * @hide
     */
    public static final int INSTALL_ALLOCATE_AGGRESSIVE = 0x00008000;

    /**
     * Flag parameter for package installation as a virtual preload.
     *
     * @hide
     */
    public static final int INSTALL_VIRTUAL_PRELOAD = 0x00010000;

    /**
     * Flag parameter for package installation of APEX packages.
     *
     * @hide
     */
    public static final int INSTALL_APEX = 0x00020000;

    /**
     * Flag parameter for package installation to enable rollback.
     *
     * @hide
     */
    public static final int INSTALL_ENABLE_ROLLBACK = 0x00040000;

    /**
     * Flag parameter for package installation to disable verification.
     *
     * @hide
     */
    public static final int INSTALL_DISABLE_VERIFICATION = 0x00080000;

    /**
     * Flag parameter for package installation to allow downgrade.
     *
     * @hide
     */
    public static final int INSTALL_ALLOW_DOWNGRADE = 0x00100000;

    /**
     * Flag parameter for staged package installation.
     *
     * @hide
     */
    public static final int INSTALL_STAGED = 0x00200000;

    /**
     * Flag parameter for dry-run package installation.
     *
     * @hide
     */
    public static final int INSTALL_DRY_RUN = 0x00800000;

    /**
     * Flag parameter for package installation to disable allowed APEX update checks.
     *
     * @hide
     */
    public static final int INSTALL_DISABLE_ALLOWED_APEX_UPDATE_CHECK = 0x00800000;

    /**
     * Flag parameter for package installation to bypass the low target SDK block.
     *
     * @hide
     */
    public static final int INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK = 0x01000000;

    /**
     * Flag parameter for package installation to request update ownership.
     *
     * @hide
     */
    public static final int INSTALL_REQUEST_UPDATE_OWNERSHIP = 1 << 25;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * on success.
     *
     * @hide
     */
    public static final int INSTALL_SUCCEEDED = 1;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the package is already installed.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_ALREADY_EXISTS = -1;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the package archive file is invalid.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_INVALID_APK = -2;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the URI passed in is invalid.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_INVALID_URI = -3;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the package manager service found that the device didn't have enough storage space to
     * install the app.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_INSUFFICIENT_STORAGE = -4;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if a package is already installed with the same name.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_DUPLICATE_PACKAGE = -5;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the requested shared user does not exist.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_NO_SHARED_USER = -6;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if a previously installed package of the same name has a different signature than the new
     * package (and the old package's data was not removed).
     *
     * @hide
     */
    public static final int INSTALL_FAILED_UPDATE_INCOMPATIBLE = -7;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the new package is requested a shared user which is already installed on the device and
     * does not have matching signature.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_SHARED_USER_INCOMPATIBLE = -8;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the new package uses a shared library that is not available.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_MISSING_SHARED_LIBRARY = -9;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * when the package being replaced is a system app and the caller didn't provide the
     * {@link #DELETE_SYSTEM_APP} flag.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_REPLACE_COULDNT_DELETE = -10;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the new package failed while optimizing and validating its dex files, either because there
     * was not enough storage or the validation failed.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_DEXOPT = -11;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the new package failed because the current SDK version is older than that required by the
     * package.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_OLDER_SDK = -12;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the new package failed because it contains a content provider with the same authority as a
     * provider already installed in the system.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_CONFLICTING_PROVIDER = -13;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the new package failed because the current SDK version is newer than that required by the
     * package.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_NEWER_SDK = -14;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the new package failed because it has specified that it is a test-only package and the
     * caller has not supplied the {@link #INSTALL_ALLOW_TEST} flag.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_TEST_ONLY = -15;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the package being installed contains native code, but none that is compatible with the
     * device's CPU_ABI.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_CPU_ABI_INCOMPATIBLE = -16;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the new package uses a feature that is not available.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_MISSING_FEATURE = -17;

    // ------ Errors related to sdcard
    /**
     * Installation return code: this is passed in the
     * {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS} if a secure container mount point couldn't be
     * accessed on external media.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_CONTAINER_ERROR = -18;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the new package couldn't be installed in the specified install location.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_INVALID_INSTALL_LOCATION = -19;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the new package couldn't be installed in the specified install location because the media
     * is not available.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_MEDIA_UNAVAILABLE = -20;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the new package couldn't be installed because the verification timed out.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_VERIFICATION_TIMEOUT = -21;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the new package couldn't be installed because the verification did not succeed.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_VERIFICATION_FAILURE = -22;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the package changed from what the calling program expected.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_PACKAGE_CHANGED = -23;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the new package is assigned a different UID than it previously held.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_UID_CHANGED = -24;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the new package has an older version code than the currently installed package.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_VERSION_DOWNGRADE = -25;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the old package has target SDK high enough to support runtime permission and the new
     * package has target SDK low enough to not support runtime permissions.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_PERMISSION_MODEL_DOWNGRADE = -26;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the new package attempts to downgrade the target sandbox version of the app.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_SANDBOX_VERSION_DOWNGRADE = -27;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the new package requires at least one split and it was not provided.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_MISSING_SPLIT = -28;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the new package targets a deprecated SDK version.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_DEPRECATED_SDK_VERSION = -29;

    /**
     * Installation parse return code: this is passed in the
     * {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS} if the parser was given a path that is not a
     * file, or does not end with the expected '.apk' extension.
     *
     * @hide
     */
    public static final int INSTALL_PARSE_FAILED_NOT_APK = -100;

    /**
     * Installation parse return code: this is passed in the
     * {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS} if the parser was unable to retrieve the
     * AndroidManifest.xml file.
     *
     * @hide
     */
    public static final int INSTALL_PARSE_FAILED_BAD_MANIFEST = -101;

    /**
     * Installation parse return code: this is passed in the
     * {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS} if the parser encountered an unexpected
     * exception.
     *
     * @hide
     */
    public static final int INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION = -102;

    /**
     * Installation parse return code: this is passed in the
     * {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS} if the parser did not find any certificates in
     * the .apk.
     *
     * @hide
     */
    public static final int INSTALL_PARSE_FAILED_NO_CERTIFICATES = -103;

    /**
     * Installation parse return code: this is passed in the
     * {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS} if the parser found inconsistent certificates on
     * the files in the .apk.
     *
     * @hide
     */
    public static final int INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES = -104;

    /**
     * Installation parse return code: this is passed in the
     * {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS} if the parser encountered a
     * CertificateEncodingException in one of the files in the .apk.
     *
     * @hide
     */
    public static final int INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING = -105;

    /**
     * Installation parse return code: this is passed in the
     * {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS} if the parser encountered a bad or missing
     * package name in the manifest.
     *
     * @hide
     */
    public static final int INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME = -106;

    /**
     * Installation parse return code: tthis is passed in the
     * {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS} if the parser encountered a bad shared user id
     * name in the manifest.
     *
     * @hide
     */
    public static final int INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID = -107;

    /**
     * Installation parse return code: this is passed in the
     * {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS} if the parser encountered some structural
     * problem in the manifest.
     *
     * @hide
     */
    public static final int INSTALL_PARSE_FAILED_MANIFEST_MALFORMED = -108;

    /**
     * Installation parse return code: this is passed in the
     * {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS} if the parser did not find any actionable tags
     * (instrumentation or application) in the manifest.
     *
     * @hide
     */
    public static final int INSTALL_PARSE_FAILED_MANIFEST_EMPTY = -109;

    /**
     * Installation failed return code: this is passed in the
     * {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS} if the system failed to install the package
     * because of system issues.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_INTERNAL_ERROR = -110;

    /**
     * Installation failed return code: this is passed in the
     * {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS} if the system failed to install the package
     * because the user is restricted from installing apps.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_USER_RESTRICTED = -111;

    /**
     * Installation failed return code: this is passed in the
     * {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS} if the system failed to install the package
     * because it is attempting to define a permission that is already defined by some existing
     * package.
     * <p>
     * The package name of the app which has already defined the permission is passed to a
     * {@link PackageInstallObserver}, if any, as the {@link #EXTRA_FAILURE_EXISTING_PACKAGE} string
     * extra; and the name of the permission being redefined is passed in the
     * {@link #EXTRA_FAILURE_EXISTING_PERMISSION} string extra.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_DUPLICATE_PERMISSION = -112;

    /**
     * Installation failed return code: this is passed in the
     * {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS} if the system failed to install the package
     * because its packaged native code did not match any of the ABIs supported by the system.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_NO_MATCHING_ABIS = -113;

    /**
     * Internal return code for NativeLibraryHelper methods to indicate that the package
     * being processed did not contain any native code. This is placed here only so that
     * it can belong to the same value space as the other install failure codes.
     *
     * @hide
     */
    public static final int NO_NATIVE_LIBRARIES = -114;

    /**
     * @hide
     */
    public static final int INSTALL_FAILED_ABORTED = -115;

    /**
     * Installation failed return code: install type is incompatible with some other
     * installation flags supplied for the operation; or other circumstances such as trying
     * to upgrade a system app via an Incremental or instant app install.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_SESSION_INVALID = -116;

    /**
     * Installation parse return code: this is passed in the
     * {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS} if the dex metadata file is invalid or
     * if there was no matching apk file for a dex metadata file.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_BAD_DEX_METADATA = -117;

    /**
     * Installation parse return code: this is passed in the
     * {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS} if there is any signature problem.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_BAD_SIGNATURE = -118;

    /**
     * Installation failed return code: a new staged session was attempted to be committed while
     * there is already one in-progress or new session has package that is already staged.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_OTHER_STAGED_SESSION_IN_PROGRESS = -119;

    /**
     * Installation failed return code: one of the child sessions does not match the parent session
     * in respect to staged or rollback enabled parameters.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_MULTIPACKAGE_INCONSISTENCY = -120;

    /**
     * Installation failed return code: the required installed version code
     * does not match the currently installed package version code.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_WRONG_INSTALLED_VERSION = -121;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the new package failed because it contains a request to use a process that was not
     * explicitly defined as part of its &lt;processes&gt; tag.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_PROCESS_NOT_DEFINED = -122;

    /**
     * Installation failed return code: the {@code resources.arsc} of one of the APKs being
     * installed is compressed or not aligned on a 4-byte boundary. Resource tables that cannot be
     * memory mapped exert excess memory pressure on the system and drastically slow down
     * construction of {@link Resources} objects.
     *
     * @hide
     */
    public static final int INSTALL_PARSE_FAILED_RESOURCES_ARSC_COMPRESSED = -124;

    /**
     * Installation failed return code: the package was skipped and should be ignored.
     * <p>
     * The reason for the skip is undefined.
     *
     * @hide
     */
    public static final int INSTALL_PARSE_FAILED_SKIPPED = -125;

    /**
     * Installation failed return code: this is passed in the
     * {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS} if the system failed to install the package
     * because it is attempting to define a permission group that is already defined by some
     * existing package.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_DUPLICATE_PERMISSION_GROUP = -126;

    /**
     * Installation failed return code: this is passed in the
     * {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS} if the system failed to install the package
     * because it is attempting to define a permission in a group that does not exists or that is
     * defined by an packages with an incompatible certificate.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_BAD_PERMISSION_GROUP = -127;

    /**
     * Installation failed return code: an error occurred during the activation phase of this
     * session.
     *
     * @hide
     */
    public static final int INSTALL_ACTIVATION_FAILED = -128;

    /**
     * Installation failed return code: requesting user pre-approval is currently unavailable.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_PRE_APPROVAL_NOT_AVAILABLE = -129;

    /**
     * Installation return code: this is passed in the {@link PackageInstallerHidden#EXTRA_LEGACY_STATUS}
     * if the new package declares bad certificate digest for a shared library in the package
     * manifest.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_SHARED_LIBRARY_BAD_CERTIFICATE_DIGEST = -130;

    /**
     * Installation failed return code: if the system failed to install the package that
     * {@link android.R.attr#multiArch} is true in its manifest because its packaged
     * native code did not match all of the natively ABIs supported by the system.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_MULTI_ARCH_NOT_MATCH_ALL_NATIVE_ABIS = -131;

    /**
     * Flag parameter for {@link #deletePackage} to indicate that you don't want to delete the
     * package's data directory.
     *
     * @hide
     */

    public static final int DELETE_KEEP_DATA = 0x00000001;

    /**
     * Flag parameter for {@link #deletePackage} to indicate that you want the
     * package deleted for all users.
     *
     * @hide
     */

    public static final int DELETE_ALL_USERS = 0x00000002;

    /**
     * Flag parameter for {@link #deletePackage} to indicate that, if you are calling
     * uninstall on a system that has been updated, then don't do the normal process
     * of uninstalling the update and rolling back to the older system version (which
     * needs to happen for all users); instead, just mark the app as uninstalled for
     * the current user.
     *
     * @hide
     */
    public static final int DELETE_SYSTEM_APP = 0x00000004;

    /**
     * Flag parameter for {@link #deletePackage} to indicate that, if you are calling
     * uninstall on a package that is replaced to provide new feature splits, the
     * existing application should not be killed during the removal process.
     *
     * @hide
     */
    public static final int DELETE_DONT_KILL_APP = 0x00000008;

    /**
     * Flag parameter for {@link PackageInstaller#uninstall(VersionedPackage, int, IntentSender)} to
     * indicate that the deletion is an archival. This
     * flag is only for internal usage as part of
     * {@link PackageInstaller#requestArchive}.
     */
    public static final int DELETE_ARCHIVE = 0x00000010;

    /**
     * Flag parameter for {@link #deletePackage} to indicate that package deletion
     * should be chatty.
     *
     * @hide
     */
    public static final int DELETE_CHATTY = 0x80000000;

    /**
     * Return code for when package deletion succeeds. This is passed to the
     * {@link IPackageDeleteObserver} if the system succeeded in deleting the
     * package.
     *
     * @hide
     */
    public static final int DELETE_SUCCEEDED = 1;

    /**
     * Deletion failed return code: this is passed to the
     * {@link IPackageDeleteObserver} if the system failed to delete the package
     * for an unspecified reason.
     *
     * @hide
     */
    public static final int DELETE_FAILED_INTERNAL_ERROR = -1;

    /**
     * Deletion failed return code: this is passed to the
     * {@link IPackageDeleteObserver} if the system failed to delete the package
     * because it is the active DevicePolicy manager.
     *
     * @hide
     */
    public static final int DELETE_FAILED_DEVICE_POLICY_MANAGER = -2;

    /**
     * Deletion failed return code: this is passed to the
     * {@link IPackageDeleteObserver} if the system failed to delete the package
     * since the user is restricted.
     *
     * @hide
     */
    public static final int DELETE_FAILED_USER_RESTRICTED = -3;

    /**
     * Deletion failed return code: this is passed to the
     * {@link IPackageDeleteObserver} if the system failed to delete the package
     * because a profile or device owner has marked the package as
     * uninstallable.
     *
     * @hide
     */
    public static final int DELETE_FAILED_OWNER_BLOCKED = -4;

    /**
     * @hide
     */
    public static final int DELETE_FAILED_ABORTED = -5;

    /**
     * Deletion failed return code: this is passed to the
     * {@link IPackageDeleteObserver} if the system failed to delete the package
     * because the packge is a shared library used by other installed packages.
     *
     * @hide
     */
    public static final int DELETE_FAILED_USED_SHARED_LIBRARY = -6;

    /**
     * Deletion failed return code: this is passed to the
     * {@link IPackageDeleteObserver} if the system failed to delete the package
     * because there is an app pinned.
     *
     * @hide
     */
    public static final int DELETE_FAILED_APP_PINNED = -7;

    /**
     * Deletion failed return code: this is passed to the
     * {@link IPackageDeleteObserver} if the system failed to delete the package
     * for any child profile with {@link UserProperties#getDeleteAppWithParent()} as true.
     *
     * @hide
     */
    public static final int DELETE_FAILED_FOR_CHILD_PROFILE = -8;

    private PackageManagerHidden() {
        throw new UnsupportedOperationException();
    }
}
