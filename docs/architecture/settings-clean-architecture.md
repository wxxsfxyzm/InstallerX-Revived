# Settings 域 Clean Architecture 最终方案

## 目标

- **Domain 层**只包含业务语义（设置项语义、仓储接口、领域模型）。
- **Data 层**只包含具体存储实现（DataStore、Room）。
- **Presentation 层**通过用例/仓储接口获取设置，不接触 DataStore key。

## 推荐目录（以 settings 域为例）

```text
app/src/main/java/com/rosan/installer/
  domain/
    settings/
      model/
        NamedPackage.kt
        SharedUid.kt
      repository/
        AppSettingsRepo.kt
        AppRepo.kt
        ConfigRepo.kt

  data/
    settings/
      local/
        datastore/
          AppDataStore.kt
      repository/
        AppSettingsRepoImpl.kt      # DataStore -> Domain 仓储实现
        AppRepoImpl.kt              # Room -> Domain 仓储实现
        ConfigRepoImpl.kt           # Room -> Domain 仓储实现
      model/
        room/
          ...

  ui/
    ...
```

## 分层职责

### Domain
- `AppSettingsRepo` 提供 settings 领域接口。
- `StringSetting/IntSetting/BooleanSetting/...` 提供 settings 语义键。
- `AppRepo/ConfigRepo` 作为设置域内应用配置关联的业务仓储接口。

### Data
- `AppDataStore` 是 DataStore 适配器，不向上暴露 `Preferences.Key`。
- `AppSettingsRepoImpl` 将 settings 语义映射到 DataStore key。
- `AppRepoImpl/ConfigRepoImpl` 负责 Room DAO 到领域仓储接口的适配。

### Presentation
- ViewModel / Activity / Handler 仅依赖 `domain.settings.repository.*`。
- 不再 import `AppDataStore` / `Preferences.Key`。

## DI 约束

- `datastoreModule`: 提供 `AppDataStore` + `AppSettingsRepo` 绑定。
- `roomModule`: 提供 `AppRepo/ConfigRepo`（domain 接口）绑定。
- 其他模块仅注入 domain 仓储接口。

## 后续优化（下一阶段）

1. 将 `StringSetting/BooleanSetting/...` 进一步升级为语义化 API（例如 `observeThemeSettings()`）。
2. 补齐 `domain/settings/usecase`（读写设置用例），让 ViewModel 依赖 usecase 而不是 repo。
3. 把 `ConfigUtil` 逐步拆为 usecase + mapper，减少静态工具对象。
4. 删除 legacy `AppSettingKey` 兼容对象。
