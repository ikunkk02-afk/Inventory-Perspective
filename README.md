# Inventory Perspective

Fabric 1.21.1 客户端模组。第一阶段：打开生存背包时平滑进入越肩镜头，默认玩家在左、背包在右，关闭后返回原先的第一/第三人称视角。

![默认右侧背包](docs/images/inventory-right.png)

## 安装与使用

将本模组及对应 **Fabric 1.21.1** 的前置放入客户端 `mods` 文件夹：

- Fabric API
- [InvMove 0.9.3](https://modrinth.com/mod/invmove/version/nOwgsiRT)：背包内移动、跳跃、疾跑及下蹲。
- [Cloth Config 15.0.140](https://modrinth.com/mod/cloth-config/version/HpMb5wGb)：设置界面。
- 可选 [Mod Menu](https://modrinth.com/mod/modmenu)：从模组列表打开本模组设置；按启动器提示补齐它的前置。

进入生存世界，按原有背包键打开。背包操作仍由原版处理，贴图沿用已启用的材质包。仅改变面板的位置，槽位、拖动、数字键、合成等不另做一套逻辑。移动由 InvMove 处理，请保持它的移动选项开启；其默认下蹲模式会保持打开前的下蹲状态，也可在 InvMove 设置中改为实时按键模式。搜索框聚焦时建议启用 InvMove 的禁用移动选项。

Mod Menu → Inventory Perspective → 设置：可启停、切换左右、调整动画时长。无 Mod Menu 时可编辑 `config/inventory-perspective.json`（首次保存设置后生成），默认如下：

```json
{ "enabled": true, "inventoryOnRight": true, "transitionMillis": 350 }
```

## 当前范围

- 生存背包与碰撞检测后的越肩镜头；关闭、死亡、切换屏幕及退出世界会释放镜头。
- 配方书展开时沿用原版排布，保证搜索、翻页和合成可操作。小 GUI 宽度下回退居中面板。
- 创造背包、箱子和其他容器保留原版展示。
- 第一版保留背包内的小人预览，完整沿用原版面板。
- 实时拖动编辑、缩放、透明度与其他摄像机模组的专项兼容属于后续阶段。

普通背包贴图替换通过原版资源加载生效；修改背包布局的界面模组仍需单独验证。InvMove 允许的移动是否被服务器接受由服务器规则决定。

## 构建

使用 **JDK 25** 运行 Gradle（Loom 1.18.3 / Gradle 9.7.1），并安装 **JDK 21** 供 Java toolchain 编译与运行客户端。产物的目标 Java 版本为 21。

```powershell
./gradlew.bat build
./gradlew.bat runClient
```

可通过 `-PwithoutModMenu` 在开发运行时移除可选的 Mod Menu，检查可选依赖路径。构建结果在 `build/libs/`。

## 自动客户端检查

测试会启动独立 Minecraft 客户端、创建专用平坦世界、执行检查后退出，不接触已有存档。测试模组与输入模拟不会进入发布 JAR。

```powershell
./gradlew.bat --no-configuration-cache -PclientSmoke runClientSmoke
./gradlew.bat --no-configuration-cache -PclientSmoke -PwithoutModMenu runClientSmoke
```

结果及原始截图分别位于 `run-smoke/smoke-evidence/` 和 `run-smoke-without-modmenu/smoke-evidence/`。检查移动通过 InvMove 的真实输入轮询执行，模拟的仅为物理按键状态；物品点击由原版屏幕处理并经集成服务器同步。验证范围见 [验证记录](docs/validation.md)。

MIT License，见 LICENSE。
