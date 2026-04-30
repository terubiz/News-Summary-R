import { KeywordResponse, deleteKeyword, updateKeyword } from '../api/keywordsApi'

interface Props {
  keywords: KeywordResponse[]
  onChanged: () => void
}

export default function KeywordList({ keywords, onChanged }: Props) {
  const handleToggle = async (keyword: KeywordResponse) => {
    await updateKeyword(keyword.id, { isActive: !keyword.isActive })
    onChanged()
  }

  const handleDelete = async (id: number) => {
    await deleteKeyword(id)
    onChanged()
  }

  if (keywords.length === 0) {
    return <p className="text-gray-500">キーワードが登録されていません</p>
  }

  return (
    <ul className="divide-y divide-gray-200">
      {keywords.map((kw) => (
        <li key={kw.id} className="flex items-center justify-between py-2">
          <span className={kw.isActive ? 'text-gray-900' : 'text-gray-400 line-through'}>
            {kw.word}
          </span>
          <div className="flex gap-2">
            <button
              onClick={() => handleToggle(kw)}
              className="text-sm text-blue-600 hover:underline"
            >
              {kw.isActive ? '無効化' : '有効化'}
            </button>
            <button
              onClick={() => handleDelete(kw.id)}
              className="text-sm text-red-600 hover:underline"
            >
              削除
            </button>
          </div>
        </li>
      ))}
    </ul>
  )
}
