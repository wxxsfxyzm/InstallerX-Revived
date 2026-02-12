---
# https://vitepress.dev/reference/default-theme-home-page
layout: home
title: InstallerX Revived - Modern Android Package Installer
description: Universal APK installer with advanced features, Material 3 design, and Shizuku support

head:
  - - meta
    - name: keywords
      content: "InstallerX, Android installer, APK installer, XAPK, APKS, package manager"
  - - meta
    - property: og:title
      content: "InstallerX Revived - Modern Android Package Installer"
  - - meta
    - property: og:description
      content: "Universal Android APK installer supporting APK, APKS, APKM, XAPK formats"

hero:
  name: "InstallerX Revived"
  text: "A modern Android package installer"
  image:
    src: /ic_launcher.svg
    alt: InstallerX Logo
  actions:
    - theme: brand
      text: Get Started
      link: /guide/intro
    - theme: alt
      text: View on GitHub
      link: https://github.com/wxxsfxyzm/InstallerX-Revived

features:
  - title: Universal Compatibility
    details: Seamlessly install APK, APKS, APKM, XAPK, and even ZIP archives. Supports batch installation and automatic selection of the best split configuration.
  - title: Modern Aesthetics
    details: Switchable between Material 3 Expressive and Miuix styles. Features system icon pack integration, dynamic color for Android 8+, and clear package info comparisons.
  - title: Advanced Capabilities
    details: Leverages Shizuku/Root to bypass restrictions. Supports setting various install options, dex2oat optimization, and custom install flags.
  - title: Native & Secure
    details: Built with native APIs (except module flashing). Includes signature verification, permission viewing, package blacklisting, and multi-user installation support.
---

<script setup>
import { ref, onMounted } from 'vue'

const displayText = ref('')
const fullText = "You know some birds are not meant to be caged, their feathers are just too bright."
const speed = 60

onMounted(() => {
  let i = 0
  const type = () => {
    if (i < fullText.length) {
      displayText.value += fullText.charAt(i)
      i++
      setTimeout(type, speed)
    }
  }
  type()
})
</script>

<style scoped>
.custom-typewriter-wrapper {
  display: block;
  margin: 16px 0 0 0; 
  line-height: 1.4;
  min-height: 3em; 
}

.typewriter-text {
  color: var(--vp-c-text-2);
  font-size: 1.2rem;
  font-weight: 500;
  display: inline-block; 
  vertical-align: top;
}

.cursor {
  font-weight: bold;
  color: var(--vp-c-brand-1);
  animation: blink 0.8s infinite;
  margin-left: 2px;
  display: inline-block;
  user-select: none;
  pointer-events: none;
  -webkit-user-select: none;
}

@keyframes blink { 50% { opacity: 0; } }

:deep(.VPHero .text) {
  margin-bottom: 0 !important;
}

@media (min-width: 640px) {
  .typewriter-text { font-size: 1.5rem; }
  .custom-typewriter-wrapper { min-height: 4.5rem; }
}
</style>

<ClientOnly>
  <Teleport to=".VPHero .heading">
    <div class="custom-typewriter-wrapper">
      <p class="typewriter-text">
        {{ displayText }}<span class="cursor">|</span>
      </p>
    </div>
  </Teleport>
</ClientOnly>