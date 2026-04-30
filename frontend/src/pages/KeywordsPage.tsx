import { useCallback, useEffect, useState } from 'react'
import { KeywordResponse, getKeywords } from '../api/keywordsApi'
import KeywordAddForm from '../components/KeywordAddForm'
import KeywordList from '../components/KeywordList'

export default function KeywordsPage() {
  const [keywords, setKeywords] = useState<KeywordResponse[]>([])
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setKeywords(await getKeywords())
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  return (
    <div className="max-w-lg mx-auto p-6">
      <h1 className="text-xl font-bold mb-4">キーワード管理</h1>
      <KeywordAddForm onAdded={load} />
      <div className="mt-4">
        {loading ? (
          <p className="text-gray-500">読み込み中...</p>
        ) : (
          <KeywordList keywords={keywords} onChanged={load} />
        )}
      </div>
    </div>
  )
}
