<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useBlogStore, useThemeStore } from '@/stores'
import { getConfigByKey } from '@/api/systemConfig'

const router = useRouter()
const blogStore = useBlogStore()
const themeStore = useThemeStore()

const isDark = computed(() => {
  if (themeStore.mode === 'dark') return true
  if (themeStore.mode === 'light') return false
  return window.matchMedia('(prefers-color-scheme: dark)').matches
})

/* 滚动检测 */
const scrolled = ref(false)
const handleScroll = () => {
  scrolled.value = window.scrollY > 60
}
onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
  loadSystemConfig()
  let tries = 0
  const tryAttach = () => {
    if (progressBarRef.value && progressBarRef.value instanceof HTMLElement) {
      _attachProgressListeners()
      return true
    }
    if (++tries > 60) return false
    setTimeout(tryAttach, 500)
    return false
  }
  tryAttach()
})
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  _detachProgressListeners()
})

/* 搜索 */
const searchVisible = ref(false)
const keyword = ref('')
const searchInputRef = ref(null)
const mobileNavVisible = ref(false)

/* 音乐播放 */
const isPlaying = ref(false)
const audioRef = ref(null)
const progressBarRef = ref(null)
const musicIndex = ref(0)
const musicListVisible = ref(false)
const neteasePlaylistId = ref('')
const currentTime = ref(0)
const duration = ref(0)
const bufferedPercent = ref(0)
const isDragging = ref(false)
const displayPct = ref(0)
const isProgressHover = ref(false)
const hoverPct = ref(0)
const hoverTimeStr = ref('0:00')
const REPEAT_MODES = ['sequence', 'list', 'one']
const REPEAT_LABELS = {
  sequence: '顺序播放',
  list: '列表循环',
  one: '单曲循环'
}
const repeatIndex = ref(0)
const repeatMode = computed(() => REPEAT_MODES[repeatIndex.value])
const repeatLabel = computed(() => REPEAT_LABELS[repeatMode.value])

const currentTrack = computed(() => blogStore.musics[musicIndex.value] || null)

const formatTime = (seconds) => {
  if (!seconds || !isFinite(seconds)) return '0:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

const updateProgress = () => {
  if (audioRef.value && !isDragging.value) {
    currentTime.value = audioRef.value.currentTime
    if (audioRef.value.duration) duration.value = audioRef.value.duration
    if (duration.value)
      displayPct.value = (currentTime.value / duration.value) * 100
    if (
      audioRef.value.buffered &&
      audioRef.value.buffered.length &&
      duration.value
    ) {
      try {
        bufferedPercent.value =
          (audioRef.value.buffered.end(audioRef.value.buffered.length - 1) /
            duration.value) *
          100
      } catch {
        /* ignore range err when buffered gaps exist */
      }
    }
  }
}

const updateBuffered = () => {
  if (!audioRef.value || !duration.value) return
  if (audioRef.value.buffered && audioRef.value.buffered.length) {
    try {
      bufferedPercent.value =
        (audioRef.value.buffered.end(audioRef.value.buffered.length - 1) /
          duration.value) *
        100
    } catch {
      /* ignore range err when buffered gaps exist */
    }
  }
}

const _rectCache = { el: null, rect: null, at: 0 }
const getRect = () => {
  const el = progressBarRef.value
  if (!el) return null
  const now = Date.now()
  if (_rectCache.el !== el || now - _rectCache.at > 80) {
    _rectCache.el = el
    _rectCache.rect = el.getBoundingClientRect()
    _rectCache.at = now
  }
  return _rectCache.rect
}
const pctFromX = (clientX) => {
  const r = getRect()
  if (!r) return 0
  return Math.max(0, Math.min(1, (clientX - r.left) / r.width))
}

const seekToPct = (pct) => {
  if (!audioRef.value || !duration.value) return
  const t = Math.max(0, Math.min(duration.value, pct * duration.value))
  try {
    audioRef.value.currentTime = t
  } catch {
    /* ignore seek failure on unsupported media */
  }
  currentTime.value = t
  displayPct.value = (t / duration.value) * 100
}

// ========== 原生DOM事件（100%可靠） ==========
let _listenersAttached = false
let _lastClickAt = 0

const _nativeMouseDown = (ev) => {
  if (!duration.value || (ev.button !== undefined && ev.button !== 0)) return
  try {
    ev.preventDefault()
  } catch {
    /* preventDefault may throw on some passive events */
  }
  _lastClickAt = Date.now()
  const p = pctFromX(ev.clientX)
  isDragging.value = true
  displayPct.value = p * 100
  if (progressBarRef.value) progressBarRef.value.classList.add('dragging')
  document.addEventListener('mousemove', _nativeDocMouseMove, true)
  document.addEventListener('mouseup', _nativeDocMouseUp, true)
  document.addEventListener('selectstart', _nativeNoSelect, true)
}
const _nativeDocMouseMove = (ev) => {
  if (!isDragging.value) return
  const p = pctFromX(ev.clientX)
  displayPct.value = p * 100
}
const _nativeDocMouseUp = (ev) => {
  if (!isDragging.value) return
  const p = pctFromX(ev.clientX)
  isDragging.value = false
  if (progressBarRef.value) progressBarRef.value.classList.remove('dragging')
  seekToPct(p)
  document.removeEventListener('mousemove', _nativeDocMouseMove, true)
  document.removeEventListener('mouseup', _nativeDocMouseUp, true)
  document.removeEventListener('selectstart', _nativeNoSelect, true)
}
const _nativeNoSelect = (ev) => {
  try {
    ev.preventDefault()
  } catch {
    /* preventDefault may throw on some passive events */
  }
}

const _nativeTouchStart = (ev) => {
  if (!duration.value || !ev.touches || !ev.touches[0]) return
  const t = ev.touches[0]
  const p = pctFromX(t.clientX)
  isDragging.value = true
  displayPct.value = p * 100
  if (progressBarRef.value) progressBarRef.value.classList.add('dragging')
  document.addEventListener('touchmove', _nativeTouchMove, {
    passive: false,
    capture: true
  })
  document.addEventListener('touchend', _nativeTouchEnd, true)
}
const _nativeTouchMove = (ev) => {
  if (!isDragging.value || !ev.touches || !ev.touches[0]) return
  try {
    ev.preventDefault()
  } catch {
    /* preventDefault may throw on some passive events */
  }
  const p = pctFromX(ev.touches[0].clientX)
  displayPct.value = p * 100
}
const _nativeTouchEnd = () => {
  if (!isDragging.value) return
  const p = displayPct.value / 100
  isDragging.value = false
  if (progressBarRef.value) progressBarRef.value.classList.remove('dragging')
  seekToPct(p)
  document.removeEventListener('touchmove', _nativeTouchMove, true)
  document.removeEventListener('touchend', _nativeTouchEnd, true)
}

const _nativeBarEnter = () => {
  isProgressHover.value = true
}
const _nativeBarLeave = () => {
  isProgressHover.value = false
}
const _nativeBarMove = (ev) => {
  if (!duration.value) return
  const p = pctFromX(ev.clientX)
  hoverPct.value = p * 100
  hoverTimeStr.value = formatTime(p * duration.value)
}
const _nativeBarClick = (ev) => {
  if (Date.now() - _lastClickAt < 220) return
  if (isDragging.value) return
  const p = pctFromX(ev.clientX)
  seekToPct(p)
}

const _attachProgressListeners = () => {
  if (_listenersAttached) return
  const el = progressBarRef.value
  if (!el) return
  el.addEventListener('mousedown', _nativeMouseDown)
  el.addEventListener('touchstart', _nativeTouchStart, { passive: true })
  el.addEventListener('mouseenter', _nativeBarEnter)
  el.addEventListener('mouseleave', _nativeBarLeave)
  el.addEventListener('mousemove', _nativeBarMove)
  el.addEventListener('click', _nativeBarClick)
  _listenersAttached = true
}
const _detachProgressListeners = () => {
  if (!_listenersAttached) return
  const el = progressBarRef.value
  if (el) {
    el.removeEventListener('mousedown', _nativeMouseDown)
    el.removeEventListener('touchstart', _nativeTouchStart)
    el.removeEventListener('mouseenter', _nativeBarEnter)
    el.removeEventListener('mouseleave', _nativeBarLeave)
    el.removeEventListener('mousemove', _nativeBarMove)
    el.removeEventListener('click', _nativeBarClick)
  }
  _listenersAttached = false
}

const loadSystemConfig = async () => {
  try {
    const res = await getConfigByKey('netease_playlist_id')
    if (res.data && res.data.configValue) {
      neteasePlaylistId.value = res.data.configValue
    }
  } catch (e) {
    console.error('加载系统配置失败', e)
  }
}

const togglePlay = () => {
  if (!audioRef.value || !currentTrack.value) return
  if (isPlaying.value) {
    audioRef.value.pause()
  } else {
    audioRef.value.play()
  }
  isPlaying.value = !isPlaying.value
}

const playTrack = (index) => {
  musicIndex.value = index
  isPlaying.value = false
  nextTick(() => {
    if (audioRef.value) {
      audioRef.value.load()
      audioRef.value
        .play()
        .then(() => {
          isPlaying.value = true
        })
        .catch(() => {})
    }
  })
}

const prevTrack = () => {
  if (!blogStore.musics.length) return
  const len = blogStore.musics.length
  playTrack((musicIndex.value - 1 + len) % len)
}

const nextTrack = () => {
  if (!blogStore.musics.length) return
  playTrack((musicIndex.value + 1) % blogStore.musics.length)
}

const handleEnded = () => {
  const len = blogStore.musics.length
  if (!len) return
  const mode = repeatMode.value
  if (mode === 'one') {
    nextTick(() => {
      if (audioRef.value) {
        audioRef.value.currentTime = 0
        audioRef.value.play().catch(() => {})
        isPlaying.value = true
      }
    })
    return
  }
  if (mode === 'sequence' && musicIndex.value >= len - 1) {
    isPlaying.value = false
    if (audioRef.value) audioRef.value.currentTime = 0
    return
  }
  nextTrack()
}

const toggleRepeat = () => {
  repeatIndex.value = (repeatIndex.value + 1) % REPEAT_MODES.length
}

const toggleMusicList = () => {
  musicListVisible.value = !musicListVisible.value
}

const navItems = [
  {
    label: '主页',
    icon: 'icon-zhuye',
    href: '/home/',
    external: true
  },
  { label: '博客', icon: 'icon-boke', to: '/' },
  { label: '归档', icon: 'icon-guidang', to: '/archive' },
  { label: '友链', icon: 'icon-lianjie', to: '/links' },
  { label: '留言', icon: 'icon-liuyan', to: '/message' },
  { label: '关于', icon: 'icon-guanyu', to: '/about' },
  {
    label: '开往',
    icon: 'icon-subway',
    href: 'https://www.travellings.cn/go.html',
    external: true
  }
]

const doSearch = () => {
  const kw = keyword.value.trim()
  if (!kw) return
  searchVisible.value = false
  keyword.value = ''
  router.push({ path: '/', query: { search: kw } })
}

const toggleSearch = () => {
  mobileNavVisible.value = false
  searchVisible.value = !searchVisible.value
  if (!searchVisible.value) {
    keyword.value = ''
  } else {
    nextTick(() => searchInputRef.value?.focus())
  }
}

const toggleMobileNav = () => {
  mobileNavVisible.value = !mobileNavVisible.value
}

const navTo = (item) => {
  mobileNavVisible.value = false
  if (item.external) {
    window.open(item.href, '_blank')
  } else {
    router.push(item.to)
  }
}
</script>

<template>
  <header class="site-header" :class="{ scrolled, dark: isDark }">
    <div class="header-inner">
      <div class="header-left">
        <router-link to="/" class="site-title"
          >师忆的博客 · Shiyi Blog</router-link
        >
        <nav class="nav-desktop">
          <template v-for="item in navItems" :key="item.label">
            <a
              v-if="item.external"
              :href="item.href"
              target="_blank"
              rel="noopener"
              class="nav-link"
            >
              <i :class="['iconfont', item.icon]" /> {{ item.label }}
            </a>
            <router-link v-else :to="item.to" class="nav-link">
              <i :class="['iconfont', item.icon]" /> {{ item.label }}
            </router-link>
          </template>
        </nav>
      </div>

      <div class="header-right">
        <div v-if="currentTrack" class="mini-player-wrap">
          <div class="mini-player" @click="toggleMusicList">
            <img
              v-if="currentTrack.coverImage"
              :src="currentTrack.coverImage"
              class="player-cover"
              :class="{ spinning: isPlaying }"
            />
            <div class="player-info">
              <span class="player-title">{{ currentTrack.title }}</span>
              <div class="player-progress-wrap">
                <div
                  ref="progressBarRef"
                  class="player-progress-bar player-long-progress"
                >
                  <div
                    class="player-progress-buffered"
                    :style="{ width: bufferedPercent + '%' }"
                  />
                  <div
                    class="player-progress-fill"
                    :style="{ width: displayPct + '%' }"
                  />
                  <div
                    class="player-progress-thumb"
                    :style="{ left: displayPct + '%' }"
                  />
                  <transition name="fade-tip">
                    <div
                      v-show="isProgressHover && !isDragging && duration"
                      class="player-progress-tooltip"
                      :style="{ left: hoverPct + '%' }"
                    >
                      {{ hoverTimeStr }}
                    </div>
                  </transition>
                </div>
                <span class="player-time"
                  >{{ formatTime(currentTime) }}/{{
                    formatTime(duration || currentTrack.duration || 0)
                  }}</span
                >
              </div>
            </div>
          </div>
          <button
            class="player-btn"
            @click.stop="toggleRepeat"
            :title="repeatLabel"
          >
            <svg
              v-if="repeatMode === 'sequence'"
              viewBox="0 0 24 24"
              width="15"
              height="15"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <polyline points="17 1 21 5 17 9" />
              <path d="M3 11V9a4 4 0 0 1 4-4h14" />
              <polyline points="7 23 3 19 7 15" />
              <path d="M21 13v2a4 4 0 0 1-4 4H3" />
            </svg>
            <svg
              v-else-if="repeatMode === 'list'"
              viewBox="0 0 24 24"
              width="15"
              height="15"
              fill="none"
              stroke="currentColor"
              stroke-width="2.2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <polyline points="17 1 21 5 17 9" />
              <path d="M3 11V9a4 4 0 0 1 4-4h14" />
              <polyline points="7 23 3 19 7 15" />
              <path d="M21 13v2a4 4 0 0 1-4 4H3" />
            </svg>
            <svg
              v-else
              viewBox="0 0 24 24"
              width="15"
              height="15"
              fill="none"
              stroke="currentColor"
              stroke-width="2.4"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <polyline points="17 1 21 5 17 9" />
              <path d="M3 11V9a4 4 0 0 1 4-4h14" />
              <polyline points="7 23 3 19 7 15" />
              <path d="M21 13v2a4 4 0 0 1-4 4H3" />
              <text
                x="12"
                y="15"
                text-anchor="middle"
                font-size="7"
                font-weight="700"
                fill="currentColor"
                stroke="none"
                style="font-family: system-ui, Arial"
              >
                1
              </text>
            </svg>
          </button>
          <button class="player-btn" @click.stop="prevTrack" title="上一首">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="currentColor">
              <polygon points="19 20 9 12 19 4 19 20" />
              <line
                x1="5"
                y1="19"
                x2="5"
                y2="5"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
              />
            </svg>
          </button>
          <button
            class="player-btn play-pause-btn"
            @click.stop="togglePlay"
            title="播放/暂停"
          >
            <i
              class="iconfont"
              :class="isPlaying ? 'icon-zanting' : 'icon-play-full'"
            />
          </button>
          <button class="player-btn" @click.stop="nextTrack" title="下一首">
            <i class="iconfont icon-next" />
          </button>
          <audio
            ref="audioRef"
            :src="currentTrack.musicUrl"
            preload="none"
            @ended="handleEnded"
            @timeupdate="updateProgress"
            @loadedmetadata="updateProgress"
            @progress="updateBuffered"
          />
          <transition name="fade">
            <div v-show="musicListVisible" class="music-panel">
              <div class="music-panel-header">
                <span><i class="iconfont icon-yinle" /> 播放列表</span>
                <span class="music-panel-count"
                  >{{ blogStore.musics.length }} 首</span
                >
              </div>
              <div v-if="neteasePlaylistId" class="netease-player-wrap">
                <iframe
                  :src="
                    'https://music.163.com/outchain/player?type=0&id=' +
                    neteasePlaylistId +
                    '&auto=0&height=320'
                  "
                  width="100%"
                  height="320"
                  frameborder="no"
                  border="0"
                  marginwidth="0"
                  marginheight="0"
                  scrolling="no"
                />
              </div>
              <ul class="music-panel-list">
                <li
                  v-for="(m, idx) in blogStore.musics"
                  :key="m.id"
                  class="music-panel-item"
                  :class="{ active: idx === musicIndex }"
                  @click="playTrack(idx)"
                >
                  <img
                    v-if="m.coverImage"
                    :src="m.coverImage"
                    class="music-panel-cover"
                  />
                  <div class="music-panel-info">
                    <span class="music-panel-name">{{ m.title }}</span>
                    <span class="music-panel-artist">{{ m.artist }}</span>
                  </div>
                  <i
                    v-if="idx === musicIndex && isPlaying"
                    class="iconfont icon-yinle playing-icon"
                  />
                </li>
              </ul>
            </div>
          </transition>
        </div>

        <!-- 暗色模式切换 -->
        <button
          class="theme-toggle"
          :title="isDark ? '切换到浅色模式' : '切换到暗色模式'"
          @click="themeStore.toggle"
        >
          <!-- 太阳图标（暗色时显示，点击切换到亮色） -->
          <svg
            v-if="isDark"
            viewBox="0 0 24 24"
            width="17"
            height="17"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <circle cx="12" cy="12" r="5" />
            <line x1="12" y1="1" x2="12" y2="3" />
            <line x1="12" y1="21" x2="12" y2="23" />
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
            <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
            <line x1="1" y1="12" x2="3" y2="12" />
            <line x1="21" y1="12" x2="23" y2="12" />
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
            <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
          </svg>
          <!-- 月亮图标（亮色时显示，点击切换到暗色） -->
          <svg
            v-else
            viewBox="0 0 24 24"
            width="17"
            height="17"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z" />
          </svg>
        </button>

        <div class="search-area">
          <div class="search-box" :class="{ expanded: searchVisible }">
            <input
              ref="searchInputRef"
              v-show="searchVisible"
              v-model="keyword"
              type="text"
              placeholder="搜索文章..."
              class="search-input"
              @keyup.enter="doSearch"
              @blur="searchVisible = false"
            />
          </div>
          <button class="search-toggle" @click="toggleSearch" title="搜索">
            <i class="iconfont icon-sousuo" />
          </button>
        </div>

        <button class="mobile-menu-btn" @click="toggleMobileNav">
          <span :class="['bar', { open: mobileNavVisible }]" />
        </button>
      </div>
    </div>

    <nav v-show="mobileNavVisible" class="nav-mobile">
      <a
        v-for="item in navItems"
        :key="item.label"
        class="nav-mobile-link"
        @click="navTo(item)"
      >
        <i :class="['iconfont', item.icon]" /> {{ item.label }}
      </a>
      <a class="nav-mobile-link" @click="toggleSearch">
        <i class="iconfont icon-sousuo" /> 搜索
      </a>
      <a class="nav-mobile-link" @click="themeStore.toggle">
        <template v-if="isDark">
          <svg
            viewBox="0 0 24 24"
            width="15"
            height="15"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            style="vertical-align: -2px; margin-right: 4px"
          >
            <circle cx="12" cy="12" r="5" />
            <line x1="12" y1="1" x2="12" y2="3" />
            <line x1="12" y1="21" x2="12" y2="23" />
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
            <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
            <line x1="1" y1="12" x2="3" y2="12" />
            <line x1="21" y1="12" x2="23" y2="12" />
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
            <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
          </svg>
          浅色模式
        </template>
        <template v-else>
          <svg
            viewBox="0 0 24 24"
            width="15"
            height="15"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            style="vertical-align: -2px; margin-right: 4px"
          >
            <path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z" />
          </svg>
          暗色模式
        </template>
      </a>
    </nav>
  </header>
</template>

<style scoped>
.site-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: transparent;
  border-bottom: none;
  transition:
    background 0.3s,
    box-shadow 0.3s;
}
.site-header.scrolled {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.06);
}
.site-header.dark.scrolled {
  background: rgba(35, 35, 35, 0.95);
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.3);
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 28px;
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 28px;
}
.site-title {
  font-family: var(--blog-serif);
  font-size: 18px;
  font-weight: 800;
  color: #fff;
  text-decoration: none;
  letter-spacing: 0.5px;
  white-space: nowrap;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
  transition:
    color 0.3s,
    text-shadow 0.3s;
}
.scrolled .site-title {
  color: #303133;
  text-shadow: none;
}
.dark.scrolled .site-title {
  color: #e5e5e5;
}
.nav-desktop {
  display: flex;
  align-items: center;
  gap: 2px;
}
.nav-link {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);
  text-decoration: none;
  padding: 6px 10px;
  border-radius: 4px;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  transition:
    color 0.2s,
    background 0.2s,
    text-shadow 0.3s;
}
.nav-link .iconfont {
  font-size: 14px;
}
.nav-link:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.15);
}
.scrolled .nav-link {
  color: #606266;
  text-shadow: none;
}
.scrolled .nav-link:hover {
  color: #303133;
  background: #f5f7fa;
}
.dark.scrolled .nav-link {
  color: #b0b0b0;
}
.dark.scrolled .nav-link:hover {
  color: #e5e5e5;
  background: #333;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 迷你播放器 */
.mini-player-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
  position: relative;
  flex-shrink: 0;
}
.mini-player {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 14px 5px 5px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.25);
  cursor: pointer;
  backdrop-filter: blur(4px);
  min-width: 230px;
  transition:
    background 0.2s,
    border-color 0.2s,
    min-width 0.2s,
    padding 0.2s;
}
.scrolled .mini-player {
  background: #f5f7fa;
  border-color: #e4e7ed;
}
.dark.scrolled .mini-player {
  background: #333;
  border-color: #444;
}
.mini-player:hover {
  border-color: rgba(255, 255, 255, 0.5);
}
.scrolled .mini-player:hover {
  border-color: #909399;
}
.dark.scrolled .mini-player:hover {
  border-color: #666;
}
.player-cover {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.3);
  flex-shrink: 0;
}
.scrolled .player-cover {
  border-color: #e4e7ed;
}
.dark.scrolled .player-cover {
  border-color: #444;
}
.player-cover.spinning {
  animation: spin 8s linear infinite;
}
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
.player-title {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.92);
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
  transition: color 0.3s;
}
.scrolled .player-title {
  color: #303133;
}
.dark.scrolled .player-title {
  color: #e5e5e5;
}
.player-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 150px;
}
.player-progress-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  user-select: none;
  -webkit-user-select: none;
}
.player-progress-bar {
  flex: 1;
  height: 4px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
  cursor: pointer;
  overflow: visible;
  position: relative;
  transition: height 0.15s;
}
.player-progress-bar.player-long-progress {
  height: 5px;
}
.player-progress-bar:hover,
.player-progress-bar.dragging {
  height: 7px;
}
.scrolled .player-progress-bar {
  background: #dcdfe6;
}
.dark.scrolled .player-progress-bar {
  background: #555;
}
.player-progress-buffered {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: 0;
  background: rgba(255, 255, 255, 0.22);
  border-radius: 3px;
  pointer-events: none;
  z-index: 1;
}
.scrolled .player-progress-buffered {
  background: rgba(120, 140, 170, 0.35);
}
.dark.scrolled .player-progress-buffered {
  background: rgba(255, 255, 255, 0.12);
}
.player-progress-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: 0;
  background: linear-gradient(90deg, #fff, #f0f0f0);
  border-radius: 3px;
  box-shadow: 0 0 4px rgba(255, 255, 255, 0.3);
  pointer-events: none;
  z-index: 2;
}
.player-progress-bar.dragging .player-progress-fill {
  transition: none;
}
.player-progress-bar:not(.dragging) .player-progress-fill {
  transition: width 0.12s linear;
}
.scrolled .player-progress-fill {
  background: linear-gradient(90deg, #409eff, #67c23a);
  box-shadow: 0 0 3px rgba(64, 158, 255, 0.4);
}
.dark.scrolled .player-progress-fill {
  background: linear-gradient(90deg, #fff, #c0c4cc);
  box-shadow: 0 0 3px rgba(255, 255, 255, 0.3);
}
.player-progress-thumb {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%) scale(0);
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid rgba(64, 158, 255, 0.9);
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.15);
  transition: transform 0.15s;
  pointer-events: none;
  z-index: 3;
}
.dark .player-progress-thumb {
  background: #fff;
  border-color: rgba(103, 194, 58, 0.9);
}
.player-progress-bar:hover .player-progress-thumb,
.player-progress-bar.dragging .player-progress-thumb {
  transform: translate(-50%, -50%) scale(1);
}
.player-progress-tooltip {
  position: absolute;
  bottom: calc(100% + 6px);
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.78);
  color: #fff;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
  pointer-events: none;
  font-variant-numeric: tabular-nums;
  line-height: 1.4;
  z-index: 10;
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}
.fade-tip-enter-active,
.fade-tip-leave-active {
  transition:
    opacity 0.12s ease,
    transform 0.12s ease;
}
.fade-tip-enter-from,
.fade-tip-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(3px);
}
.player-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
  min-width: 72px;
  text-align: right;
}
.scrolled .player-time {
  color: #606266;
}
.dark.scrolled .player-time {
  color: #b0b0b0;
}
.player-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 5px 6px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 16px;
  transition:
    color 0.2s,
    transform 0.1s;
  display: flex;
  align-items: center;
  border-radius: 4px;
}
.player-btn:hover {
  color: #fff;
  transform: translateY(-1px);
}
.player-btn:active {
  transform: translateY(0) scale(0.94);
}
.player-btn.play-pause-btn {
  font-size: 18px;
  padding: 5px 8px;
}
.player-btn svg {
  display: block;
}
.scrolled .player-btn {
  color: #606266;
}
.scrolled .player-btn:hover {
  color: #409eff;
}
.dark.scrolled .player-btn {
  color: #b0b0b0;
}
.dark.scrolled .player-btn:hover {
  color: #fff;
}

/* 音乐列表 */
.music-panel {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 280px;
  max-height: 360px;
  background: var(--blog-card);
  border: 1px solid var(--blog-border);
  border-radius: 8px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.1);
  z-index: 200;
  overflow: hidden;
}
.music-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid #ebeef5;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.dark .music-panel-header {
  border-bottom-color: #333;
  color: #e5e5e5;
}
.music-panel-header .iconfont {
  font-size: 14px;
  margin-right: 4px;
}
.music-panel-count {
  font-size: 12px;
  color: #909399;
  font-weight: 400;
}
.netease-player-wrap {
  border-bottom: 1px solid #ebeef5;
}
.dark .netease-player-wrap {
  border-bottom-color: #333;
}
.music-panel-list {
  list-style: none;
  margin: 0;
  padding: 4px 0;
  max-height: 300px;
  overflow-y: auto;
}
.music-panel-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  cursor: pointer;
  transition: background 0.12s;
}
.music-panel-item:hover {
  background: #f5f7fa;
}
.dark .music-panel-item:hover {
  background: #333;
}
.music-panel-item.active {
  background: #f5f7fa;
}
.dark .music-panel-item.active {
  background: #333;
}
.music-panel-item.active .music-panel-name {
  color: #000;
  font-weight: 600;
}
.dark .music-panel-item.active .music-panel-name {
  color: #fff;
}
.music-panel-cover {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  object-fit: cover;
  flex-shrink: 0;
  border: 1px solid #ebeef5;
}
.dark .music-panel-cover {
  border-color: #444;
}
.music-panel-info {
  flex: 1;
  min-width: 0;
}
.music-panel-name {
  display: block;
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dark .music-panel-name {
  color: #e5e5e5;
}
.music-panel-artist {
  display: block;
  font-size: 11px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.playing-icon {
  font-size: 14px;
  color: #000;
  flex-shrink: 0;
}
.dark .playing-icon {
  color: #fff;
}
.fade-enter-active,
.fade-leave-active {
  transition:
    opacity 0.15s,
    transform 0.15s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* 暗色模式切换 */
.theme-toggle {
  background: none;
  border: none;
  cursor: pointer;
  padding: 6px;
  color: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  transition: color 0.2s;
}
.theme-toggle:hover {
  color: #fff;
}
.scrolled .theme-toggle {
  color: #606266;
}
.scrolled .theme-toggle:hover {
  color: #000;
}
.dark.scrolled .theme-toggle {
  color: #b0b0b0;
}
.dark.scrolled .theme-toggle:hover {
  color: #fff;
}

/* 搜索 */
.search-area {
  display: flex;
  align-items: center;
}
.search-box {
  overflow: hidden;
  width: 0;
  transition: width 0.3s ease;
}
.search-box.expanded {
  width: 180px;
}
.search-input {
  width: 100%;
  border: 1px solid var(--blog-border);
  border-radius: 4px;
  padding: 5px 10px;
  font-size: 13px;
  background: var(--blog-card);
  color: var(--blog-text);
  outline: none;
  font-family: inherit;
}
.search-input:focus {
  border-color: #000;
}
.dark .search-input:focus {
  border-color: #aaa;
}
.search-toggle {
  background: none;
  border: none;
  cursor: pointer;
  padding: 6px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 16px;
  transition: color 0.2s;
  display: flex;
  align-items: center;
}
.search-toggle:hover {
  color: #fff;
}
.scrolled .search-toggle {
  color: #606266;
}
.scrolled .search-toggle:hover {
  color: #000;
}
.dark.scrolled .search-toggle {
  color: #b0b0b0;
}
.dark.scrolled .search-toggle:hover {
  color: #fff;
}

/* 移动端 */
.mobile-menu-btn {
  display: none;
  background: none;
  border: none;
  cursor: pointer;
  width: 28px;
  height: 28px;
  position: relative;
}
.bar,
.bar::before,
.bar::after {
  display: block;
  width: 18px;
  height: 2px;
  background: #fff;
  position: absolute;
  left: 5px;
  transition:
    transform 0.2s,
    background 0.3s;
}
.scrolled .bar,
.scrolled .bar::before,
.scrolled .bar::after {
  background: #303133;
}
.dark.scrolled .bar,
.dark.scrolled .bar::before,
.dark.scrolled .bar::after {
  background: #e5e5e5;
}
.bar {
  top: 13px;
}
.bar::before {
  content: '';
  top: -6px;
}
.bar::after {
  content: '';
  top: 6px;
}
.bar.open {
  background: transparent;
}
.bar.open::before {
  top: 0;
  transform: rotate(45deg);
}
.bar.open::after {
  top: 0;
  transform: rotate(-45deg);
}
.nav-mobile {
  display: none;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(8px);
  padding: 8px 24px 12px;
  border-top: 1px solid #ebeef5;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}
.dark .nav-mobile {
  background: rgba(35, 35, 35, 0.96);
  border-top-color: #333;
}
.nav-mobile-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 0;
  font-size: 14px;
  color: #606266;
  text-decoration: none;
  border-bottom: 1px solid #f2f6fc;
  cursor: pointer;
}
.nav-mobile-link .iconfont {
  font-size: 15px;
}
.nav-mobile-link:hover {
  color: #303133;
}
.dark .nav-mobile-link {
  color: #b0b0b0;
  border-bottom-color: #333;
}
.dark .nav-mobile-link:hover {
  color: #e5e5e5;
}

@media (max-width: 1200px) {
  .mini-player {
    min-width: 200px;
  }
  .player-info {
    min-width: 120px;
  }
  .player-title {
    max-width: 85px;
  }
}
@media (max-width: 1000px) {
  .header-left {
    gap: 18px;
  }
  .header-right {
    gap: 6px;
  }
  .mini-player-wrap {
    gap: 0;
  }
  .player-btn {
    padding: 4px 4px;
    font-size: 14px;
  }
  .mini-player {
    min-width: 180px;
    padding: 4px 10px 4px 4px;
    gap: 8px;
  }
  .player-cover {
    width: 28px;
    height: 28px;
  }
  .player-info {
    min-width: 100px;
    gap: 3px;
  }
  .player-title {
    max-width: 70px;
    font-size: 11px;
  }
  .player-progress-wrap {
    gap: 5px;
  }
  .player-progress-bar.player-long-progress {
    height: 4px;
  }
  .player-time {
    font-size: 10px;
    min-width: 60px;
  }
}
@media (max-width: 768px) {
  .nav-desktop {
    display: none;
  }
  .theme-toggle {
    display: none;
  }
  .search-box.expanded {
    width: 110px;
  }
  .mobile-menu-btn {
    display: block;
  }
  .nav-mobile {
    display: flex;
    flex-direction: column;
  }
  .header-inner {
    padding: 0 16px;
    gap: 6px;
  }
  /* 移动端自适应：播放器精简显示（保留核心播放+进度条）*/
  .mini-player-wrap {
    display: flex;
    align-items: center;
    gap: 1px;
  }
  .mini-player {
    min-width: auto;
    padding: 3px 8px 3px 3px;
    border-radius: 18px;
    gap: 6px;
  }
  .player-cover {
    width: 24px;
    height: 24px;
  }
  .player-info {
    min-width: 90px;
    flex: 1;
  }
  .player-title {
    max-width: 90px;
    font-size: 10.5px;
  }
  .player-progress-wrap {
    gap: 4px;
  }
  .player-progress-bar.player-long-progress {
    height: 3px;
  }
  .player-progress-bar:hover {
    height: 4px;
  }
  .player-time {
    display: none;
  }
  /* 移动端：隐藏次要按钮（循环、上一首），仅保留核心三控件 */
  .player-btn.play-pause-btn {
    font-size: 16px;
    padding: 4px 6px;
  }
  .mini-player-wrap .player-btn:nth-child(2),
  .mini-player-wrap .player-btn:nth-child(3) {
    display: none;
  }
}
@media (max-width: 520px) {
  .site-title {
    font-size: 15px;
  }
  .header-inner {
    padding: 0 10px;
  }
  .mini-player {
    padding: 3px 6px 3px 3px;
  }
  .player-info {
    min-width: 70px;
  }
  .player-title {
    max-width: 60px;
  }
  .mini-player-wrap .player-btn.play-pause-btn {
    padding: 3px 4px;
    font-size: 14px;
  }
  .mini-player-wrap .player-btn {
    font-size: 13px;
  }
}
</style>
