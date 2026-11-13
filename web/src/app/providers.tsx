import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { App as AntApp, ConfigProvider } from 'antd'
import viVN from 'antd/locale/vi_VN'
import type { ReactNode } from 'react'
import { antTheme } from '@/shared/theme/theme'

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false } } })

/** Các provider bọc toàn app: theme Ant Design (tiếng Việt), App (message/modal), React Query. */
export default function Providers({ children }: { children: ReactNode }) {
  return (
    <ConfigProvider locale={viVN} theme={antTheme}>
      <AntApp>
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
      </AntApp>
    </ConfigProvider>
  )
}
