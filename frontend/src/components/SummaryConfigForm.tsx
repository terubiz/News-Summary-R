import { useEffect, useState } from 'react'
import { SummaryConfigRequest, SummaryConfigResponse, getSummaryConfig, updateSummaryConfig } from '../api/summaryConfigApi'

export default function SummaryConfigForm() {
  const [form, setForm] = useState<SummaryConfigRequest>({
    executionTime: '07:00',
    lookbackDays: 1,
    fetchCount: 10,
    aiProviderName: 'gemini',
  })
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    getSummaryConfig().then((cfg: SummaryConfigResponse) =>
      setForm({
        executionTime: cfg.executionTime,
        lookbackDays: cfg.lookbackDays,
        fetchCount: cfg.fetchCount,
        aiProviderName: cfg.aiProviderName,
      })
    )
  }, [])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await updateSummaryConfig(form)
      setSaved(true)
      setError('')
      setTimeout(() => setSaved(false), 3000)
    } catch {
      setError('保存に失敗しました')
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700">実行時刻 (HH:mm)</label>
        <input
          type="text"
          value={form.executionTime}
          onChange={(e) => setForm({ ...form, executionTime: e.target.value })}
          pattern="^([01]\d|2[0-3]):[0-5]\d$"
          className="mt-1 block w-full border rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700">遡り日数 (1–365)</label>
        <input
          type="number"
          min={1}
          max={365}
          value={form.lookbackDays}
          onChange={(e) => setForm({ ...form, lookbackDays: Number(e.target.value) })}
          className="mt-1 block w-full border rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700">取得件数 (1–100)</label>
        <input
          type="number"
          min={1}
          max={100}
          value={form.fetchCount}
          onChange={(e) => setForm({ ...form, fetchCount: Number(e.target.value) })}
          className="mt-1 block w-full border rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700">AI プロバイダー</label>
        <select
          value={form.aiProviderName}
          onChange={(e) => setForm({ ...form, aiProviderName: e.target.value })}
          className="mt-1 block w-full border rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
        >
          <option value="gemini">Gemini</option>
        </select>
      </div>
      <button
        type="submit"
        className="bg-blue-600 text-white text-sm px-4 py-2 rounded hover:bg-blue-700"
      >
        保存
      </button>
      {saved && <p className="text-green-600 text-sm">保存しました</p>}
      {error && <p className="text-red-600 text-sm">{error}</p>}
    </form>
  )
}
