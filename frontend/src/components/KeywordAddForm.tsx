import { useState } from 'react'
import { createKeyword } from '../api/keywordsApi'

interface Props {
  onAdded: () => void
}

export default function KeywordAddForm({ onAdded }: Props) {
  const [word, setWord] = useState('')
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const trimmed = word.trim()
    if (!trimmed) return
    try {
      await createKeyword({ word: trimmed })
      setWord('')
      setError('')
      onAdded()
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } }).response?.status
      setError(status === 409 ? 'そのキーワードは既に登録されています' : '追加に失敗しました')
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex gap-2">
      <input
        type="text"
        value={word}
        onChange={(e) => setWord(e.target.value)}
        placeholder="キーワードを入力"
        className="flex-1 border rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
      />
      <button
        type="submit"
        className="bg-blue-600 text-white text-sm px-4 py-1.5 rounded hover:bg-blue-700"
      >
        追加
      </button>
      {error && <p className="text-red-600 text-sm self-center">{error}</p>}
    </form>
  )
}
