import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'

// Vant 组件按需引入
import {
  Button,
  Cell,
  CellGroup,
  Form,
  Field,
  Tabbar,
  TabbarItem,
  Icon,
  Card,
  Tag,
  Search,
  List,
  PullRefresh,
  NavBar,
  Popup,
  Toast,
  Dialog,
  Loading,
  Empty,
  Divider,
  Image as VanImage,
  Lazyload
} from 'vant'

// Vant 样式
import 'vant/lib/index.css'

const app = createApp(App)
const pinia = createPinia()

// 注册 Vant 组件
app.use(Button)
app.use(Cell)
app.use(CellGroup)
app.use(Form)
app.use(Field)
app.use(Tabbar)
app.use(TabbarItem)
app.use(Icon)
app.use(Card)
app.use(Tag)
app.use(Search)
app.use(List)
app.use(PullRefresh)
app.use(NavBar)
app.use(Popup)
app.use(Toast)
app.use(Dialog)
app.use(Loading)
app.use(Empty)
app.use(Divider)
app.use(VanImage)
app.use(Lazyload)

app.use(pinia)
app.use(router)
app.mount('#app')
