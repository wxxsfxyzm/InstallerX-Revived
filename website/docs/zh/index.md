---
# https://vitepress.dev/reference/default-theme-home-page
layout: home
title: InstallerX Revived - 现代化 Android 应用安装器
description: 全能的 Android APK 安装器，支持 APK、APKS、APKM、XAPK 格式，拥有 Material 3 设计和 Shizuku 支持

head:
  - - meta
    - name: keywords
      content: "InstallerX, Android安装器, APK安装, XAPK, 应用管理"
  - - meta
    - property: og:title
      content: "InstallerX Revived - 现代化 Android 应用安装器"
  - - meta
    - property: og:description
      content: "全能的 Android APK 安装器，支持 APK、APKS、APKM、XAPK 格式"

hero:
  name: "InstallerX Revived"
  text: "现代化的 Android 应用安装器"
  image:
    src: /ic_launcher.svg
    alt: InstallerX Logo
  actions:
    - theme: brand
      text: 开始了解 
      link: /zh/guide/intro
    - theme: alt
      text: 查看 GitHub
      link: https://github.com/wxxsfxyzm/InstallerX-Revived

features:
  - title: 全能兼容
    details: 无缝安装 APK、APKS、APKM、XAPK，甚至直接安装 ZIP 压缩包。支持批量安装以及自动选择最佳分包配置。
  - title: 现代美学
    details: 可在 Material 3 Expressive 和 Miuix 风格间自由切换。集成系统图标包支持、动态取色（Android 8+），以及清晰的安装包信息对比。
  - title: 高级能力
    details: 利用 Shizuku/Root 绕过系统限制。支持设定各类安装选项、Dex2oat 优化以及自定义安装选项。
  - title: 原生与安全
    details: 基于原生 API 构建（模块刷写除外）。内置签名校验、权限预览、应用黑名单及多用户安装支持。
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