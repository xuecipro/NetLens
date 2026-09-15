# NetLens

**网络分析 · 测速 · 优化建议**（中英双语）

开源 Android 网络工具：在功能上覆盖 **WiFi Analyzer Pro** 的核心能力，并借鉴中国信通院 **全球网测** 的测速与诊断思路，针对当前网络状况给出可执行的优化建议。

## 功能

### WiFi 分析（对标 WiFi Analyzer Pro）
- 扫描周边 WiFi：SSID / BSSID / 信号强度 / 频段（2.4 / 5 / 6 GHz）/ 信道 / 信道宽度
- 安全类型：OPEN / WEP / WPA / WPA2 / WPA3 / Enterprise
- WiFi 标准：WiFi 4 / 5 / 6 / 6E / 7
- 当前连接详情：链路速率、IP、网关、DNS、厂商 OUI
- 信道图与信道评分，推荐更空闲信道
- 局域网设备扫描（ARP + 并发探测）

### 网络测速（借鉴全球网测）
- 下载 / 上传吞吐（默认 Cloudflare 公共测速点）
- 延迟（ICMP ping，失败时 TCP 兜底）
- 抖动、丢包率
- 网络评分与等级（A+ ~ F）

### 智能优化建议
综合信号、信道拥挤度、安全协议、链路速率与测速结果，输出中英文建议：
- 弱信号 / 覆盖边缘
- 信道拥挤与切换推荐
- 过时或开放加密
- 协商速率与实测吞吐差距
- 高延迟 / 高抖动 / 高丢包

## 与 WiFi Analyzer Pro 功能对照

| WiFi Analyzer Pro | NetLens |
|---|---|
| 周边 WiFi 扫描与图表 | WiFi 列表 + 信号条 + 频段过滤 |
| 信道推荐 / Channel rating | 信道图 + 评分 + 最佳信道 |
| 当前网络详情（SSID/BSSID/厂商/速率/安全/DHCP） | 当前连接卡片 |
| 已连接设备 | 局域网设备扫描 |
| 信号与延迟分析 | RSSI 仪表 + Ping 延迟/抖动/丢包 |
| 导出结果 | 分享报告 |
| 深色/浅色主题、刷新间隔 | 设置页 |
| （Pro 相关）无广告开源替代 | MIT 开源、无广告无追踪 |

在以上基础上增加：完整吞吐测速（类似全球网测）与规则化优化建议。

## 技术栈
- Kotlin + Jetpack Compose + Material 3
- 单 Activity + Navigation Compose
- OkHttp 测速引擎
- 无广告、无追踪 SDK


## 构建

### 本机构建
需要 JDK 17 + Android SDK（compileSdk 35）：

```bash
gradle assembleDebug
# 或
./gradlew assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/`

### GitHub Actions
推送到 GitHub 后，Actions 会自动构建 Debug / Release APK，可在 **Actions → Artifacts** 下载。

```bash
git init
git add .
git commit -m "feat: NetLens initial release"
git branch -M main
git remote add origin https://github.com/<you>/NetLens.git
git push -u origin main
```

## 权限说明
| 权限 | 用途 |
|------|------|
| ACCESS_WIFI_STATE / CHANGE_WIFI_STATE | WiFi 扫描与连接信息 |
| ACCESS_NETWORK_STATE | 网络状态 |
| ACCESS_FINE_LOCATION / COARSE_LOCATION | Android 扫描周边 WiFi 的系统要求 |
| NEARBY_WIFI_DEVICES | Android 13+ WiFi 设备 |
| INTERNET | 测速与连通性检测 |

## 目录结构
```
app/src/main/java/com/opensource/netlens/
  data/model/       数据模型
  data/wifi/        WiFi 扫描与连接
  data/speed/       测速 / Ping
  data/devices/     局域网设备
  data/advisor/     优化建议规则引擎
  ui/               Compose 界面
```

## License
MIT
