# 解析阶段流式处理与缓存优化分析

## 背景

InstallerX 已在 ZIP 分析层加入 seekable 支持，因此需要重新评估解析阶段是否仍有必要把输入或容器内 APK 缓存为本地文件，并对照 `D:/code/PackageInstaller-vvb` 的处理方式确认可行边界。

结论是：**可以明显减少落盘，但当前不能无条件删除全部缓存。**

现有 seekable 支持解决的是“在本地文件上随机访问 ZIP entry”的问题。它仍以 `FileEntity.path` 为底座，还没有把 `content://`、文件描述符或其他可重开来源抽象为通用随机访问数据源。因此合理目标应是：

> seekable 且生命周期可靠时零缓存，能力不足时保留落盘回退。

## 当前存在的两层缓存

### 1. 顶层 `content://` 源文件缓存

`SourceResolver.resolveContentUri()` 会先尝试把 URI 解析为当前进程可直接读取的真实路径。如果成功，则关闭 `AssetFileDescriptor` 并直接使用该路径；如果失败，则把内容复制到 session 缓存目录。

这层缓存不仅为解析器提供文件路径，还承担以下职责：

- 主动断开与来源 `ContentProvider` 的 Binder 依赖；
- 保证等待用户确认期间来源仍然可用；
- 保证通知安装和自动安装等较长生命周期流程不会因来源应用退出而失败；
- 防止 URI 授权被撤销、provider 被杀或源内容被替换后影响安装；
- 为模块安装等必须接收真实文件路径的流程提供输入。

因此，它不是单纯由 ZIP API 不支持流式输入造成的临时设计。

相关位置：

- `app/src/main/java/com/rosan/installer/data/session/resolver/SourceResolver.kt`
- `app/src/main/java/com/rosan/installer/data/session/handler/ActionHandler.kt`

### 2. 容器内 APK 临时文件

分析 APKS、普通多 APK ZIP，以及部分 XAPK 时，`ApkParser.parseArchiveEntryFull()` 会把容器内 APK 解压为 `anl_*.apk`，再通过 `parseFull()` 分析。

这份临时 APK 当前用于：

- `ApkAssets.loadFromPath()` 加载 APK 资源；
- 解析资源化的应用名称和图标；
- 解析完整 AndroidManifest，包括权限和兼容性信息；
- 枚举 native library 并确定架构；
- 读取 Xposed 相关文件和元数据；
- 通过 apksig 分析签名；
- 在分析结束后作为部分返回实体的数据来源保留到安装完成。

相关位置：

- `app/src/main/java/com/rosan/installer/data/engine/parser/ApkParser.kt`
- `app/src/main/java/com/rosan/installer/data/engine/parser/strategy/ApksStrategy.kt`
- `app/src/main/java/com/rosan/installer/data/engine/parser/strategy/MultiApkZipStrategy.kt`
- `app/src/main/java/com/rosan/installer/data/engine/parser/strategy/XApkStrategy.kt`

## 现有 seekable 支持的实际边界

当前实现已经能够：

- 通过 Commons Compress 的 seekable channel 读取中央目录；
- 在中央目录不可用或与 local header 不一致时读取 local header；
- 保存 entry 的 offset、压缩大小、解压大小、压缩方法和 CRC；
- 安装时按 entry 打开输入流，不必预先解压全部 split APK；
- 对 `STORED` 和 `DEFLATED` entry 提供流式读取及 CRC 校验。

但它仍然依赖本地文件：

- `SeekableZipReader` 接收 `File` 并使用 `RandomAccessFile`；
- `UnifiedZipFileProvider` 接收文件路径或 `File`；
- `SeekableZipEntryEntity` 的父级是 `FileEntity`；
- `FileSliceInputStream` 按文件路径重新打开输入；
- `FileTypeDetector` 和 `UnifiedContainerAnalyser` 只处理 `FileEntity`；
- `ApkParser.parseFull()` 也明确要求 `FileEntity`。

所以这里的 seekable 是“本地文件可 seek”，而不是“任意来源可 seek”。

## 与 PackageInstaller-vvb 的差异

vvb 对顶层 `AssetFileDescriptor` 直接执行：

```kotlin
ZipFile.builder()
    .setSeekableByteChannel(afd.createInputStream().channel)
    .get()
```

对于普通 APK，vvb 还会通过 `ApkAssets.loadFromFd()` 直接加载文件描述符。安装阶段则重新打开 URI，把 APK 或选中的 ZIP entries 流式写入 `PackageInstaller.Session`。

但 vvb 对容器内 APK 的分析更轻量：它从内层 APK 中读取 `AndroidManifest.xml` 字节，再通过 `XmlBlock` 解析。这样可以避免解压整个内层 APK，却不能等价提供 InstallerX 当前的全部分析结果，尤其是：

- APK 自身资源表中的 label 和 icon；
- 完整权限和兼容性提示；
- native library 架构分析；
- Xposed 元数据；
- 完整 apksig 签名验证。

同时，vvb 的解析与安装流程相对短且单一，可以在两个阶段分别重新打开 URI。InstallerX 还需要覆盖 dialog、notification、automatic、batch 和模块安装等流程，不能默认 URI 在整个 session 内始终可重新打开。

因此，vvb 的实现可以作为 FD/channel 接入方式的参考，但不能直接作为 InstallerX 缓存策略的等价替换。

## 各类输入的可行性

| 场景 | 零缓存可行性 | 主要约束 |
| --- | --- | --- |
| 可直接读取的 `file://` 或真实路径 | 已实现 | 直接使用 `FileEntity` |
| 可 seek 且可可靠持有的 `content://` | 条件可行 | 需要由 session 管理 AFD/PFD 生命周期 |
| pipe、socket、不可 seek FD | 不可直接实现 | ZIP 中央目录和签名验证需要随机访问 |
| 普通顶层 APK 的 seekable FD | 可行 | `ApkAssets.loadFromFd()` 和 apksig `DataSource` 可使用 FD/channel |
| ZIP 中 `STORED` 的内层 APK | Android 11+ 条件可行 | 可把父 FD 的 entry offset/length 交给 `ApkAssets.loadFromFd()` |
| ZIP 中 `DEFLATED` 的内层 APK | 不能直接作为 APK FD | FD slice 是压缩数据，解压流不具备随机访问能力 |
| APKM/XAPK 中已有完整 JSON 元数据的 APK | 大部分分析可无临时文件 | 签名验证仍需随机访问数据源 |
| Magisk/KernelSU/APatch 模块 ZIP | 当前不能去掉路径 | 现有安装命令要求真实文件路径 |
| HTTP/HTTPS 来源 | 暂不建议取消缓存 | 安装生命周期、重试和随机访问均需稳定本地副本；还要保持 offline 边界 |

## apksig 可以不依赖临时文件

项目当前使用 apksig 9.3.0。其 `ApkVerifier.Builder` 同时接受：

```text
ApkVerifier.Builder(File)
ApkVerifier.Builder(DataSource)
```

`DataSources` 可以从以下对象创建随机访问数据源：

- `RandomAccessFile`；
- `RandomAccessFile` 的 offset/length slice；
- `FileChannel`；
- `FileChannel` 的 offset/length slice；
- `ByteBuffer`。

因此，对于顶层 seekable FD 以及父 ZIP 中 `STORED` 的 APK entry，签名分析可以直接使用 channel slice，不需要创建 `sig_*.apk`。

对于 `DEFLATED` entry，解压后的 APK 没有天然的随机访问 backing store。除非引入内存映射、分块随机访问缓存或其他复杂实现，否则仍需要一个解压后的临时文件。对大型 APK 使用完整内存缓冲并不合适。

## 推荐实施路径

### 第一阶段：缩短内层 APK 临时文件生命周期

这是风险最低且收益直接的一步。

调整 `parseArchiveEntryFull()`：

1. 仍把内层 APK 临时解压为文件；
2. 使用临时文件完成 `ApkAssets`、Manifest、资源和签名分析；
3. 将返回 `AppEntity` 的 `data` 恢复为原始 `ZipFileEntity` 或 `SeekableZipEntryEntity`；
4. 在 `finally` 中立即删除 `anl_*.apk`；
5. 安装阶段继续从原始外层 ZIP entry 流式写入 session。

这样不能做到解析过程中完全零落盘，但可以消除当前从分析结束一直保留到 session 结束的重复 APK 副本，并且不改变安装数据源语义。

需要特别处理：

- 确保 `BaseEntity` 和 `SplitEntity` 都不会残留临时 `FileEntity`；
- 签名结果、图标、权限和架构必须在删除前全部物化；
- 分析失败和取消时都必须删除临时文件；
- 并行分析多个 APK 时，临时文件所有权必须相互独立。

### 第二阶段：引入可重开、可随机访问的数据源抽象

不要继续在 `DataEntity` 中增加只适用于某一种 FD 的特例。更合适的方向是抽象一个 session-owned source，例如提供：

```kotlin
interface RandomAccessSource : Closeable {
    val size: Long
    fun openStream(offset: Long = 0, length: Long = size): InputStream
    fun openChannel(): SeekableByteChannel
}
```

Android 数据层可以额外提供受控的 FD 能力，但不应把 Android `Context` 或 `ContentResolver` 泄漏到 domain 层。

该抽象应满足：

- 每次调用都能得到位置独立的 stream/channel；
- 明确 source、slice 和打开的 stream/channel 分别由谁关闭；
- 支持父 source 上的 offset/length slice；
- 能判断是否真正 seekable；
- session 结束时统一释放持有的 AFD/PFD；
- provider 失效后产生明确错误，而不是静默回退到错误数据；
- 不依赖反射重建裸文件描述符；
- 不使用 `/proc/<pid>/fd` 作为跨生命周期的数据契约。

随后让以下组件基于 source/channel 工作，而非强制使用路径：

- `FileTypeDetector`；
- `UnifiedZipFileProvider`；
- `SeekableZipReader`；
- `UnifiedZipFile`；
- ZIP entry `DataEntity`；
- `PendingApkSignatureAnalyzer`；
- 普通 APK 的 `ApkParser` FD 路径。

### 第三阶段：为 `ApkAssets` 增加 FD/slice 解析路径

普通顶层 APK 可以直接使用它自己的 FD。

对于容器内 APK：

- `STORED` entry 的 payload 在父文件中是完整连续 APK，可使用 `dataOffset` 和 `uncompressedSize`；
- Android 11+ 可使用带 offset/length 的 `ApkAssets.loadFromFd()`；
- Android 10 及以下没有等价的 offset/length 调用，内层 APK 仍应回退到临时文件；
- `DEFLATED` entry 无论系统版本如何，都不能直接把压缩 slice 当作 APK 加载。

FD 解析路径需要与当前 path 解析路径产生一致的：

- packageName、版本、splitName；
- label、icon；
- 权限；
- ABI/架构；
- Xposed 信息；
- 签名结果和警告。

### 第四阶段：对 `content://` 采用能力检测和保守回退

只有同时满足以下条件时，才考虑不复制顶层 URI：

- FD 是普通文件且真实可 seek；
- 长度与 offset 可确定；
- URI 授权或已打开 FD 能可靠覆盖整个 session；
- 当前安装流允许持有 provider 关联；
- 不需要把路径交给模块安装器或远端特权进程；
- 重新打开或持有失败时能够在分析前回退缓存，而不是安装阶段才失败。

通知、自动安装、批量安装和等待用户操作的 dialog session 应保守处理。即使技术上可以 seek，也不一定值得牺牲当前“复制后断开 provider”的可靠性。

## 不建议的方案

### 把一次性 `InputStream` 直接放进 session

当前 `StreamDataEntity` 保存的是单个 stream，无法安全重读，也无法支持类型检测、解析、签名和安装多次消费。它不适合作为通用解析数据源。

### 对所有内层 APK 只解析 Manifest

这会退化 InstallerX 已有的完整分析能力，导致 label、icon、权限、架构、Xposed 信息或签名结果缺失。除非明确接受产品行为变化，否则不应照搬 vvb 的轻量路径。

### 用完整 `ByteArray` 替代临时文件

这只是把磁盘压力变成内存压力。大型 APK、批量 APK 或多 split 并发分析时容易造成明显内存峰值，且不适合作为默认策略。

### 无条件长期持有来源 provider 的 FD

这会重新引入当前缓存逻辑刻意规避的 provider 关联和来源生命周期问题，也可能使通知/自动安装在来源应用退出后变得不可靠。

## 建议的验证矩阵

实现时至少覆盖以下组合：

| 维度 | 用例 |
| --- | --- |
| 来源 | 真实路径、普通 DocumentsProvider、私有 FileProvider、不可 seek pipe、HTTP |
| 格式 | APK、APKS、APKM、XAPK、多 APK ZIP、模块 ZIP、混合模块 |
| ZIP entry | `STORED`、`DEFLATED`、损坏中央目录、中央目录与 local header 不一致 |
| 系统版本 | Android 8、10、11、当前 target 平台 |
| 安装流 | dialog、notification、automatic、batch |
| 分析选项 | 签名检查开启/关闭、模块功能开启/关闭 |
| 生命周期 | 来源应用退出、授权失效、用户长时间停留、取消、分析异常、安装重试 |

需要验证的不只是编译和解析结果，还包括：

- 分析结束后没有遗留不必要的 `anl_*.apk`/`sig_*.apk`；
- 安装读取的仍是原始 entry，且 CRC/大小正确；
- 关闭 session 后所有 AFD/PFD/channel 均释放；
- 单应用安装提前清缓存和批量安装延后清缓存的行为保持正确；
- offline flavor 没有获得任何网络能力或权限变化；
- 模块安装仍能获得真实且可由特权后端访问的路径。

## 最终建议

不建议一次性把解析缓存全部删除。推荐顺序是：

1. 先让容器内临时 APK 只存活于 `parseArchiveEntryFull()` 调用期间；
2. 再将 ZIP 和签名层改为通用随机访问 source；
3. 为普通 APK和 `STORED` 内层 APK增加 FD/slice 解析；
4. 最后仅对满足生命周期条件的 `content://` 取消顶层缓存；
5. 始终为不可 seek、`DEFLATED` 内层 APK、模块安装及不可靠来源保留落盘回退。

这个路径能保留 InstallerX 当前的完整分析能力和多安装流可靠性，同时逐步消除确实没有必要的重复落盘。
