// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.packageinfo

/** Names verified current AOSP signing keys, excluding rotation ancestors and source stamps. */
fun AppSignatureInfo.aospSigningKeys(): Set<String> {
    if (!verified) return emptySet()
    return buildSet {
        signerSha256Set.mapNotNullTo(this) { it.aospKeyName() }
        if (isHybrid) hybridClassicalCertificate?.sha256?.aospKeyName()?.let { add(it) }
        fallbackIdentity?.signerSha256Set?.mapNotNullTo(this) { it.aospKeyName() }
    }
}

private fun String.aospKeyName(): String? = AOSP_SIGNING_KEYS[lowercase()]

// SHA-256 of the DER-encoded public APK development certificates; no APK or certificate reads.
// https://android.googlesource.com/platform/build/+/045a3d6a3e359633a14853a5a5e1e4f2a11cbdae/target/product/security/
private val AOSP_SIGNING_KEYS = mapOf(
    "a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc" to "testkey",
    "c8a2e9bccf597c2fb6dc66bee293fc13f2fc47ec77bc6b2b0d52c11f51192ab8" to "platform",
    "28bbfe4a7b97e74681dc55c2fbb6ccb8d6c74963733f6af6ae74d8c3a6e879fd" to "shared",
    "465983f7791f2abeb43ea2cbdc7f21a8260b72bc08a55c839fc1a43bc741a81e" to "media",
    "e1dbadce60dc080d15b58a014b0dcf9400e24de23fa00b287a5a982bfebda2ee" to "networkstack",
    "a6ccc500ff0e7421200eb66a7fe174ef1b00e52ca91727070cbedf061ff76c35" to "bluetooth",
    "abf21f9e2af1d881cc673fddcefa6ed9c269a437bd64b279cf45844cfd589126" to "sdk_sandbox",
    "fae9122a8721d6e2a196d2224dffcf773c9127e2bb956cbddb40b009192ffdfd" to "nfc",
)
