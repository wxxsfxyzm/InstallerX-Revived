import { defineConfig } from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
  // --- 全局通用配置 (Global Config) ---
  head: [
    ['meta', { charset: 'utf-8' }],
    ['meta', { name: 'viewport', content: 'width=device-width, initial-scale=1' }],
    ['meta', { name: 'theme-color', content: '#3c3c3c' }],
    
    // SEO Meta Tags
    ['meta', { name: 'keywords', content: 'InstallerX, Android installer, APK installer, XAPK, APKS, package manager, app installation' }],
    ['meta', { name: 'author', content: 'iamr0s & InstallerX Revived Contributors' }],
    ['meta', { name: 'robots', content: 'index, follow, max-snippet:-1, max-image-preview:large, max-video-preview:-1' }],
    ['meta', { property: 'og:type', content: 'website' }],
    ['meta', { property: 'og:url', content: 'https://wxxsfxyzm.github.io/InstallerX-Revived/' }],
    ['meta', { property: 'og:title', content: 'InstallerX Revived - Modern Android Package Installer' }],
    ['meta', { property: 'og:description', content: 'Universal Android APK installer with advanced features, Material 3 design, and Shizuku support' }],
    ['meta', { property: 'og:image', content: 'https://wxxsfxyzm.github.io/InstallerX-Revived/ic_launcher.svg' }],
    ['meta', { property: 'twitter:card', content: 'summary_large_image' }],
    ['meta', { property: 'twitter:url', content: 'https://wxxsfxyzm.github.io/InstallerX-Revived/' }],
    ['meta', { property: 'twitter:title', content: 'InstallerX Revived' }],
    ['meta', { property: 'twitter:description', content: 'Universal Android APK installer with advanced features' }],
    ['meta', { property: 'twitter:image', content: 'https://wxxsfxyzm.github.io/InstallerX-Revived/ic_launcher.svg' }],
    
    // Canonical URL
    ['link', { rel: 'canonical', href: 'https://wxxsfxyzm.github.io/InstallerX-Revived/' }],
    
    // Preconnect to external resources
    ['link', { rel: 'preconnect', href: 'https://github.com' }],
    ['link', { rel: 'dns-prefetch', href: 'https://github.com' }],
    
    // Icon
    ['link', { rel: 'icon', href: '/InstallerX-Revived/ic_launcher.svg' }],
    ['link', { rel: 'shortcut icon', href: '/InstallerX-Revived/ic_launcher.svg' }],
    ['link', { rel: 'apple-touch-icon', href: '/InstallerX-Revived/ic_launcher.svg' }],
    
    // JSON-LD Structured Data
    ['script', { type: 'application/ld+json' }, JSON.stringify({
      '@context': 'https://schema.org',
      '@type': 'SoftwareApplication',
      'name': 'InstallerX Revived',
      'description': 'A modern, feature-rich Android package installer with universal compatibility',
      'applicationCategory': 'UtilitiesApplication',
      'operatingSystem': 'Android',
      'url': 'https://wxxsfxyzm.github.io/InstallerX-Revived/',
      'image': 'https://wxxsfxyzm.github.io/InstallerX-Revived/ic_launcher.svg',
      'author': {
        '@type': 'Organization',
        'name': 'InstallerX Revived Contributors'
      }
    })],
    
    // Sitemap and RSS feed links
    ['link', { rel: 'sitemap', href: '/InstallerX-Revived/sitemap.xml' }]
  ],
  base: '/InstallerX-Revived/',
  title: "InstallerX Revived",
  description: "Universal Android APK installer supporting APK, APKS, APKM, XAPK formats. Features Material 3 design, Shizuku support, and advanced installation options.",

  sitemap: {
    hostname: 'https://wxxsfxyzm.github.io/InstallerX-Revived/',
    lastmodDateOnly: false
  },

  lastUpdated: true,

  // 默认语言 (当不匹配任何路径时)
  lang: 'en-US',

  themeConfig: {
    // 全局共享的主题配置 (Logo, 社交链接, 搜索)
    // 这些不需要根据语言改变
    socialLinks: [
      { icon: 'github', link: 'https://github.com/wxxsfxyzm/InstallerX-Revived' },
      { icon: 'telegram', link: 'https://t.me/installerx_revived' }
    ],

    search: {
      provider: 'local',
      options: {
        locales: {
          zh: {
            translations: {
              button: {
                buttonText: '搜索文档',
                buttonAriaLabel: '搜索文档'
              },
              modal: {
                noResultsText: '无法找到相关结果',
                resetButtonTitle: '清除查询条件',
                footer: {
                  selectText: '选择',
                  navigateText: '切换',
                  closeText: '关闭'
                }
              }
            }
          }
        }
      }
    }
  },

  // --- 多语言配置 (Locales) ---
  locales: {
    // === 英文 (默认根目录) ===
    root: {
      label: 'English',
      lang: 'en',
      themeConfig: {
        // 英文导航栏
        nav: [
          { text: 'Home', link: '/' },
          { text: 'Guide', link: '/guide/intro' }
        ],

        // 英文侧边栏
        sidebar: [
          {
            text: 'User Guide',
            items: [
              { text: 'What is InstallerX Revived?', link: '/guide/intro' },
              { text: 'Installation', link: '/guide/installation' },
              { text: 'Quick Start', link: '/guide/quick-start' }
            ]
          },
          {
            text: 'Advanced Features',
            items: [
              { text: 'Profiles & Scopes', link: '/guide/profiles' },
              { text: 'System Integration', link: '/guide/system-integration' },
              { text: 'Laboratory', link: '/guide/laboratory' },
              { text: 'App Settings', link: '/guide/app-settings' }
            ]
          },
          {
            text: 'Install Options',
            items: [
              { text: 'Parameters', link: '/guide/install-options' },
              { text: 'Exceptions', link: '/guide/exceptions' }
            ]
          },
          {
            text: 'Support',
            items: [
              { text: 'FAQ', link: '/guide/faq' }
            ]
          }
        ],

        // 英文页脚
        footer: {
          message: 'Released under the GPL-3.0 License.',
          copyright: 'Copyright © 2023-now iamr0s & InstallerX Revived Contributors'
        },

        // 英文界面文字
        docFooter: {
          prev: 'Previous Page',
          next: 'Next Page'
        },
        editLink: {
          pattern: 'https://github.com/wxxsfxyzm/InstallerX-Revived/edit/main/website/docs/:path',
          text: 'Edit this page on GitHub'
        }
      }
    },

    // === 中文 (位于 /zh/ 目录) ===
    zh: {
      label: '简体中文',
      lang: 'zh',
      link: '/zh/', // 这里对应 docs/zh/ 目录

      // 中文特定的 SEO 信息
      title: "InstallerX Revived",
      description: "现代、功能丰富的 Android 安装器",

      themeConfig: {
        // 中文导航栏
        nav: [
          { text: '首页', link: '/zh/' },
          { text: '指南', link: '/zh/guide/intro' }
        ],

        // 中文侧边栏 (链接路径都要加 /zh/ 前缀)
        sidebar: [
          {
            text: '用户指南',
            items: [
              { text: '什么是 InstallerX Revived?', link: '/zh/guide/intro' },
              { text: '安装指南', link: '/zh/guide/installation' },
              { text: '快速开始', link: '/zh/guide/quick-start' }
            ]
          },
          {
            text: '高级功能',
            items: [
              { text: '用户配置文件', link: '/zh/guide/profiles' },
              // { text: '应用设置', link: '/zh/guide/app-settings' },
              // { text: '实验室', link: '/zh/guide/laboratory' },
              // { text: '系统集成', link: '/zh/guide/system-integration' }
            ]
          },
          // {
          //   text: '安装选项',
          //   items: [
          //     { text: '参数设定', link: '/zh/guide/install-options' },
          //     { text: '异常处理', link: '/zh/guide/exceptions' }
          //   ]
          // },
          {
            text: '支持',
            items: [
              { text: '常见问题', link: '/zh/guide/faq' }
            ]
          }
        ],

        // 中文页脚
        footer: {
          message: '基于 GPL-3.0 协议发布',
          copyright: 'Copyright © 2023 - Present iamr0s & InstallerX Revived Contributors'
        },

        // 中文界面文字本地化
        docFooter: {
          prev: '上一页',
          next: '下一页'
        },
        outline: {
          label: '页面导航'
        },
        lastUpdated: {
          text: '最后更新于'
        },
        sidebarMenuLabel: '菜单',
        returnToTopLabel: '回到顶部',
        darkModeSwitchLabel: '主题',

        editLink: {
          pattern: 'https://github.com/wxxsfxyzm/InstallerX-Revived/edit/main/website/docs/:path',
          text: '在 GitHub 上编辑此页'
        }
      }
    }
  }
})