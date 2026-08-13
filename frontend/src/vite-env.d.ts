/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_MOCKING?: 'enabled'
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
